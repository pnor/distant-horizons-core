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

package com.seibel.lod.builders.worldGeneration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.seibel.lod.api.forge.LodConfig;
import com.seibel.lod.api.lod.ClientApi;
import com.seibel.lod.builders.lodBuilding.LodBuilder;
import com.seibel.lod.enums.config.DistanceGenerationMode;
import com.seibel.lod.objects.lod.LodDimension;
import com.seibel.lod.util.LodUtil;
import com.seibel.lod.wrappers.chunk.ChunkPosWrapper;
import com.seibel.lod.wrappers.world.WorldWrapper;
import com.seibel.lod.wrappers.worldGeneration.WorldGeneratorWrapper;

import net.minecraftforge.common.WorldWorkerManager.IWorker;

/**
 * This is used to generate a LodChunk at a given ChunkPos.
 * 
 * @author James Seibel
 * @version 11-13-2021
 */
public class LodGenWorker implements IWorker // TODO is there a way to have this fabric/forge independent?
{
	public static ExecutorService genThreads = Executors.newFixedThreadPool(LodConfig.CLIENT.advancedModOptions.threading.numberOfWorldGenerationThreads.get(), new ThreadFactoryBuilder().setNameFormat("Gen-Worker-Thread-%d").build());
	
	private boolean threadStarted = false;
	private final LodChunkGenThread thread;
	
	
	
	public LodGenWorker(ChunkPosWrapper newPos, DistanceGenerationMode newGenerationMode,
			LodBuilder newLodBuilder,
			LodDimension newLodDimension, WorldWrapper serverWorld)
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
		
		
		
		thread = new LodChunkGenThread(newPos, newGenerationMode,
				newLodBuilder,
				newLodDimension, serverWorld);
	}
	
	@Override
	public boolean doWork()
	{
		if (!threadStarted)
		{
			if (LodConfig.CLIENT.worldGenerator.distanceGenerationMode.get() == DistanceGenerationMode.SERVER)
			{
				// if we are using SERVER generation that has to be done
				// synchronously to prevent crashing and harmful
				// interactions with the normal world generator
				thread.run();
			}
			else
			{
				// Every other method can
				// be done asynchronously
				Thread newThread = new Thread(thread);
				newThread.setPriority(5);
				genThreads.execute(newThread);
			}
			
			threadStarted = true;
			
			// useful for debugging
//        	ClientProxy.LOGGER.info(thread.lodDim.getNumberOfLods());
//        	ClientProxy.LOGGER.info(genThreads.toString());
		}
		
		return false;
	}
	
	@Override
	public boolean hasWork()
	{
		return !threadStarted;
	}
	
	
	
	
	private static class LodChunkGenThread implements Runnable
	{
		private WorldGeneratorWrapper worldGenWrapper; 
		
		public final LodDimension lodDim;
		public final DistanceGenerationMode generationMode;
		
		private final ChunkPosWrapper pos;
		
		public LodChunkGenThread(ChunkPosWrapper newPos, DistanceGenerationMode newGenerationMode,
				LodBuilder newLodBuilder,
				LodDimension newLodDimension, WorldWrapper worldWrapper)
		{
			worldGenWrapper = new WorldGeneratorWrapper(newLodBuilder, newLodDimension, worldWrapper);
			
			pos = newPos;
			generationMode = newGenerationMode;
			lodDim = newLodDimension;
		}
		
		@Override
		public void run()
		{
			try
			{
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
						worldGenWrapper.generateUsingBiomesOnly(pos, generationMode);
						break;
					case SURFACE:
						// faster
						worldGenWrapper.generateUsingSurface(pos);
						break;
					case FEATURES:
						// fast
						worldGenWrapper.generateUsingFeatures(pos);
						break;
					case SERVER:
						// very slow
						worldGenWrapper.generateWithServer(pos);
						break;
					}
					

//					boolean dataExistence = lodDim.doesDataExist(new LevelPos((byte) 3, pos.x, pos.z));
//					if (dataExistence)
//						ClientProxy.LOGGER.info(pos.x + " " + pos.z + " Success!");
//					else
//						ClientProxy.LOGGER.info(pos.x + " " + pos.z);

					// shows the pool size, active threads, queued tasks and completed tasks
//					ClientProxy.LOGGER.info(genThreads.toString());

//					long endTime = System.currentTimeMillis();
//					System.out.println(endTime - startTime);
					
				}// if in range
			}
			catch (Exception e)
			{
				ClientApi.LOGGER.error(LodChunkGenThread.class.getSimpleName() + ": ran into an error: " + e.getMessage());
				e.printStackTrace();
			}
			finally
			{
				// decrement how many threads are running
				LodWorldGenerator.INSTANCE.numberOfChunksWaitingToGenerate.addAndGet(-1);
				
				// this position is no longer being generated
				LodWorldGenerator.INSTANCE.positionsWaitingToBeGenerated.remove(pos);
			}
		}// run
		
		
	}
	
	
	/**
	 * Stops the current genThreads if they are running
	 * and then recreates the Executor service. <br><br>
	 * <p>
	 * This is done to clear any outstanding tasks
	 * that may exist after the player leaves their current world.
	 * If this isn't done unfinished tasks may be left in the queue
	 * preventing new LodChunks form being generated.
	 */
	public static void restartExecutorService()
	{
		if (genThreads != null && !genThreads.isShutdown())
		{
			genThreads.shutdownNow();
		}
		genThreads = Executors.newFixedThreadPool(LodConfig.CLIENT.advancedModOptions.threading.numberOfWorldGenerationThreads.get(), new ThreadFactoryBuilder().setNameFormat("Gen-Worker-Thread-%d").build());
	}
	
}
