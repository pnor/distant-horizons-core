/*
 *    This file is part of the Distant Horizon mod (formerly the LOD Mod),
 *    licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2020  James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.seibel.lod.core.builders.worldGeneration;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.seibel.lod.core.api.ClientApi;
import com.seibel.lod.core.builders.lodBuilding.LodBuilder;
import com.seibel.lod.core.enums.config.DistanceGenerationMode;
import com.seibel.lod.core.objects.PosToGenerateContainer;
import com.seibel.lod.core.objects.lod.LodDimension;
import com.seibel.lod.core.util.DetailDistanceUtil;
import com.seibel.lod.core.util.LevelPosUtil;
import com.seibel.lod.core.util.LodThreadFactory;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.util.SingletonHandler;
import com.seibel.lod.core.wrapperInterfaces.IWrapperFactory;
import com.seibel.lod.core.wrapperInterfaces.chunk.AbstractChunkPosWrapper;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IWorldWrapper;
import com.seibel.lod.core.wrapperInterfaces.worldGeneration.AbstractWorldGeneratorWrapper;

/**
 * A singleton that handles all long distance LOD world generation.
 * @author Leonardo Amato
 * @author James Seibel
 * @version 9-25-2021
 */
public class LodWorldGenerator
{
	private static final IMinecraftWrapper MC = SingletonHandler.get(IMinecraftWrapper.class);
	private static final ILodConfigWrapperSingleton CONFIG = SingletonHandler.get(ILodConfigWrapperSingleton.class);
	private static final IWrapperFactory WRAPPER_FACTORY = SingletonHandler.get(IWrapperFactory.class);
	
	
	/** This holds the thread used to create LOD generation requests off the main thread. */
	private final ExecutorService mainGenThread = Executors.newSingleThreadExecutor(new LodThreadFactory(this.getClass().getSimpleName() + " world generator"));
	private ExecutorService genSubThreads = Executors.newFixedThreadPool(CONFIG.client().advanced().threading().getNumberOfWorldGenerationThreads(),
			new ThreadFactoryBuilder().setNameFormat("Gen-Worker-Thread-%d").build());
	
	
	/** we only want to queue up one generator thread at a time */
	private boolean generatorThreadRunning = false;
	
	/**
	 * How many chunks to generate outside the player's view distance at one
	 * time. (or more specifically how many requests to make at one time). I
	 * multiply by 8 to make sure there is always a buffer of chunk requests, to
	 * make sure the CPU is always busy, and we can generate LODs as quickly as
	 * possible.
	 */
	public int maxChunkGenRequests;
	
	/**
	 * This keeps track of how many chunk generation requests are on going. This is
	 * to limit how many chunks are queued at once. To prevent chunks from being
	 * generated for a long time in an area the player is no longer in.
	 */
	public final AtomicInteger numberOfChunksWaitingToGenerate = new AtomicInteger(0);
	
	public final Set<AbstractChunkPosWrapper> positionsWaitingToBeGenerated = new HashSet<>();
	
	/**
	 * Singleton copy of this object
	 */
	public static final LodWorldGenerator INSTANCE = new LodWorldGenerator();
	
	private LodWorldGenerator() {}
	
	/**
	 * Queues up LodNodeGenWorkers for the given lodDimension.
	 * @param renderer needed so the LodNodeGenWorkers can flag that the
	 * buffers need to be rebuilt.
	 */
	public void queueGenerationRequests(LodDimension lodDim, LodBuilder lodBuilder)
	{
		if (CONFIG.client().worldGenerator().getDistanceGenerationMode() != DistanceGenerationMode.NONE
				&& !generatorThreadRunning
				&& MC.hasSinglePlayerServer())
		{
			// the thread is now running, don't queue up another thread
			generatorThreadRunning = true;
			
			// just in case the config changed
			maxChunkGenRequests = CONFIG.client().advanced().threading().getNumberOfWorldGenerationThreads() * 8;
			
			Runnable generatorFunc = (() ->
			{
				try
				{
					// round the player's block position down to the nearest chunk BlockPos
					int playerPosX = MC.getPlayerBlockPos().getX();
					int playerPosZ = MC.getPlayerBlockPos().getZ();
					
					
					//=======================================//
					// fill in positionsWaitingToBeGenerated //
					//=======================================//
					
					IWorldWrapper serverWorld = LodUtil.getServerWorldFromDimension(lodDim.dimension);
					
					PosToGenerateContainer posToGenerate = lodDim.getPosToGenerate(
							maxChunkGenRequests,
							playerPosX,
							playerPosZ);
					
					
					byte detailLevel;
					int posX;
					int posZ;
					int nearIndex = 0;
					int farIndex = 0;
					
					for (int i = 0; i < posToGenerate.getNumberOfPos(); i++)
					{
						// I wish there was a way to compress this code, but I'm not aware of 
						// an easy way to do so.
						
						// add the near positions
						if (posToGenerate.getNthDetail(nearIndex, true) != 0 && nearIndex < posToGenerate.getNumberOfNearPos())
						{
							detailLevel = (byte) (posToGenerate.getNthDetail(nearIndex, true) - 1);
							posX = posToGenerate.getNthPosX(nearIndex, true);
							posZ = posToGenerate.getNthPosZ(nearIndex, true);
							nearIndex++;
							
							AbstractChunkPosWrapper chunkPos = WRAPPER_FACTORY.createChunkPos(LevelPosUtil.getChunkPos(detailLevel, posX), LevelPosUtil.getChunkPos(detailLevel, posZ));
							
							// prevent generating the same chunk multiple times
							if (positionsWaitingToBeGenerated.contains(chunkPos))
								continue;
							
							// don't add more to the generation queue then allowed
							if (numberOfChunksWaitingToGenerate.get() >= maxChunkGenRequests)
								break;
							
							positionsWaitingToBeGenerated.add(chunkPos);
							numberOfChunksWaitingToGenerate.addAndGet(1);
							queueWork(chunkPos, DetailDistanceUtil.getDistanceGenerationMode(detailLevel), lodBuilder, lodDim, serverWorld);
						}
						
						
						// add the far positions
						if (posToGenerate.getNthDetail(farIndex, false) != 0 && farIndex < posToGenerate.getNumberOfFarPos())
						{
							detailLevel = (byte) (posToGenerate.getNthDetail(farIndex, false) - 1);
							posX = posToGenerate.getNthPosX(farIndex, false);
							posZ = posToGenerate.getNthPosZ(farIndex, false);
							farIndex++;
							
							AbstractChunkPosWrapper chunkPos = WRAPPER_FACTORY.createChunkPos(LevelPosUtil.getChunkPos(detailLevel, posX), LevelPosUtil.getChunkPos(detailLevel, posZ));
							
							// don't add more to the generation queue then allowed
							if (numberOfChunksWaitingToGenerate.get() >= maxChunkGenRequests)
								continue;
							//break;
							
							// prevent generating the same chunk multiple times
							if (positionsWaitingToBeGenerated.contains(chunkPos))
								continue;
							
							positionsWaitingToBeGenerated.add(chunkPos);
							numberOfChunksWaitingToGenerate.addAndGet(1);
							queueWork(chunkPos, DetailDistanceUtil.getDistanceGenerationMode(detailLevel), lodBuilder, lodDim, serverWorld);
						}
					}
					
				}
				catch (Exception e)
				{
					// this shouldn't ever happen, but just in case
					e.printStackTrace();
				}
				finally
				{
					generatorThreadRunning = false;
				}
			});
			if (WRAPPER_FACTORY.isWorldGeneratorSingleThreaded()) {
				generatorFunc.run();
			} else {
				mainGenThread.execute(generatorFunc);
			}
		} // if distanceGenerationMode != DistanceGenerationMode.NONE && !generatorThreadRunning
	} // queueGenerationRequests
	
	private void queueWork(AbstractChunkPosWrapper newPos, DistanceGenerationMode newGenerationMode,
			LodBuilder newLodBuilder,
			LodDimension newLodDimension, IWorldWrapper serverWorld)
	{
		// just a few sanity checks
		if (newPos == null)
			throw new IllegalArgumentException("LodChunkGenWorker must have a non-null ChunkPos");
		
		if (newLodBuilder == null)
			throw new IllegalArgumentException("LodChunkGenThread requires a non-null LodChunkBuilder");
		
		if (newLodDimension == null)
			throw new IllegalArgumentException("LodChunkGenThread requires a non-null LodDimension");
		
		if (serverWorld == null)
			throw new IllegalArgumentException("LodChunkGenThread requires a non-null ServerWorld");
		
		Runnable method = (() -> {generateChunk(newPos, newGenerationMode,
				newLodBuilder, newLodDimension, serverWorld);});
		
		if (CONFIG.client().worldGenerator().getDistanceGenerationMode() == DistanceGenerationMode.FULL
			|| WRAPPER_FACTORY.isWorldGeneratorSingleThreaded())
		{
			// if we are using FULL generation there is no reason
			// to queue up a bunch of generation requests,
			// because MC's internal server (as of 1.16.5) only
			// responds with a single thread. And we don't
			// want to cause more lag than necessary or queue up
			// requests that may end up being unneeded.
			// In 1.17+, world generation becomes completely single
			// threaded. So to allow that, we check the boolean for
			// whether the wrapper requires single thread
			method.run();
		}
		else
		{
			// Every other method can
			// be done asynchronously
			genSubThreads.execute(method);
		}
		
		// useful for debugging
//    	ClientProxy.LOGGER.info(thread.lodDim.getNumberOfLods());
//    	ClientProxy.LOGGER.info(genThreads.toString());
	}
	
	private void generateChunk(AbstractChunkPosWrapper pos, DistanceGenerationMode generationMode, 
			LodBuilder newLodBuilder, LodDimension lodDim, IWorldWrapper worldWrapper)
	{
		// try
		{
			AbstractWorldGeneratorWrapper worldGenWrapper = WRAPPER_FACTORY.createWorldGenerator(newLodBuilder, lodDim, worldWrapper);
			// only generate LodChunks if they can
			// be added to the current LodDimension
			
			if (lodDim.regionIsInRange(pos.getX() / LodUtil.REGION_WIDTH_IN_CHUNKS, pos.getZ() / LodUtil.REGION_WIDTH_IN_CHUNKS))
			{					
				switch (generationMode)
				{
				case NONE:
					// don't generate
					break;
				case BIOME_ONLY:
				case BIOME_ONLY_SIMULATE_HEIGHT:
					// fastest
					worldGenWrapper.generateBiomesOnly(pos, generationMode);
					break;
				case SURFACE:
					// faster
					worldGenWrapper.generateSurface(pos);
					break;
				case FEATURES:
					// fast
					worldGenWrapper.generateFeatures(pos);
					break;
				case FULL:
					// very slow
					worldGenWrapper.generateFull(pos);
					break;
				}

//				boolean dataExistence = lodDim.doesDataExist(new LevelPos((byte) 3, pos.x, pos.z));
//				if (dataExistence)
//					ClientProxy.LOGGER.info(pos.x + " " + pos.z + " Success!");
//				else
//					ClientProxy.LOGGER.info(pos.x + " " + pos.z);

				// shows the pool size, active threads, queued tasks and completed tasks
//				ClientProxy.LOGGER.info(genThreads.toString());
				
			}// if in range
		}
		// catch (Exception e)
		// {
		// 	ClientApi.LOGGER.error(LodWorldGenerator.class.getSimpleName() + ": ran into an error: " + e.getMessage());
		// 	e.printStackTrace();
		// }
		// finally
		{
			// decrement how many threads are running
			LodWorldGenerator.INSTANCE.numberOfChunksWaitingToGenerate.addAndGet(-1);
			
			// this position is no longer being generated
			LodWorldGenerator.INSTANCE.positionsWaitingToBeGenerated.remove(pos);
		}
	}// run
	
	/**
	 * Stops the current genThreads if they are running
	 * and then recreates the Executor service. <br><br>
	 * <p>
	 * This is done to clear any outstanding tasks
	 * that may exist after the player leaves their current world.
	 * If this isn't done unfinished tasks may be left in the queue
	 * preventing new LodChunks form being generated.
	 */
	public void restartExecutorService()
	{
		if (genSubThreads != null && !genSubThreads.isShutdown())
		{
			genSubThreads.shutdownNow();
		}
		genSubThreads = Executors.newFixedThreadPool(CONFIG.client().advanced().threading().getNumberOfWorldGenerationThreads(),
				new ThreadFactoryBuilder().setNameFormat("Gen-Worker-Thread-%d").build());
	}
	
}
