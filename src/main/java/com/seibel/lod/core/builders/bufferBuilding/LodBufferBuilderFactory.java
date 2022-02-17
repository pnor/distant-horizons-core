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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
import com.seibel.lod.core.objects.lod.LodDimension;
import com.seibel.lod.core.objects.lod.LodRegion;
import com.seibel.lod.core.objects.lod.RegionPos;
import com.seibel.lod.core.objects.opengl.LodQuadBuilder;
import com.seibel.lod.core.objects.opengl.LodVertexBuffer;
import com.seibel.lod.core.objects.opengl.RenderRegion;
import com.seibel.lod.core.render.GLProxy;
import com.seibel.lod.core.render.LodRenderer;
import com.seibel.lod.core.util.DataPointUtil;
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
 * This object creates the buffers that are rendered by the LodRenderer.
 * 
 * @author James Seibel
 * @version 12-9-2021
 */
public class LodBufferBuilderFactory {

	// TODO: Do some Perf logging of Buffer Building
	public static final boolean ENABLE_BUFFER_PERF_LOGGING = false;
	public static final boolean ENABLE_BUFFER_SWAP_LOGGING = true;
	public static final boolean ENABLE_BUFFER_UPLOAD_LOGGING = false;
	public static final boolean ENABLE_LAG_SPIKE_LOGGING = false;
	public static final long LAG_SPIKE_THRESOLD_NS = TimeUnit.NANOSECONDS.convert(16, TimeUnit.MILLISECONDS);

	public static class LagSpikeCatcher {

		long timer = System.nanoTime();

		public LagSpikeCatcher() {
		}

		public void end(String source) {
			if (!ENABLE_LAG_SPIKE_LOGGING)
				return;
			timer = System.nanoTime() - timer;
			if (timer > LAG_SPIKE_THRESOLD_NS) {
				ClientApi.LOGGER.info("LagSpikeCatcher: " + source + " took " + Duration.ofNanos(timer) + "!");
			}
		}
	}

	private static final ILodConfigWrapperSingleton CONFIG = SingletonHandler.get(ILodConfigWrapperSingleton.class);
	private static final IMinecraftWrapper MC = SingletonHandler.get(IMinecraftWrapper.class);

	/** The thread used to generate new LODs off the main thread. */
	private static LodThreadFactory mainGenThreadFactory = new LodThreadFactory(
			LodBufferBuilderFactory.class.getSimpleName() + " - main", Thread.NORM_PRIORITY - 2);
	public static ExecutorService mainGenThread = Executors.newSingleThreadExecutor(mainGenThreadFactory);

	/** The threads used to generate buffers. */
	private static LodThreadFactory bufferBuilderThreadFactory = new LodThreadFactory("BufferBuilder",
			Thread.NORM_PRIORITY - 2);
	public static ExecutorService bufferBuilderThreads = Executors.newFixedThreadPool(
			CONFIG.client().advanced().threading().getNumberOfBufferBuilderThreads(), bufferBuilderThreadFactory);

	/** The thread used to upload buffers. */
	private static LodThreadFactory bufferUploadThreadFactory = new LodThreadFactory(
			LodBufferBuilderFactory.class.getSimpleName() + " - upload", Thread.NORM_PRIORITY - 1);
	public static ExecutorService bufferUploadThread = Executors.newSingleThreadExecutor(bufferUploadThreadFactory);

	public static final long MAX_BUFFER_UPLOAD_TIMEOUT_NANOSECONDS = TimeUnit.NANOSECONDS.convert(1, TimeUnit.SECONDS);

	/**
	 * When uploading to a buffer that is too small, recreate it this many times
	 * bigger than the upload payload
	 */
	public static final double BUFFER_EXPANSION_MULTIPLIER = 1.3;

	/**
	 * When buffers are first created they are allocated to this size (in Bytes).
	 * This size will be too small, more than likely. The buffers will be expanded
	 * when need be to fit the larger sizes.
	 */
	public static final int DEFAULT_MEMORY_ALLOCATION = (LodUtil.LOD_VERTEX_FORMAT.getByteSize() * 3) * 8;
	public static final int MAX_TRIANGLES_PER_BUFFER = (1024 * 1024 * 1)
			/ (LodUtil.LOD_VERTEX_FORMAT.getByteSize() * 3);

	public static int skyLightPlayer = 15;

	/**
	 * How many buffers there are for the given region. <Br>
	 * This is done because some regions may require more memory than can be
	 * directly allocated, so we split the regions into smaller sections. <Br>
	 * This keeps track of those sections.
	 */
	// TODO: Check why this is unused
	// public volatile int[][] numberOfBuffersPerRegion;

	/** Used when building new VBOs */
	public volatile MovableGridList<RenderRegion> buildableVbos;
	public volatile int buildableCenterBlockX;
	public volatile int buildableCenterBlockY;
	public volatile int buildableCenterBlockZ;
	/** VBOs that are sent over to the LodNodeRenderer */
	public volatile MovableGridList<RenderRegion> drawableVbos;
	public volatile int drawableCenterBlockX;
	public volatile int drawableCenterBlockY;
	public volatile int drawableCenterBlockZ;
	/**
	 * if this is true the LOD buffers need to be reset and the Renderer should call
	 * the lodGenBuffers nomatter it should have been a full or partial regen or not
	 */
	public volatile boolean frontBufferRequireReset = false;
	public volatile boolean allBuffersRequireReset = false;

	/**
	 * if this is true the LOD buffers are currently being regenerated.
	 */
	public boolean generatingBuffers = false;

	/**
	 * if this is true new LOD buffers have been generated and are waiting to be
	 * swapped with the drawable buffers
	 */
	private boolean switchVbos = false;
	// The hideFrontBuffer is for when switching dimensions
	private volatile boolean hideFrontBuffer = false;
	private volatile boolean hideBackBuffer = false;

	/** Size of the buffer builders in bytes last time we created them */
	public int previousBufferSize = 0;

	/** Width of the dimension in regions last time we created the buffers */
	public int previousRegionWidth = 0;

	/**
	 * this is used to prevent multiple threads creating, destroying, or using the
	 * buffers at the same time
	 */
	private final ReentrantLock bufferLock = new ReentrantLock();

	private MovableGridList<PosToRenderContainer> setsToRender;

	private int lastX = 0;
	private int lastZ = 0;

	public LodBufferBuilderFactory() {

	}

	/**
	 * Create a thread to asynchronously generate LOD buffers centered around the
	 * given camera X and Z. <br>
	 * This method will write to the drawable near and far buffers. <br>
	 * After the buildable buffers have been generated they must be swapped with the
	 * drawable buffers in the LodRenderer to be drawn.
	 * 
	 * @return whether it has started a generation task or is blocked
	 */
	public boolean updateAndSwapLodBuffersAsync(LodRenderer renderer, LodDimension lodDim, int playerX, int playerY,
			int playerZ, boolean partialRegen, boolean flushBuffers) {

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

		if (!fullRegen && !partialRegen)
			return false;

		generatingBuffers = true;

		Runnable thread = () -> generateLodBuffersThread(renderer, lodDim, playerX, playerY, playerZ, fullRegen);

		mainGenThread.execute(thread);
		return true;
	}

	private void generateLodBuffersThread(LodRenderer renderer, LodDimension lodDim, int playerX, int playerY,
			int playerZ, boolean fullRegen) {
		bufferLock.lock();

		long startTime = System.currentTimeMillis();
		ArrayList<RegionPos> posToCleanup = new ArrayList<RegionPos>();
		ArrayList<Callable<Boolean>> nodeToRenderThreads = new ArrayList<Callable<Boolean>>();

		try {
			// round the player's block position down to the nearest chunk BlockPos
			int playerRegionX = LevelPosUtil.convert(LodUtil.BLOCK_DETAIL_LEVEL, playerX, LodUtil.REGION_DETAIL_LEVEL);
			int playerRegionZ = LevelPosUtil.convert(LodUtil.BLOCK_DETAIL_LEVEL, playerZ, LodUtil.REGION_DETAIL_LEVEL);
			int renderRange;
			int vboX;
			int vboY;
			int vboZ;

			boolean tooFar = Math.abs(buildableCenterBlockX - playerX)
					+ Math.abs(buildableCenterBlockZ - playerZ) > 100_000;

			if (fullRegen || tooFar || buildableVbos == null || setsToRender == null) {
				if (buildableVbos != null) {
					buildableVbos.clear(RenderRegion::close);
				}

				renderRange = lodDim.getWidth() / 2; // get lodDim half width
				buildableVbos = new MovableGridList<RenderRegion>(renderRange, playerRegionX, playerRegionZ);
				setsToRender = new MovableGridList<PosToRenderContainer>(renderRange, playerRegionX, playerRegionZ);
				// this will be the center of the VBOs once they have been built
				// FIXME: Currently this will drift apart from player pos if there has not been
				// a fullRegen for a while
				buildableCenterBlockX = playerX;
				buildableCenterBlockY = playerY;
				buildableCenterBlockZ = playerZ;
				vboX = playerX;
				vboY = playerY;
				vboZ = playerZ;
			} else {
				renderRange = buildableVbos.gridCentreToEdge;
				vboX = buildableCenterBlockX;
				vboY = buildableCenterBlockY;
				vboZ = buildableCenterBlockZ;
				buildableVbos.move(playerRegionX, playerRegionZ, RenderRegion::close);
				setsToRender.move(playerRegionX, playerRegionZ);
			}
			posToCleanup.ensureCapacity(buildableVbos.size());
			nodeToRenderThreads.ensureCapacity(buildableVbos.size());

			// ================================//
			// create the nodeToRenderThreads //
			// ================================//

			skyLightPlayer = MC.getWrappedClientWorld().getSkyLight(playerX, playerY, playerZ);
			// int minCullingRange =
			// SingletonHandler.get(ILodConfigWrapperSingleton.class).client().graphics().advancedGraphics().getBacksideCullingRange();
			// int cullingRangeX = Math.max((int)(1.5 * Math.abs(lastX - playerX)),
			// minCullingRange);
			// int cullingRangeZ = Math.max((int)(1.5 * Math.abs(lastZ - playerZ)),
			// minCullingRange);
			lastX = playerX;
			lastZ = playerZ;
			List<CompletableFuture<?>> futuresBuffer = new LinkedList<CompletableFuture<?>>();
			for (int indexX = 0; indexX < buildableVbos.gridSize; indexX++) {
				for (int indexZ = 0; indexZ < buildableVbos.gridSize; indexZ++) {
					final int regionX = indexX + buildableVbos.getCenterX() - buildableVbos.gridCentreToEdge;
					final int regionZ = indexZ + buildableVbos.getCenterY() - buildableVbos.gridCentreToEdge;

					boolean needRegen = lodDim.getAndClearRegionNeedBufferRegen(regionX, regionZ);
					needRegen |= fullRegen;
					if (!needRegen)
						continue;

					LodRegion region = lodDim.getRegion(regionX, regionZ);
					if (region == null)
						continue;

					RegionPos regionPos = new RegionPos(regionX, regionZ);
					posToCleanup.add(regionPos);

					byte minDetail = region.getMinDetailLevel();
					final int pX = playerX;
					final int pZ = playerZ;

					class ResultPair {
						final LodQuadBuilder quadBuilder;
						final RegionPos regionPos;

						ResultPair(LodQuadBuilder quadBuilder, RegionPos regionPos) {
							this.quadBuilder = quadBuilder;
							this.regionPos = regionPos;
						}

						LodQuadBuilder quadBuilder() {
							return quadBuilder;
						}

						RegionPos regionPos() {
							return regionPos;
						}
					}

					CompletableFuture<ResultPair> future = CompletableFuture.supplyAsync(() -> {
						LodQuadBuilder quadBuilder = new LodQuadBuilder(6);
						makeLodRenderData(quadBuilder, lodDim, regionPos, pX, pZ, vboX, vboZ, minDetail);
						return new ResultPair(quadBuilder, regionPos);
					}, bufferUploadThread).whenCompleteAsync((result, e) -> {
						if (e != null)
							return;
						try {
							uploadBuffers(result.quadBuilder, result.regionPos);
						} catch (Exception e3) {
							ClientApi.LOGGER.error("\"LodNodeBufferBuilder\" was unable to upload buffer: ", e3);
						}
					}, bufferUploadThread);
					futuresBuffer.add(future);
				} // region z
			} // region z

			// ================================//
			// execute the nodeToRenderThreads //
			// ================================//

			long executeStart = System.currentTimeMillis();
			// wait for all threads to finish
			CompletableFuture<Void> allFutures = CompletableFuture
					.allOf(futuresBuffer.toArray(new CompletableFuture[futuresBuffer.size()]));
			try {
				allFutures.get(60, TimeUnit.SECONDS);
			} catch (TimeoutException te) {
				ClientApi.LOGGER.error("LodBufferBuilder timed out: ", te);
				bufferBuilderThreadFactory.dumpAllThreadStacks();
				bufferUploadThreadFactory.dumpAllThreadStacks();
				bufferBuilderThreads.shutdownNow();
				bufferUploadThread.shutdownNow();
				bufferBuilderThreadFactory = new LodThreadFactory("BufferBuilder", Thread.NORM_PRIORITY - 2);
				bufferBuilderThreads = Executors.newFixedThreadPool(
						CONFIG.client().advanced().threading().getNumberOfBufferBuilderThreads(),
						bufferBuilderThreadFactory);
				bufferUploadThreadFactory = new LodThreadFactory(
						LodBufferBuilderFactory.class.getSimpleName() + " - upload", Thread.NORM_PRIORITY - 1);
				bufferUploadThread = Executors.newSingleThreadExecutor(bufferUploadThreadFactory);
				return;
			} catch (Exception e) {
				ClientApi.LOGGER.error("LodBufferBuilder ran into trouble: ", e);
			}
			long executeEnd = System.currentTimeMillis();

			long endTime = System.currentTimeMillis();
			long buildTime = endTime - startTime;
			long executeTime = executeEnd - executeStart;
			if (ENABLE_BUFFER_PERF_LOGGING)
				ClientApi.LOGGER.info("Thread Build&Upload(" + nodeToRenderThreads.size() + "/"
						+ (lodDim.getWidth() * lodDim.getWidth()) + (fullRegen ? "FULL" : "") + ") time: " + buildTime
						+ " ms" + '\n' + "thread execute time: " + executeTime + " ms");
			// mark that the buildable buffers as ready to swap
			switchVbos = true;
		} catch (Exception e) {
			ClientApi.LOGGER.error("\"LodNodeBufferBuilder.generateLodBuffersAsync\" ran into trouble: ", e);
		} finally {
			// regardless of whether we were able to successfully create
			// the buffers, we are done generating.
			generatingBuffers = false;
			bufferLock.unlock();
		}
	}

	private RegionPos makeLodRenderData(LodQuadBuilder quadBuilder, LodDimension lodDim, RegionPos regPos, int playerX,
			int playerZ, int vboX, int vboZ, byte minDetail) {// , int cullingRangeX, int cullingRangeZ) {

		// Variable initialization
		int playerChunkX = LevelPosUtil.convert(LodUtil.BLOCK_DETAIL_LEVEL, playerX, LodUtil.CHUNK_DETAIL_LEVEL);
		int playerChunkZ = LevelPosUtil.convert(LodUtil.BLOCK_DETAIL_LEVEL, playerZ, LodUtil.CHUNK_DETAIL_LEVEL);
		DebugMode debugMode = CONFIG.client().advanced().debugging().getDebugMode();

		// We ask the lod dimension which block we have to render given the player
		// position
		PosToRenderContainer posToRender = setsToRender.get(regPos.x, regPos.z);
		// previous setToRender cache
		if (posToRender == null) {
			posToRender = setsToRender.setAndGet(regPos.x, regPos.z,
					new PosToRenderContainer(minDetail, regPos.x, regPos.z));
		}
		posToRender.clear(minDetail, regPos.x, regPos.z);
		lodDim.getPosToRender(posToRender, regPos, playerX, playerZ);

		for (int index = 0; index < posToRender.getNumberOfPos(); index++) {

			byte detailLevel = posToRender.getNthDetailLevel(index);
			int posX = posToRender.getNthPosX(index);
			int posZ = posToRender.getNthPosZ(index);

			long[] posData = lodDim.getAllData(detailLevel, posX, posZ);
			if (posData == null || posData.length == 0 || !DataPointUtil.doesItExist(posData[0])
					|| DataPointUtil.isVoid(posData[0]))
				continue;
			long[][] adjData = new long[4][];

			int chunkXdist = LevelPosUtil.getChunkPos(detailLevel, posX) - playerChunkX;
			int chunkZdist = LevelPosUtil.getChunkPos(detailLevel, posZ) - playerChunkZ;

			// TODO: In the future, We don't need to ignore rendered chunks! Just build it
			// and leave it for the renderer to decide!
			// We don't want to render this fake block if
			// The block is inside the render distance with, is not bigger than a chunk and
			// is positioned in a chunk set as vanilla rendered

			// The block is in the player chunk or in a chunk adjacent to the player
			if (detailLevel <= LodUtil.CHUNK_DETAIL_LEVEL && isThisPositionGoingToBeRendered(
					LevelPosUtil.getChunkPos(detailLevel, posX), LevelPosUtil.getChunkPos(detailLevel, posZ))) {
				continue;
			}

			// we check if the block to render is not in player chunk
			boolean posNotInPlayerChunk = !(chunkXdist == 0 && chunkZdist == 0);

			// We extract the adj data in the four cardinal direction

			// we first reset the adjShadeDisabled. This is used to disable the shade on the
			// border when we have transparent block like water or glass
			// to avoid having a "darker border" underground
			// Arrays.fill(adjShadeDisabled, false);

			// We check every adj block in each direction
			for (LodDirection lodDirection : LodDirection.ADJ_DIRECTIONS) {
				int xAdj = posX + lodDirection.getNormal().x;
				int zAdj = posZ + lodDirection.getNormal().z;
				chunkXdist = LevelPosUtil.getChunkPos(detailLevel, xAdj) - playerChunkX;
				chunkZdist = LevelPosUtil.getChunkPos(detailLevel, zAdj) - playerChunkZ;
				boolean adjPosInPlayerChunk = (chunkXdist == 0 && chunkZdist == 0);

				// If the adj block is rendered in the same region and with same detail
				// and is positioned in a place that is not going to be rendered by vanilla game
				// then we can set this position as adj
				// We avoid cases where the adjPosition is in player chunk while the position is
				// not
				// to always have a wall underwater
				if (posToRender.contains(detailLevel, xAdj, zAdj)
						&& !isThisPositionGoingToBeRendered(LevelPosUtil.getChunkPos(detailLevel, xAdj),
								LevelPosUtil.getChunkPos(detailLevel, zAdj))
						&& !(posNotInPlayerChunk && adjPosInPlayerChunk)) {
					adjData[lodDirection.ordinal() - 2] = lodDim.getAllData(detailLevel, xAdj, zAdj);
				}
			}

			// We render every vertical lod present in this position
			// We only stop when we find a block that is void or non-existing block
			for (int i = 0; i < posData.length; i++) {
				long data = posData[i];
				// If the data is not renderable (Void or non-existing) we stop since there is
				// no data left in this position
				if (DataPointUtil.isVoid(data) || !DataPointUtil.doesItExist(data))
					break;

				long adjDataTop = i - 1 >= 0 ? posData[i - 1] : DataPointUtil.EMPTY_DATA;
				long adjDataBot = i + 1 < posData.length ? posData[i + 1] : DataPointUtil.EMPTY_DATA;

				// We send the call to create the vertices
				CubicLodTemplate.addLodToBuffer(data, adjDataTop, adjDataBot, adjData, detailLevel,
						LevelPosUtil.getRegionModule(detailLevel, posX),
						LevelPosUtil.getRegionModule(detailLevel, posZ), quadBuilder, debugMode);
			}

		} // for pos to in list to render
			// the thread executed successfully
		return regPos;
	}

	// Will be removed in a1.7
	@Deprecated
	private boolean isThisPositionGoingToBeRendered(int chunkX, int chunkZ) {
		MovableGridList<Boolean> chunkGrid = ClientApi.renderer.vanillaRenderedChunks;
		Boolean isRendered = chunkGrid.get(chunkX, chunkZ);

		// skip any chunks that Minecraft is going to render
		if (isRendered == null || !isRendered)
			return false;

		// check if the chunk is on the border
		if (CONFIG.client().graphics().advancedGraphics().getVanillaOverdraw() == VanillaOverdraw.BORDER)
			return !LodUtil.isBorderChunk(ClientApi.renderer.vanillaRenderedChunks, chunkX, chunkZ);
		else
			return true;
	}

	private final SpamReducedLogger ramLogger = new SpamReducedLogger(1);

	public void dumpBufferMemoryUsage() {
		if (!ramLogger.canMaybeLog())
			return;
		ramLogger.info("Dumping Ram Usage for buffer usage...");
		long bufferCount = 0;
		long fullBufferCount = 0;
		long totalUsage = 0;
		int maxLength = MAX_TRIANGLES_PER_BUFFER * (LodUtil.LOD_VERTEX_FORMAT.getByteSize() * 3);
		if (buildableVbos == null) {
			ramLogger.info("Buildable VBOs are null!");
		} else
			for (RenderRegion buffers : buildableVbos) {
				if (buffers == null)
					continue;
				LodVertexBuffer[] bs = buffers.debugGetBuffers().clone();
				for (LodVertexBuffer b : bs) {
					if (b == null)
						continue;
					bufferCount++;
					if (b.size == maxLength) {
						fullBufferCount++;
					} else if (b.size > maxLength) {
						ramLogger.info("BUFFER OVERSIZED: {} (max size is {})", new UnitBytes(b.size),
								new UnitBytes(maxLength));
					}
					totalUsage += b.size;
				}
			}
		if (drawableVbos == null) {
			ramLogger.info("Drawable VBOs are null!");
		} else
			for (RenderRegion buffers : drawableVbos) {
				if (buffers == null)
					continue;
				LodVertexBuffer[] bs = buffers.debugGetBuffers().clone();
				for (LodVertexBuffer b : bs) {
					if (b == null)
						continue;
					bufferCount++;
					if (b.size == maxLength) {
						fullBufferCount++;
					} else if (b.size > maxLength) {
						ramLogger.info("BUFFER OVERSIZED: {} (max size is {})", new UnitBytes(b.size),
								new UnitBytes(maxLength));
					}
					totalUsage += b.size;
				}
			}
		ramLogger.info("================================================");
		ramLogger.info("Buffers: [{}], Full-sized Buffers: [{}], Total: [{}]", bufferCount, fullBufferCount,
				new UnitBytes(totalUsage));
		ramLogger.info("================================================");
		ramLogger.incLogTries();
	}

	// ===============================//
	// BufferBuilder related methods //
	// ===============================//

	/**
	 * Sets the buffers and Vbos to null, forcing them to be recreated <br>
	 * and destroys any bound OpenGL objects. <br>
	 * <br>
	 * <p>
	 * May have to wait for the bufferLock to open.
	 */
	public void destroyBuffers() {
		MovableGridList<RenderRegion> toBeDeletedBuildableVbos;
		MovableGridList<RenderRegion> toBeDeletedDrawableVbos;
		bufferLock.lock();
		try {
			toBeDeletedBuildableVbos = buildableVbos;
			toBeDeletedDrawableVbos = drawableVbos;
			buildableVbos = null;
			drawableVbos = null;
		} finally {
			bufferLock.unlock();
		}
		// make sure the buffers are deleted in a openGL context
		GLProxy.getInstance().recordOpenGlCall(() -> {
			// destroy the VBOs if they aren't already
			if (toBeDeletedBuildableVbos != null) {
				toBeDeletedBuildableVbos.clear(RenderRegion::close);
			}
			if (toBeDeletedDrawableVbos != null) {
				toBeDeletedDrawableVbos.clear(RenderRegion::close);
			}
		});
	}

	/**
	 * Upload all buildableBuffers to the GPU. We should already be in the builder
	 * context
	 */
	private void uploadBuffers(LodQuadBuilder quadBuilder, RegionPos p) {
		GLProxy glProxy = GLProxy.getInstance();
		GLProxyContext oldContext = glProxy.getGlContext();
		glProxy.setGlContext(GLProxyContext.LOD_BUILDER);
		try {
			// determine the upload method
			GpuUploadMethod uploadMethod = glProxy.getGpuUploadMethod();

			// determine the upload timeout
			long BPerNS = CONFIG.client().advanced().buffers().getGpuUploadPerMegabyteInMilliseconds(); // MB -> B =
																										// 1/1,000,000.
																										// MS -> NS =
																										// 1,000,000.
																										// So, MBPerMS =
																										// BPerNS.
			long remainingNS = 0; // We don't want to pause for like 0.1 ms... so we store those tiny MS.
			long totalBytes = 0;
			int vbosNeeded = quadBuilder.getCurrentNeededVertexBuffers();

			// Setup the VBO array
			LagSpikeCatcher vboSetup = new LagSpikeCatcher();
			RenderRegion renderRegion = buildableVbos.get(p.x, p.z);
			if (renderRegion == null)
				renderRegion = buildableVbos.setAndGet(p.x, p.z, new RenderRegion(vbosNeeded));
			else
				renderRegion.resize(vbosNeeded);
			vboSetup.end("vboSetup");

			// Start the upload
			Iterator<ByteBuffer> iter = quadBuilder.makeVertexBuffers();
			int i = 0;
			while (iter.hasNext()) {
				ByteBuffer bb = iter.next();
				LagSpikeCatcher vboU = new LagSpikeCatcher();
				vboUpload(p, i++, bb, uploadMethod);
				vboU.end("vboUpload");

				// upload buffers over an extended period of time
				// to hopefully prevent stuttering.
				totalBytes += bb.limit();
				remainingNS += bb.limit() * BPerNS;
				if (remainingNS >= TimeUnit.NANOSECONDS.convert(1000 / 60, TimeUnit.MILLISECONDS)) {
					if (remainingNS > MAX_BUFFER_UPLOAD_TIMEOUT_NANOSECONDS)
						remainingNS = MAX_BUFFER_UPLOAD_TIMEOUT_NANOSECONDS;
					try {
						Thread.sleep(remainingNS / 1000000, (int) (remainingNS % 1000000));
					} catch (InterruptedException e) {
					}
					remainingNS = 0;
				}
			}
			if (ENABLE_BUFFER_UPLOAD_LOGGING)
				ClientApi.LOGGER.info("Uploaded {} sub buffers for {} with total of {}", i, p,
						new UnitBytes(totalBytes));
		} finally {
			glProxy.setGlContext(oldContext);
		}
	}

	/** Uploads the uploadBuffer so the GPU can use it. */
	private void vboUpload(RegionPos regPos, int iIndex, ByteBuffer uploadBuffer, GpuUploadMethod uploadMethod) {
		int maxLength = MAX_TRIANGLES_PER_BUFFER * (LodUtil.LOD_VERTEX_FORMAT.getByteSize() * 3);
		boolean useBuffStorage = uploadMethod == GpuUploadMethod.BUFFER_STORAGE;
		RenderRegion region = buildableVbos.get(regPos.x, regPos.z);
		LodVertexBuffer vbo = region.getOrMakeVbo(iIndex, useBuffStorage);

		// this is how many points will be rendered
		vbo.vertexCount = (uploadBuffer.limit() / LodUtil.LOD_VERTEX_FORMAT.getByteSize());

		// If size is zero, just ignore it.
		if (uploadBuffer.limit() == 0)
			return;
		LagSpikeCatcher bindBuff = new LagSpikeCatcher();
		bindBuff.end("glBindBuffer vbo.id");

		try {
			// if possible use the faster buffer storage route
			if (uploadMethod == GpuUploadMethod.BUFFER_STORAGE) {
				GL32.glBindBuffer(GL32.GL_ARRAY_BUFFER, vbo.id);
				long size = vbo.size;
				if (size < uploadBuffer.limit()
						|| size > uploadBuffer.limit() * BUFFER_EXPANSION_MULTIPLIER * BUFFER_EXPANSION_MULTIPLIER) {
					int newSize = (int) (uploadBuffer.limit() * BUFFER_EXPANSION_MULTIPLIER);
					if (newSize > maxLength)
						newSize = maxLength;
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
			} else if (uploadMethod == GpuUploadMethod.BUFFER_MAPPING) {
				GL32.glBindBuffer(GL32.GL_ARRAY_BUFFER, vbo.id);
				// no stuttering but high GPU usage
				// stores everything in system memory instead of GPU memory
				// making rendering much slower.
				// Unless the user is running integrated graphics,
				// in that case this will actually work better than SUB_DATA.
				long size = vbo.size;
				if (size < uploadBuffer.limit()
						|| size > uploadBuffer.limit() * BUFFER_EXPANSION_MULTIPLIER * BUFFER_EXPANSION_MULTIPLIER) {
					int newSize = (int) (uploadBuffer.limit() * BUFFER_EXPANSION_MULTIPLIER);
					if (newSize > maxLength)
						newSize = maxLength;
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
			} else if (uploadMethod == GpuUploadMethod.DATA) {
				GL32.glBindBuffer(GL32.GL_ARRAY_BUFFER, vbo.id);
				// TODO: Check this nonsense comment!
				// hybrid bufferData //
				// high stutter, low GPU usage
				// But simplest/most compatible
				LagSpikeCatcher buffData = new LagSpikeCatcher();
				GL32.glBufferData(GL32.GL_ARRAY_BUFFER, uploadBuffer, GL32.GL_STATIC_DRAW);
				vbo.size = uploadBuffer.limit();
				buffData.end("glBufferData Data");
			} else {
				GL32.glBindBuffer(GL32.GL_ARRAY_BUFFER, vbo.id);
				// TODO: Check this nonsense comment!
				// hybrid subData/bufferData //
				// less stutter, low GPU usage
				long size = vbo.size;
				if (size < uploadBuffer.limit()
						|| size > uploadBuffer.limit() * BUFFER_EXPANSION_MULTIPLIER * BUFFER_EXPANSION_MULTIPLIER) {
					int newSize = (int) (uploadBuffer.limit() * BUFFER_EXPANSION_MULTIPLIER);
					if (newSize > maxLength)
						newSize = maxLength;
					LagSpikeCatcher buffResize = new LagSpikeCatcher();
					GL32.glBufferData(GL32.GL_ARRAY_BUFFER, newSize, GL32.GL_STATIC_DRAW);
					vbo.size = newSize;
					buffResize.end("glBufferData SubData resize");
				}
				LagSpikeCatcher buffSubData = new LagSpikeCatcher();
				GL32.glBufferSubData(GL32.GL_ARRAY_BUFFER, 0, uploadBuffer);
				buffSubData.end("glBufferSubData SubData");
			}
		} catch (Exception e) {
			ClientApi.LOGGER.error("vboUpload failed: " + e.getClass().getSimpleName());
			e.printStackTrace();
		}
	}// vboUpload

	private boolean swapBuffers() {
		bufferLock.lock();
		if (ENABLE_BUFFER_SWAP_LOGGING)
			ClientApi.LOGGER.debug("Lod Swap Buffers");
		{
			boolean shouldRegenBuff = true;
			try {
				MovableGridList<RenderRegion> tmpVbo = drawableVbos;
				drawableVbos = buildableVbos;
				buildableVbos = tmpVbo;

				// ClientApi.LOGGER.info("Lod Swapped Buffers: "+drawableVbos.toDetailString());

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

				// FIXME: Race condition on the allBuffersRequireReset boolean
				shouldRegenBuff = frontBufferRequireReset || allBuffersRequireReset;
				frontBufferRequireReset = allBuffersRequireReset;
				allBuffersRequireReset = false;
				hideFrontBuffer = hideBackBuffer;
				hideBackBuffer = false;
			} catch (Exception e) {
				// this shouldn't normally happen, but just in case it sill prevent deadlock
				ClientApi.LOGGER.error("swapBuffers ran into trouble: " + e.getMessage(), e);
			} finally {
				bufferLock.unlock();
			}
			return shouldRegenBuff;
		}
	}

	/** Get the newly created VBOs */
	public MovableGridList<RenderRegion> getFrontBuffers() {
		return shouldDrawFrontBuffer() ? drawableVbos : null;
	}

	public int getFrontBuffersCenterX() {
		return drawableCenterBlockX;
	}

	public int getFrontBuffersCenterZ() {
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
