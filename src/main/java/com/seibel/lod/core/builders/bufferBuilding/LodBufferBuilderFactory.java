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

package com.seibel.lod.core.builders.bufferBuilding;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL44;

import com.seibel.lod.core.api.ClientApi;
import com.seibel.lod.core.enums.LodDirection;
import com.seibel.lod.core.enums.config.GpuUploadMethod;
import com.seibel.lod.core.enums.config.VanillaOverdraw;
import com.seibel.lod.core.enums.rendering.DebugMode;
import com.seibel.lod.core.enums.rendering.GLProxyContext;
import com.seibel.lod.core.objects.PosToRenderContainer;
import com.seibel.lod.core.objects.VertexOptimizer;
import com.seibel.lod.core.objects.lod.LodDimension;
import com.seibel.lod.core.objects.lod.LodRegion;
import com.seibel.lod.core.objects.lod.RegionPos;
import com.seibel.lod.core.objects.opengl.LodBufferBuilder;
import com.seibel.lod.core.objects.opengl.LodVertexBuffer;
import com.seibel.lod.core.render.GLProxy;
import com.seibel.lod.core.render.LodRenderer;
import com.seibel.lod.core.util.DataPointUtil;
import com.seibel.lod.core.util.DetailDistanceUtil;
import com.seibel.lod.core.util.LevelPosUtil;
import com.seibel.lod.core.util.LodThreadFactory;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.util.MovableGridList;
import com.seibel.lod.core.util.SingletonHandler;
import com.seibel.lod.core.util.SpamReducedLogger;
import com.seibel.lod.core.util.UnitBytes;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftWrapper;

/**
 * This object creates the buffers that are
 * rendered by the LodRenderer.
 * 
 * @author James Seibel
 * @version 12-9-2021
 */
public class LodBufferBuilderFactory
{
	
	//TODO: Do some Perf logging of Buffer Building
	public static final boolean ENABLE_BUFFER_PERF_LOGGING = false;
	public static final boolean ENABLE_BUFFER_SWAP_LOGGING = true;
	public static final boolean ENABLE_BUFFER_UPLOAD_LOGGING = false;
	public static final boolean ENABLE_LAG_SPIKE_LOGGING = false;
	public static final long LAG_SPIKE_THRESOLD_NS = TimeUnit.NANOSECONDS.convert(16, TimeUnit.MILLISECONDS);
	
	public static class LagSpikeCatcher {

		long timer = System.nanoTime();
		public LagSpikeCatcher() {}
		public void end(String source) {
			if (!ENABLE_LAG_SPIKE_LOGGING) return;
			timer = System.nanoTime() - timer;
			if (timer > LAG_SPIKE_THRESOLD_NS) {
				ClientApi.LOGGER.info("LagSpikeCatcher: "+source+" took "+Duration.ofNanos(timer)+"!");
			}
		}
	}
	
	private static final ILodConfigWrapperSingleton CONFIG = SingletonHandler.get(ILodConfigWrapperSingleton.class);
	private static final IMinecraftWrapper MC = SingletonHandler.get(IMinecraftWrapper.class);
	
	/** The thread used to generate new LODs off the main thread. */
	public static final ExecutorService mainGenThread = Executors.newSingleThreadExecutor(
			new LodThreadFactory(LodBufferBuilderFactory.class.getSimpleName() + " - main", Thread.NORM_PRIORITY-2));
	/** The threads used to generate buffers. */
	public static final ExecutorService bufferBuilderThreads = Executors.newFixedThreadPool(CONFIG.client().advanced().threading().getNumberOfBufferBuilderThreads(),
			new LodThreadFactory("BufferBuilder", Thread.NORM_PRIORITY-2));

	/** The thread used to upload buffers. */
	public static final ExecutorService bufferUploadThread = Executors.newSingleThreadExecutor(
			new LodThreadFactory(LodBufferBuilderFactory.class.getSimpleName() + " - upload", Thread.NORM_PRIORITY-1));
	
	public static final long MAX_BUFFER_UPLOAD_TIMEOUT_NANOSECONDS = TimeUnit.NANOSECONDS.convert(1, TimeUnit.SECONDS);
	
	/**
	 * When uploading to a buffer that is too small,
	 * recreate it this many times bigger than the upload payload
	 */
	public static final double BUFFER_EXPANSION_MULTIPLIER = 1.3;
	
	/**
	 * When buffers are first created they are allocated to this size (in Bytes).
	 * This size will be too small, more than likely. The buffers will be expanded
	 * when need be to fit the larger sizes.
	 */
	public static final int DEFAULT_MEMORY_ALLOCATION = (LodUtil.LOD_VERTEX_FORMAT.getByteSize()*3)*8;
	public static final int MAX_TRIANGLES_PER_BUFFER = (1024*1024*1) / (LodUtil.LOD_VERTEX_FORMAT.getByteSize()*3);
	
	
	
	public static int skyLightPlayer = 15;
	
	/**
	 * How many buffers there are for the given region. <Br>
	 * This is done because some regions may require more memory than
	 * can be directly allocated, so we split the regions into smaller sections. <Br>
	 * This keeps track of those sections.
	 */
	// TODO: Check why this is unused
	//public volatile int[][] numberOfBuffersPerRegion;
	
	/** Stores the vertices when building the VBOs */
	// FIXME: Use special warparound type of movable grid list in the future
	public volatile MovableGridList<LodBufferBuilder> buildableBuffers;
	
	/** Used when building new VBOs */
	public volatile MovableGridList<LodVertexBuffer[]> buildableVbos;
	public volatile int buildableCenterBlockX;
	public volatile int buildableCenterBlockY;
	public volatile int buildableCenterBlockZ;
	/** VBOs that are sent over to the LodNodeRenderer */
	public volatile MovableGridList<LodVertexBuffer[]> drawableVbos;
	public volatile int drawableCenterBlockX;
	public volatile int drawableCenterBlockY;
	public volatile int drawableCenterBlockZ;
	/**
	 * if this is true the LOD buffers need to be reset and
	 * the Renderer should call the lodGenBuffers nomatter it
	 * should have been a full or partial regen or not
	 */
	public volatile boolean frontBufferRequireReset = false;
	public volatile boolean allBuffersRequireReset = false;
	
	/**
	 * if this is true the LOD buffers are currently being
	 * regenerated.
	 */
	public boolean generatingBuffers = false;

	
	/**
	 * if this is true new LOD buffers have been generated
	 * and are waiting to be swapped with the drawable buffers
	 */
	private boolean switchVbos = false;
	// The hideFrontBuffer is for when switching dimensions
	private volatile boolean hideFrontBuffer = false;
	private volatile boolean hideBackBuffer = false;
	
	/** Size of the buffer builders in bytes last time we created them */
	public int previousBufferSize = 0;
	
	/** Width of the dimension in regions last time we created the buffers */
	public int previousRegionWidth = 0;
	
	/** this is used to prevent multiple threads creating, destroying, or using the buffers at the same time */
	private final ReentrantLock bufferLock = new ReentrantLock();
	
	private MovableGridList<VertexOptimizer> vertexOptimizerCache;
	private MovableGridList<PosToRenderContainer> setsToRender;
	
	private int lastX = 0;
	private int lastZ = 0;
	
	public LodBufferBuilderFactory()
	{
		
	}
	
	/**
	 * Create a thread to asynchronously generate LOD buffers
	 * centered around the given camera X and Z.
	 * <br>
	 * This method will write to the drawable near and far buffers.
	 * <br>
	 * After the buildable buffers have been generated they must be
	 * swapped with the drawable buffers in the LodRenderer to be drawn.
	 * @return whether it has started a generation task or is blocked
	 */
	public boolean updateAndSwapLodBuffersAsync(LodRenderer renderer, LodDimension lodDim,
			int playerX, int playerY, int playerZ, boolean partialRegen, boolean flushBuffers)
	{
		
		// only allow one generation process to happen at a time
		if (generatingBuffers)
			return false;
		
		if (MC.getCurrentLightMap() == null)
			// the lighting hasn't loaded yet
			return false;

		allBuffersRequireReset |= flushBuffers;
		
		boolean fullRegen;
		if (switchVbos) {
			fullRegen = swapBuffers();
		} else {
			fullRegen = allBuffersRequireReset || frontBufferRequireReset;
		}
		
		if (!fullRegen && !partialRegen) return false;
		
		generatingBuffers = true;
		
		Runnable thread = () -> generateLodBuffersThread(
				renderer, lodDim, playerX, playerY, playerZ, fullRegen);
		
		mainGenThread.execute(thread);
		return true;
	}
	
	
	
	
	private void generateLodBuffersThread(LodRenderer renderer, LodDimension lodDim,
			int playerX, int playerY, int playerZ, boolean fullRegen)
	{
		bufferLock.lock();
		
		long startTime = System.currentTimeMillis();
		ArrayList<RegionPos> posToCleanup = new ArrayList<RegionPos>(); 
		ArrayList<Callable<Boolean>> nodeToRenderThreads = new ArrayList<Callable<Boolean>>();
		
		try
		{	
			// round the player's block position down to the nearest chunk BlockPos
			int playerRegionX = LevelPosUtil.convert(LodUtil.BLOCK_DETAIL_LEVEL,playerX,LodUtil.REGION_DETAIL_LEVEL);
			int playerRegionZ = LevelPosUtil.convert(LodUtil.BLOCK_DETAIL_LEVEL,playerZ,LodUtil.REGION_DETAIL_LEVEL);
			int renderRange;
			int vboX;
			int vboY;
			int vboZ;

			boolean tooFar = Math.abs(buildableCenterBlockX-playerX)+Math.abs(buildableCenterBlockZ-playerZ)>100_000;
			
			if (fullRegen || tooFar || buildableBuffers==null || buildableVbos==null
					|| setsToRender==null || vertexOptimizerCache==null) {
				if (buildableVbos != null) {
					buildableVbos.clear((bs) -> {
						for (LodVertexBuffer b : bs) if (b!=null) b.close();
						});
				}
				
				renderRange = lodDim.getWidth()/2; //get lodDim half width
				buildableBuffers = new MovableGridList<LodBufferBuilder>(renderRange, playerRegionX, playerRegionZ);
				buildableVbos = new MovableGridList<LodVertexBuffer[]>(renderRange, playerRegionX, playerRegionZ);
				setsToRender = new MovableGridList<PosToRenderContainer>(renderRange, playerRegionX, playerRegionZ);
				vertexOptimizerCache = new MovableGridList<VertexOptimizer>(renderRange, playerRegionX, playerRegionZ);
				// this will be the center of the VBOs once they have been built
				// FIXME: Currently this will drift apart from player pos if there has not been a fullRegen for a while
				buildableCenterBlockX = playerX;
				buildableCenterBlockY = playerY;
				buildableCenterBlockZ = playerZ;
				vboX = playerX;
				vboY = playerY;
				vboZ = playerZ;
			} else {
				renderRange = buildableBuffers.gridCentreToEdge;
				vboX = buildableCenterBlockX;
				vboY = buildableCenterBlockY;
				vboZ = buildableCenterBlockZ;
				buildableBuffers.move(playerRegionX, playerRegionZ);
				buildableVbos.move(playerRegionX, playerRegionZ, (bs) -> {
					for (LodVertexBuffer b : bs) if (b!=null) b.close();
					}); 
				setsToRender.move(playerRegionX, playerRegionZ);
				vertexOptimizerCache.move(playerRegionX, playerRegionZ);
			}
			posToCleanup.ensureCapacity(buildableVbos.size());
			nodeToRenderThreads.ensureCapacity(buildableVbos.size());
			
			//================================//
			// create the nodeToRenderThreads //
			//================================//
			
			skyLightPlayer = MC.getWrappedClientWorld().getSkyLight(playerX, playerY, playerZ);
			//int minCullingRange = SingletonHandler.get(ILodConfigWrapperSingleton.class).client().graphics().advancedGraphics().getBacksideCullingRange();
			//int cullingRangeX = Math.max((int)(1.5 * Math.abs(lastX - playerX)), minCullingRange);
			//int cullingRangeZ = Math.max((int)(1.5 * Math.abs(lastZ - playerZ)), minCullingRange);
			lastX = playerX;
			lastZ = playerZ;
			List<CompletableFuture<RegionPos>> futuresBuffer = new LinkedList<CompletableFuture<RegionPos>>();
			for (int indexX = 0; indexX < buildableVbos.gridSize; indexX++)
			{
				for (int indexZ = 0; indexZ < buildableVbos.gridSize; indexZ++)
				{
					final int regionX = indexX + buildableVbos.getCenterX() - buildableVbos.gridCentreToEdge;
					final int regionZ = indexZ + buildableVbos.getCenterY() - buildableVbos.gridCentreToEdge;
					
					boolean needRegen = lodDim.getAndClearRegionNeedBufferRegen(regionX, regionZ);
					needRegen |= fullRegen;
					if (!needRegen) continue;
					
					LodRegion region = lodDim.getRegion(regionX, regionZ);
					if (region == null) continue;

					RegionPos regionPos = new RegionPos(regionX, regionZ);
					posToCleanup.add(regionPos);

					byte minDetail = region.getMinDetailLevel();
					final int pX = playerX;
					final int pZ = playerZ;
					
					CompletableFuture<RegionPos> future = new CompletableFuture<RegionPos>();

					bufferBuilderThreads.submit(() -> {
						LodBufferBuilder builder = buildableBuffers.get(regionX, regionZ);
						if (builder == null) {
							builder = buildableBuffers.setAndGet(regionX, regionZ, new LodBufferBuilder(DEFAULT_MEMORY_ALLOCATION));
						} else {
							builder.reset();
						}
						builder.begin(GL32.GL_QUADS, LodUtil.LOD_VERTEX_FORMAT);
						future.complete(makeLodRenderData(lodDim, regionPos, pX, pZ, vboX, vboZ, minDetail));//, cullingRangeX, cullingRangeZ);
					});
					
					futuresBuffer.add(future.whenCompleteAsync((regPos, e) -> {
						LodBufferBuilder buffer = buildableBuffers.get(regPos.x, regPos.z);
						if (buffer == null) return;
						try {
							buffer.end();
						} catch (Exception e2) {
							ClientApi.LOGGER.error("\"LodNodeBufferBuilder\" was unable to close buildable buffer: " + e.getMessage());
							e2.printStackTrace();
							buildableBuffers.set(regPos.x, regPos.z, null);
						}
						if (e!=null) return;
						uploadBuffers(regPos);
						buildableBuffers.set(regPos.x, regPos.z, null);
					}, bufferUploadThread));
				} // region z
			} // region z

			//================================//
			// execute the nodeToRenderThreads //
			//================================//
			
			long executeStart = System.currentTimeMillis();
			// wait for all threads to finish
			CompletableFuture<Void> allFutures = CompletableFuture.allOf(futuresBuffer.toArray(new CompletableFuture[futuresBuffer.size()]));
			try {
				allFutures.get();
			} catch (Exception e) {
				ClientApi.LOGGER.error("LodBufferBuilder ran into trouble: ");
				e.printStackTrace();
			}
			long executeEnd = System.currentTimeMillis();
			
			long endTime = System.currentTimeMillis();
			long buildTime = endTime - startTime;
			long executeTime = executeEnd - executeStart;
			if (ENABLE_BUFFER_PERF_LOGGING)
				ClientApi.LOGGER.info("Thread Build&Upload("+nodeToRenderThreads.size()+"/"+(lodDim.getWidth()*lodDim.getWidth())+ (fullRegen ? "FULL" : "")+") time: " + buildTime + " ms" + '\n' +
					                        "thread execute time: " + executeTime + " ms");
			
			// mark that the buildable buffers as ready to swap
			switchVbos = true;
		}
		catch (Exception e)
		{
			ClientApi.LOGGER.error("\"LodNodeBufferBuilder.generateLodBuffersAsync\" ran into trouble: ");
			e.printStackTrace();
		}
		finally
		{
			// regardless of whether we were able to successfully create
			// the buffers, we are done generating.
			generatingBuffers = false;
			bufferLock.unlock();
		}
	}
	
	private static final ThreadLocal<VertexOptimizer> tLocalVertexOptimizer = ThreadLocal.withInitial(VertexOptimizer::new);
	private static HashMap<LodDirection,long[]> makeAdjData(int verticalData) {
		HashMap<LodDirection,long[]> map = new HashMap<>();
		map.put(LodDirection.UP, new long[1]);
		map.put(LodDirection.DOWN, new long[1]);
		for (LodDirection lodDirection : VertexOptimizer.ADJ_DIRECTIONS)
			map.put(lodDirection, new long[verticalData]);
		return map;
	}
	private static final ThreadLocal<Map<LodDirection, long[]>> tLocalAdjData = new ThreadLocal<Map<LodDirection, long[]>>();
	
	private RegionPos makeLodRenderData(LodDimension lodDim, RegionPos regPos, int playerX, int playerZ,
			int vboX, int vboZ, byte minDetail) {//, int cullingRangeX, int cullingRangeZ) {

		//Variable initialization
		int playerChunkX = LevelPosUtil.convert(LodUtil.BLOCK_DETAIL_LEVEL,playerX,LodUtil.CHUNK_DETAIL_LEVEL);
		int playerChunkZ = LevelPosUtil.convert(LodUtil.BLOCK_DETAIL_LEVEL,playerZ,LodUtil.CHUNK_DETAIL_LEVEL);
		DebugMode debugMode = CONFIG.client().advanced().debugging().getDebugMode();
		VertexOptimizer vertexOptimizer = tLocalVertexOptimizer.get();
		boolean[] adjShadeDisabled = new boolean[VertexOptimizer.DIRECTIONS.length];
		LodBufferBuilder currentBuffer = buildableBuffers.get(regPos.x, regPos.z);
		
		// determine how many LODs we can stack vertically
		int maxVerticalData = DetailDistanceUtil.getMaxVerticalData((byte) 0);
		
		//we get or create the map that will contain the adj data
		Map<LodDirection, long[]> adjData = tLocalAdjData.get();
		if (adjData==null || adjData.get(LodDirection.NORTH).length != maxVerticalData) {
			adjData = makeAdjData(maxVerticalData);
			tLocalAdjData.set(adjData);
		}

		//We ask the lod dimension which block we have to render given the player position
		PosToRenderContainer posToRender = setsToRender.get(regPos.x, regPos.z);
		//previous setToRender cache
		if (posToRender == null) {
			posToRender = setsToRender.setAndGet(regPos.x, regPos.z, new PosToRenderContainer(minDetail, regPos.x, regPos.z));
		}
		posToRender.clear(minDetail, regPos.x, regPos.z);
		lodDim.getPosToRender(posToRender, regPos, playerX, playerZ);
		
		for (int index = 0; index < posToRender.getNumberOfPos(); index++)
		{
			byte detailLevel = posToRender.getNthDetailLevel(index);
			int posX = posToRender.getNthPosX(index);
			int posZ = posToRender.getNthPosZ(index);
			
			int chunkXdist = LevelPosUtil.getChunkPos(detailLevel, posX) - playerChunkX;
			int chunkZdist = LevelPosUtil.getChunkPos(detailLevel, posZ) - playerChunkZ;
			
			// TODO: In the future, We don't need to ignore rendered chunks! Just build it and leave it for the renderer to decide!
			// We don't want to render this fake block if
			// The block is inside the render distance with, is not bigger than a chunk and is positioned in a chunk set as vanilla rendered
			
			// The block is in the player chunk or in a chunk adjacent to the player
			if(detailLevel <= LodUtil.CHUNK_DETAIL_LEVEL &&
				isThisPositionGoingToBeRendered(LevelPosUtil.getChunkPos(detailLevel, posX),
						LevelPosUtil.getChunkPos(detailLevel, posZ)))
			{
				continue;
			}
			
			//we check if the block to render is not in player chunk
			boolean posNotInPlayerChunk = !(chunkXdist == 0 && chunkZdist == 0);
			
			// We extract the adj data in the four cardinal direction
			
			// we first reset the adjShadeDisabled. This is used to disable the shade on the border when we have transparent block like water or glass
			// to avoid having a "darker border" underground
			Arrays.fill(adjShadeDisabled, false);
			
			//We check every adj block in each direction
			for (LodDirection lodDirection : VertexOptimizer.ADJ_DIRECTIONS)
			{
				int xAdj = posX + VertexOptimizer.DIRECTION_NORMAL_MAP.get(lodDirection).x;
				int zAdj = posZ + VertexOptimizer.DIRECTION_NORMAL_MAP.get(lodDirection).z;
				chunkXdist = LevelPosUtil.getChunkPos(detailLevel, xAdj) - playerChunkX;
				chunkZdist = LevelPosUtil.getChunkPos(detailLevel, zAdj) - playerChunkZ;
				boolean adjPosInPlayerChunk = (chunkXdist == 0 && chunkZdist == 0);
				
				//If the adj block is rendered in the same region and with same detail
				// and is positioned in a place that is not going to be rendered by vanilla game
				// then we can set this position as adj
				// We avoid cases where the adjPosition is in player chunk while the position is not
				// to always have a wall underwater
				if(posToRender.contains(detailLevel, xAdj, zAdj)
					&& !isThisPositionGoingToBeRendered(LevelPosUtil.getChunkPos(detailLevel, xAdj), LevelPosUtil.getChunkPos(detailLevel,zAdj))
					&& !(posNotInPlayerChunk && adjPosInPlayerChunk))
				{
					for (int verticalIndex = 0; verticalIndex < lodDim.getMaxVerticalData(detailLevel, xAdj, zAdj); verticalIndex++)
					{
						long data = lodDim.getData(detailLevel, xAdj, zAdj, verticalIndex);
						adjShadeDisabled[VertexOptimizer.DIRECTION_INDEX.get(lodDirection)] = false;
						adjData.get(lodDirection)[verticalIndex] = data;
					}
				}
				else
				{
					//Otherwise, we check if this position is
					long data = lodDim.getSingleData(detailLevel, xAdj, zAdj);
					
					adjData.get(lodDirection)[0] = DataPointUtil.EMPTY_DATA;
					
					if ((isThisPositionGoingToBeRendered(LevelPosUtil.getChunkPos(detailLevel, xAdj), LevelPosUtil.getChunkPos(detailLevel,zAdj)) || 
							(posNotInPlayerChunk && adjPosInPlayerChunk))
								&& !DataPointUtil.isVoid(data))
					{
						adjShadeDisabled[VertexOptimizer.DIRECTION_INDEX.get(lodDirection)] = DataPointUtil.getAlpha(data) < 255;
					}
				}
			}
			
			// We render every vertical lod present in this position
			// We only stop when we find a block that is void or non-existing block
			long data;
			for (int verticalIndex = 0; verticalIndex < lodDim.getMaxVerticalData(detailLevel, posX, posZ); verticalIndex++)
			{
				
				//we get the above block as adj UP
				if (verticalIndex > 0)
					adjData.get(LodDirection.UP)[0] = lodDim.getData(detailLevel, posX, posZ, verticalIndex - 1);
				else
					adjData.get(LodDirection.UP)[0] = DataPointUtil.EMPTY_DATA;
				
				
				//we get the below block as adj DOWN
				if (verticalIndex < lodDim.getMaxVerticalData(detailLevel, posX, posZ) - 1)
					adjData.get(LodDirection.DOWN)[0] = lodDim.getData(detailLevel, posX, posZ, verticalIndex + 1);
				else
					adjData.get(LodDirection.DOWN)[0] = DataPointUtil.EMPTY_DATA;
				
				//We extract the data to render
				data = lodDim.getData(detailLevel, posX, posZ, verticalIndex);
				
				//If the data is not renderable (Void or non-existing) we stop since there is no data left in this position
				if (DataPointUtil.isVoid(data) || !DataPointUtil.doesItExist(data))
					break;
				
				//We send the call to create the vertices
				CubicLodTemplate.addLodToBuffer(currentBuffer, vboX, vboZ, data, adjData,
						detailLevel, posX, posZ, vertexOptimizer, debugMode, adjShadeDisabled);//, cullingRangeX, cullingRangeZ);
			}
			
		} // for pos to in list to render
		// the thread executed successfully
		currentBuffer.end();
		return regPos;
	}
	
	// Will be removed in a1.7
	@Deprecated
	private boolean isThisPositionGoingToBeRendered(int chunkX, int chunkZ){
		MovableGridList<Boolean> chunkGrid = ClientApi.renderer.vanillaRenderedChunks;
		Boolean isRendered = chunkGrid.get(chunkX, chunkZ);
		
		// skip any chunks that Minecraft is going to render
		if (isRendered == null || !isRendered) return false; 
		
		// check if the chunk is on the border
		if (CONFIG.client().graphics().advancedGraphics().getVanillaOverdraw() == VanillaOverdraw.BORDER)
			return !LodUtil.isBorderChunk(ClientApi.renderer.vanillaRenderedChunks, chunkX, chunkZ);
		else
			return true;
	}
	

	private final SpamReducedLogger ramLogger = new SpamReducedLogger(1);
	public void dumpBufferMemoryUsage() {
		if (!ramLogger.canMaybeLog()) return;
		ramLogger.info("Dumping Ram Usage for buffer usage...");
		long bufferCount = 0;
		long fullBufferCount = 0;
		long totalUsage = 0;
		long builderCount = 0;
		long totalBuilderUsage = 0;
		int maxLength = MAX_TRIANGLES_PER_BUFFER*(LodUtil.LOD_VERTEX_FORMAT.getByteSize()*3);
		if (buildableVbos == null) {
			ramLogger.info("Buildable VBOs are null!");
		} else
		for (LodVertexBuffer[] buffers : buildableVbos) {
			if (buffers == null) continue;
			LodVertexBuffer[] bs = buffers.clone();
			for (LodVertexBuffer b : bs) {
				if (b == null) continue;
				bufferCount++;
				if (b.size == maxLength) {
					fullBufferCount++;
				} else if (b.size > maxLength) {
					ramLogger.info("BUFFER OVERSIZED: {} (max size is {})", new UnitBytes(b.size), new UnitBytes(maxLength));
				}
				totalUsage += b.size;
			}
		}
		if (buildableBuffers == null) {
			ramLogger.info("Buildable Buffers are null!");
		} else
		for (LodBufferBuilder builder : buildableBuffers) {
			if (builder == null) continue;
			builderCount++;
			totalBuilderUsage += builder.getMemUsage();
		}
		if (drawableVbos == null) {
			ramLogger.info("Drawable VBOs are null!");
		} else
		for (LodVertexBuffer[] buffers : drawableVbos) {
			if (buffers == null) continue;
			LodVertexBuffer[] bs = buffers.clone();
			for (LodVertexBuffer b : bs) {
				if (b == null) continue;
				bufferCount++;
				if (b.size == maxLength) {
					fullBufferCount++;
				} else if (b.size > maxLength) {
					ramLogger.info("BUFFER OVERSIZED: {} (max size is {})", new UnitBytes(b.size), new UnitBytes(maxLength));
				}
				totalUsage += b.size;
			}
		}
		ramLogger.info("================================================");
		ramLogger.info("Buffers: [{}], Full-sized Buffers: [{}], Total: [{}]",
				bufferCount, fullBufferCount, new UnitBytes(totalUsage));
		ramLogger.info("Builders: [{}], Total: [{}]",
				builderCount, new UnitBytes(totalBuilderUsage));
		ramLogger.info("================================================");
		ramLogger.incLogTries();
	}
	
	
	
	//===============================//
	// BufferBuilder related methods //
	//===============================//
	
	/**
	 * Sets the buffers and Vbos to null, forcing them to be recreated <br>
	 * and destroys any bound OpenGL objects. <br>
	 * <br>
	 * <p>
	 * May have to wait for the bufferLock to open.
	 */
	public void destroyBuffers() {
		MovableGridList<LodVertexBuffer[]> toBeDeletedBuildableVbos;
		MovableGridList<LodVertexBuffer[]> toBeDeletedDrawableVbos;
		bufferLock.lock();
		try {
			toBeDeletedBuildableVbos = buildableVbos;
			toBeDeletedDrawableVbos = drawableVbos;
			buildableVbos = null;
			drawableVbos = null;
			// these don't contain any OpenGL objects, so
			// they don't require any special clean-up
			buildableBuffers = null;
		} finally {
			bufferLock.unlock();
		}
		// make sure the buffers are deleted in a openGL context
		GLProxy.getInstance().recordOpenGlCall(() -> {
			// destroy the VBOs if they aren't already
			if (toBeDeletedBuildableVbos != null) {
				toBeDeletedBuildableVbos.clear((vbos) -> {
					for (LodVertexBuffer vbo : vbos) {
						if (vbo == null) continue;
						vbo.close();
					}
				});
			}
			if (toBeDeletedDrawableVbos != null) {
				toBeDeletedDrawableVbos.clear((vbos) -> {
					for (LodVertexBuffer vbo : vbos) {
						if (vbo == null) continue;
						vbo.close();
					}
				});
			}
		});
	}

	/** Upload all buildableBuffers to the GPU. We should already be in the builder context */
	private void uploadBuffers(List<RegionPos> toBeUploaded)
	{
		GLProxy glProxy = GLProxy.getInstance();
		// determine the upload method
		GpuUploadMethod uploadMethod = glProxy.getGpuUploadMethod(); 
		
		// determine the upload timeout
		long BPerNS = CONFIG.client().advanced().buffers().getGpuUploadPerMegabyteInMilliseconds(); // MB -> B = 1/1,000,000. MS -> NS = 1,000,000. So, MBPerMS = BPerNS.
		long remainingNS = 0; // We don't want to pause for like 0.1 ms... so we store those tiny MS.
		long bytesUploaded = 0;
		long totalBuffers = 0;
		
		// actually upload the buffers
		for (RegionPos p : toBeUploaded) {
			LodBufferBuilder buffer = buildableBuffers.get(p.x, p.z);

			ByteBuffer uploadBuffer = null;
			//FIXME: The sonme Buffers aren't closed/end() and causing errors!
			try {
				LagSpikeCatcher b = new LagSpikeCatcher();
				uploadBuffer = buffer.getCleanedByteBuffer();
				b.end("getCleanedByteBuffer");
			} catch (IndexOutOfBoundsException e) {
				// NOTE: Temp try/catch for above FIXME.
				e.printStackTrace();
			} catch (RuntimeException e) {
				ClientApi.LOGGER.error(LodBufferBuilderFactory.class.getSimpleName() + " - UploadBuffers failed: " + e.getMessage());
				e.printStackTrace();
			}
			if (uploadBuffer == null) continue;
			

			int maxLength = MAX_TRIANGLES_PER_BUFFER*(LodUtil.LOD_VERTEX_FORMAT.getByteSize()*3);
			LagSpikeCatcher vboSetup = new LagSpikeCatcher();
			LodVertexBuffer[] vbos = buildableVbos.get(p.x, p.z);
			int requiredFullBuffers = Math.floorDiv(uploadBuffer.limit(),maxLength);
			int additionalBuffer = Math.floorMod(uploadBuffer.limit(),maxLength);
			if (vbos == null) {
				vbos = new LodVertexBuffer[requiredFullBuffers+1];
				buildableVbos.set(p.x, p.z, vbos);
			} else if (vbos.length != requiredFullBuffers+1) {
				LodVertexBuffer[] newVbos = new LodVertexBuffer[requiredFullBuffers+1];
				if (vbos.length > requiredFullBuffers+1) {
					for (int i=requiredFullBuffers+1; i<vbos.length; i++) {
						vbos[i].close();
						vbos[i] = null;
					}
				}
				for (int i=0; i<newVbos.length && i<vbos.length; i++) {
					newVbos[i] = vbos[i];
					vbos[i] = null;
				}
				for (LodVertexBuffer b : vbos) {
					if (b != null) throw new RuntimeException("EERTERERER");
				}
				
				buildableVbos.set(p.x, p.z, newVbos);
				vbos = newVbos;
			}
			vboSetup.end("vboSetup");
			uploadBuffer.rewind();
			for (int i=0; i<=requiredFullBuffers; i++) {
				ByteBuffer subBuffer;
				uploadBuffer.position(i*maxLength);
				subBuffer = uploadBuffer.slice();
				if (i==requiredFullBuffers) {
					subBuffer.limit(additionalBuffer);
				} else {
					subBuffer.limit(maxLength);
				}
				
				LagSpikeCatcher vboU = new LagSpikeCatcher();
				vboUpload(p, i, subBuffer, uploadMethod);
				vboU.end("vboUpload");
	
				// upload buffers over an extended period of time
				// to hopefully prevent stuttering.
				remainingNS += subBuffer.limit()*BPerNS;
				bytesUploaded += subBuffer.limit();
				if (remainingNS >= TimeUnit.NANOSECONDS.convert(1000/60, TimeUnit.MILLISECONDS)) {
					if (remainingNS > MAX_BUFFER_UPLOAD_TIMEOUT_NANOSECONDS) remainingNS = MAX_BUFFER_UPLOAD_TIMEOUT_NANOSECONDS;
					try {
						Thread.sleep(remainingNS/1000000, (int) (remainingNS%1000000));
					} catch (InterruptedException e) {}
					remainingNS = 0;
				}
			}
			totalBuffers += (requiredFullBuffers+1);
			if (ENABLE_BUFFER_UPLOAD_LOGGING)
				ClientApi.LOGGER.info("Uploaded {} sub buffers for {}", (requiredFullBuffers+1), p);
		}
		if (ENABLE_BUFFER_UPLOAD_LOGGING)
			ClientApi.LOGGER.info("UploadBuffers uploaded "+bytesUploaded+" bytes, with {} sub buffers", totalBuffers);
	}
	
	
	/** Upload all buildableBuffers to the GPU. We should already be in the builder context */
	private void uploadBuffers(RegionPos p)
	{
		GLProxy glProxy = GLProxy.getInstance();
		GLProxyContext oldContext = glProxy.getGlContext();
		glProxy.setGlContext(GLProxyContext.LOD_BUILDER);
		try {
		// determine the upload method
		GpuUploadMethod uploadMethod = glProxy.getGpuUploadMethod(); 
		
		// determine the upload timeout
		long BPerNS = CONFIG.client().advanced().buffers().getGpuUploadPerMegabyteInMilliseconds(); // MB -> B = 1/1,000,000. MS -> NS = 1,000,000. So, MBPerMS = BPerNS.
		long remainingNS = 0; // We don't want to pause for like 0.1 ms... so we store those tiny MS.
		
		// actually upload the buffers
			LodBufferBuilder buffer = buildableBuffers.get(p.x, p.z);

			ByteBuffer uploadBuffer = null;
			//FIXME: The sonme Buffers aren't closed/end() and causing errors!
			try {
				LagSpikeCatcher b = new LagSpikeCatcher();
				uploadBuffer = buffer.getCleanedByteBuffer();
				b.end("getCleanedByteBuffer");
			} catch (IndexOutOfBoundsException e) {
				// NOTE: Temp try/catch for above FIXME.
				e.printStackTrace();
			} catch (RuntimeException e) {
				ClientApi.LOGGER.error(LodBufferBuilderFactory.class.getSimpleName() + " - UploadBuffers failed: " + e.getMessage());
				e.printStackTrace();
			}
			if (uploadBuffer == null) return;

			int maxLength = MAX_TRIANGLES_PER_BUFFER*(LodUtil.LOD_VERTEX_FORMAT.getByteSize()*3);
			LagSpikeCatcher vboSetup = new LagSpikeCatcher();
			LodVertexBuffer[] vbos = buildableVbos.get(p.x, p.z);
			int requiredFullBuffers = Math.floorDiv(uploadBuffer.limit(),maxLength);
			int additionalBuffer = Math.floorMod(uploadBuffer.limit(),maxLength);
			if (vbos == null) {
				vbos = new LodVertexBuffer[requiredFullBuffers+1];
				buildableVbos.set(p.x, p.z, vbos);
			} else if (vbos.length != requiredFullBuffers+1) {
				LodVertexBuffer[] newVbos = new LodVertexBuffer[requiredFullBuffers+1];
				if (vbos.length > requiredFullBuffers+1) {
					for (int i=requiredFullBuffers+1; i<vbos.length; i++) {
						vbos[i].close();
						vbos[i] = null;
					}
				}
				for (int i=0; i<newVbos.length && i<vbos.length; i++) {
					newVbos[i] = vbos[i];
					vbos[i] = null;
				}
				for (LodVertexBuffer b : vbos) {
					if (b != null) throw new RuntimeException("EERTERERER");
				}
				
				buildableVbos.set(p.x, p.z, newVbos);
				vbos = newVbos;
			}
			vboSetup.end("vboSetup");
			uploadBuffer.rewind();
			for (int i=0; i<=requiredFullBuffers; i++) {
				ByteBuffer subBuffer;
				uploadBuffer.position(i*maxLength);
				subBuffer = uploadBuffer.slice();
				if (i==requiredFullBuffers) {
					subBuffer.limit(additionalBuffer);
				} else {
					subBuffer.limit(maxLength);
				}
				
				LagSpikeCatcher vboU = new LagSpikeCatcher();
				vboUpload(p, i, subBuffer, uploadMethod);
				vboU.end("vboUpload");
	
				// upload buffers over an extended period of time
				// to hopefully prevent stuttering.
				remainingNS += subBuffer.limit()*BPerNS;
				if (remainingNS >= TimeUnit.NANOSECONDS.convert(1000/60, TimeUnit.MILLISECONDS)) {
					if (remainingNS > MAX_BUFFER_UPLOAD_TIMEOUT_NANOSECONDS) remainingNS = MAX_BUFFER_UPLOAD_TIMEOUT_NANOSECONDS;
					try {
						Thread.sleep(remainingNS/1000000, (int) (remainingNS%1000000));
					} catch (InterruptedException e) {}
					remainingNS = 0;
				}
			}
			if (ENABLE_BUFFER_UPLOAD_LOGGING)
				ClientApi.LOGGER.info("Uploaded {} sub buffers for {}", (requiredFullBuffers+1), p);
		} finally {
			glProxy.setGlContext(oldContext);
		}
	}
	
	/** Uploads the uploadBuffer so the GPU can use it. */
	private void vboUpload(RegionPos regPos, int iIndex, ByteBuffer uploadBuffer, GpuUploadMethod uploadMethod)
	{
		int maxLength = MAX_TRIANGLES_PER_BUFFER*(LodUtil.LOD_VERTEX_FORMAT.getByteSize()*3);
		boolean useBuffStorage = uploadMethod == GpuUploadMethod.BUFFER_STORAGE;
		LodVertexBuffer[] vbos = buildableVbos.get(regPos.x, regPos.z);
		
		if (vbos[iIndex] == null) {
			vbos[iIndex] = new LodVertexBuffer(useBuffStorage);
		} else if (vbos[iIndex].isBufferStorage != useBuffStorage) {
			vbos[iIndex].close();
			vbos[iIndex] = new LodVertexBuffer(useBuffStorage);
		}

		LodVertexBuffer vbo = vbos[iIndex];
		// this is how many points will be rendered
		vbo.vertexCount = (uploadBuffer.limit() / LodUtil.LOD_VERTEX_FORMAT.getByteSize());
		
		// If size is zero, just ignore it.
		if (uploadBuffer.limit()==0) return;
		LagSpikeCatcher bindBuff = new LagSpikeCatcher();
		bindBuff.end("glBindBuffer vbo.id");
		
		try {
			// if possible use the faster buffer storage route
			if (uploadMethod == GpuUploadMethod.BUFFER_STORAGE)
			{
				GL32.glBindBuffer(GL32.GL_ARRAY_BUFFER, vbo.id);
				long size = vbo.size;
				if (size < uploadBuffer.limit() ||
						size > uploadBuffer.limit()*BUFFER_EXPANSION_MULTIPLIER*BUFFER_EXPANSION_MULTIPLIER)
				{
					int newSize = (int)(uploadBuffer.limit()*BUFFER_EXPANSION_MULTIPLIER);
					if (newSize > maxLength) newSize = maxLength;
					LagSpikeCatcher buffResizeRegen = new LagSpikeCatcher();
					GL32.glDeleteBuffers(vbo.id);
					vbo.id = GL32.glGenBuffers();
					buffResizeRegen.end("glDeleteBuffers BuffStorage resize");
					LagSpikeCatcher buffResize = new LagSpikeCatcher();
					GL32.glBindBuffer(GL32.GL_ARRAY_BUFFER, vbo.id);
					GL44.glBufferStorage(GL32.GL_ARRAY_BUFFER, newSize, GL44.GL_DYNAMIC_STORAGE_BIT);
					vbo.size = newSize;
					buffResize.end("glBufferStorage BuffStorage resize");
				} 
				LagSpikeCatcher buffSubData = new LagSpikeCatcher();
				GL32.glBufferSubData(GL32.GL_ARRAY_BUFFER, 0, uploadBuffer);
				buffSubData.end("glBufferSubData BuffStorage");
			}
			else if (uploadMethod == GpuUploadMethod.BUFFER_MAPPING)
			{
				GL32.glBindBuffer(GL32.GL_ARRAY_BUFFER, vbo.id);
				// no stuttering but high GPU usage
				// stores everything in system memory instead of GPU memory
				// making rendering much slower.
				// Unless the user is running integrated graphics,
				// in that case this will actually work better than SUB_DATA.
				long size = vbo.size;
				if (size < uploadBuffer.limit() ||
						size > uploadBuffer.limit()*BUFFER_EXPANSION_MULTIPLIER*BUFFER_EXPANSION_MULTIPLIER)
				{
					int newSize = (int) (uploadBuffer.limit()*BUFFER_EXPANSION_MULTIPLIER);
					if (newSize > maxLength) newSize = maxLength;
					LagSpikeCatcher buffResize = new LagSpikeCatcher();
					GL32.glBufferData(GL32.GL_ARRAY_BUFFER, newSize, GL32.GL_STATIC_DRAW);
					vbo.size = newSize;
					buffResize.end("glBufferData BuffMapping resize");
				}
				ByteBuffer vboBuffer;
				// map buffer range is better since it can be explicitly unsynchronized
				LagSpikeCatcher buffMap = new LagSpikeCatcher();
				vboBuffer = GL32.glMapBufferRange(GL32.GL_ARRAY_BUFFER, 0, uploadBuffer.limit(),
						GL32.GL_MAP_WRITE_BIT | GL32.GL_MAP_UNSYNCHRONIZED_BIT | GL32.GL_MAP_INVALIDATE_BUFFER_BIT);
				buffMap.end("glMapBufferRange BuffMapping");
				LagSpikeCatcher buffWrite = new LagSpikeCatcher();
				vboBuffer.put(uploadBuffer);
				LagSpikeCatcher buffUnmap = new LagSpikeCatcher();
				GL32.glUnmapBuffer(GL32.GL_ARRAY_BUFFER);
				buffUnmap.end("glUnmapBuffer");
				
				
				buffWrite.end("WriteData BuffMapping");
			}
			else if (uploadMethod == GpuUploadMethod.DATA)
			{
				GL32.glBindBuffer(GL32.GL_ARRAY_BUFFER, vbo.id);
				// TODO: Check this nonsense comment!
				// hybrid bufferData //
				// high stutter, low GPU usage
				// But simplest/most compatible
				LagSpikeCatcher buffData = new LagSpikeCatcher();
				GL32.glBufferData(GL32.GL_ARRAY_BUFFER, uploadBuffer, GL32.GL_STATIC_DRAW);
				vbo.size = uploadBuffer.limit();
				buffData.end("glBufferData Data");
			}
			else
			{
				GL32.glBindBuffer(GL32.GL_ARRAY_BUFFER, vbo.id);
				// TODO: Check this nonsense comment!
				// hybrid subData/bufferData //
				// less stutter, low GPU usage
				long size = vbo.size;
				if (size < uploadBuffer.limit() ||
						size > uploadBuffer.limit()*BUFFER_EXPANSION_MULTIPLIER*BUFFER_EXPANSION_MULTIPLIER)
				{
					int newSize = (int)(uploadBuffer.limit()*BUFFER_EXPANSION_MULTIPLIER);
					if (newSize > maxLength) newSize = maxLength;
					LagSpikeCatcher buffResize = new LagSpikeCatcher();
					GL32.glBufferData(GL32.GL_ARRAY_BUFFER, newSize, GL32.GL_STATIC_DRAW);
					vbo.size = newSize;
					buffResize.end("glBufferData SubData resize");
				}
				LagSpikeCatcher buffSubData = new LagSpikeCatcher();
				GL32.glBufferSubData(GL32.GL_ARRAY_BUFFER, 0, uploadBuffer);
				buffSubData.end("glBufferSubData SubData");
			}
		}
		catch (Exception e)
		{
			ClientApi.LOGGER.error("vboUpload failed: " + e.getClass().getSimpleName());
			e.printStackTrace();
		}
	}//vboUpload
	
	private boolean swapBuffers() {
		bufferLock.lock();
		if (ENABLE_BUFFER_SWAP_LOGGING)
			ClientApi.LOGGER.info("Lod Swap Buffers");
		{
			boolean shouldRegenBuff = true;
			try
			{
				MovableGridList<LodVertexBuffer[]> tmpVbo = drawableVbos;
				drawableVbos = buildableVbos;
				buildableVbos = tmpVbo;

				//ClientApi.LOGGER.info("Lod Swapped Buffers: "+drawableVbos.toDetailString());
				
				int tempX = drawableCenterBlockX;
				int tempY = drawableCenterBlockY;
				int tempZ = drawableCenterBlockZ;
				drawableCenterBlockX = buildableCenterBlockX;
				drawableCenterBlockY = buildableCenterBlockY;
				drawableCenterBlockZ = buildableCenterBlockZ;
				buildableCenterBlockX = tempX;
				buildableCenterBlockY = tempY;
				buildableCenterBlockZ = tempZ;
				// the vbos have been swapped
				switchVbos = false;

				//FIXME: Race condition on the allBuffersRequireReset boolean
			    shouldRegenBuff = frontBufferRequireReset || allBuffersRequireReset;
				frontBufferRequireReset = allBuffersRequireReset;
				allBuffersRequireReset = false;
				hideFrontBuffer = hideBackBuffer;
				hideBackBuffer = false;
			}
			catch (Exception e)
			{
				// this shouldn't normally happen, but just in case it sill prevent deadlock
				ClientApi.LOGGER.error("swapBuffers ran into trouble: " + e.getMessage(), e);
			}
			finally
			{
				bufferLock.unlock();
			}
			return shouldRegenBuff;
		}
	}
	
	/** Get the newly created VBOs */
	public MovableGridList<LodVertexBuffer[]> getFrontBuffers()
	{
		return shouldDrawFrontBuffer() ? drawableVbos : null;
	}
	public int getFrontBuffersCenterX()
	{
		return drawableCenterBlockX;
	}
	public int getFrontBuffersCenterZ()
	{
		return drawableCenterBlockZ;
	}

	public void triggerReset() {
		allBuffersRequireReset = true;
		hideBackBuffer = true;
		hideFrontBuffer = true;
	}
	public boolean shouldDrawFrontBuffer() {
		return !hideFrontBuffer;
	}
}
