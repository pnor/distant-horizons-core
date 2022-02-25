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

import java.time.Duration;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.logging.log4j.core.tools.picocli.CommandLine.ExecutionException;

import com.seibel.lod.core.api.ApiShared;
import com.seibel.lod.core.objects.lod.LodDimension;
import com.seibel.lod.core.objects.lod.RegionPos;
import com.seibel.lod.core.objects.opengl.RenderRegion;
import com.seibel.lod.core.render.LodRenderer;
import com.seibel.lod.core.util.LevelPosUtil;
import com.seibel.lod.core.util.LodThreadFactory;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.util.MovableGridRingList;
import com.seibel.lod.core.util.SingletonHandler;
import com.seibel.lod.core.util.SpamReducedLogger;
import com.seibel.lod.core.util.StatsMap;
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
	public static final boolean ENABLE_BUFFER_PERF_LOGGING = true;
	public static final boolean ENABLE_EVENT_LOGGING = true;
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
				ApiShared.LOGGER.info("LagSpikeCatcher: " + source + " took " + Duration.ofNanos(timer) + "!");
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
	
	/**
	 * When buffers are first created they are allocated to this size (in Bytes).
	 * This size will be too small, more than likely. The buffers will be expanded
	 * when need be to fit the larger sizes.
	 */
	public static final int DEFAULT_MEMORY_ALLOCATION = (LodUtil.LOD_VERTEX_FORMAT.getByteSize() * 3) * 8;
	public static final int MAX_TRIANGLES_PER_BUFFER = (1024 * 1024 * 1)
			/ (LodUtil.LOD_VERTEX_FORMAT.getByteSize() * 3);

	public static int skyLightPlayer = 15;

	public MovableGridRingList<RenderRegion> renderRegions = null;
	
	/** Size of the buffer builders in bytes last time we created them */
	public int previousBufferSize = 0;
	/** Width of the dimension in regions last time we created the buffers */
	public int previousRegionWidth = 0;

	private boolean builderThreadRunning = false;
	
	public ReentrantLock regionsListLock = new ReentrantLock();
	
	public LodBufferBuilderFactory() {
		
	}

	public void setRegionNeedRegen(int regionX, int regionZ) {
		MovableGridRingList<RenderRegion> r = renderRegions;
		if (r==null) return;
		RenderRegion rr = r.get(regionX, regionZ);
		if (rr==null) return;
		rr.setNeedRegen();
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
			int playerZ, boolean fullRegen) {

		// only allow one generation process to happen at a time
		if (builderThreadRunning) return false;

		if (MC.getCurrentLightMap() == null)
			// the lighting hasn't loaded yet
			return false;

		builderThreadRunning = true;

		Runnable thread = () -> generateLodBuffersThread(renderer, lodDim, playerX, playerY, playerZ, fullRegen);
		mainGenThread.execute(thread);
		return true;
	}
	
	private void updateRingList(int playerX, int playerZ, int regionWidth) throws InterruptedException {
		if (renderRegions != null && regionWidth != renderRegions.getSize()) {
			renderRegions.clear(RenderRegion::close);
			renderRegions = null;
		}
		LodUtil.checkInterrupts();
		if (renderRegions == null) {
			renderRegions = new MovableGridRingList<RenderRegion>(regionWidth/2,
					LevelPosUtil.getRegion(LodUtil.BLOCK_DETAIL_LEVEL, playerX),
					LevelPosUtil.getRegion(LodUtil.BLOCK_DETAIL_LEVEL, playerZ));
			ApiShared.LOGGER.info("============Render Regions rebuilt============");
		} else {
			renderRegions.move(LevelPosUtil.getRegion(LodUtil.BLOCK_DETAIL_LEVEL, playerX),
					LevelPosUtil.getRegion(LodUtil.BLOCK_DETAIL_LEVEL, playerZ), RenderRegion::close);
		}
	}
	
	private void resetThreadPools(boolean dumpThread) {
		if (dumpThread) {
			bufferBuilderThreadFactory.dumpAllThreadStacks();
			bufferUploadThreadFactory.dumpAllThreadStacks();
		}
		bufferBuilderThreads.shutdownNow();
		bufferUploadThread.shutdownNow();
		
		bufferBuilderThreadFactory = new LodThreadFactory("BufferBuilder", Thread.NORM_PRIORITY - 2);
		bufferBuilderThreads = Executors.newFixedThreadPool(
				CONFIG.client().advanced().threading().getNumberOfBufferBuilderThreads(),
				bufferBuilderThreadFactory);
		
		bufferUploadThreadFactory = new LodThreadFactory(
				LodBufferBuilderFactory.class.getSimpleName() + " - upload", Thread.NORM_PRIORITY - 1);
		bufferUploadThread = Executors.newSingleThreadExecutor(bufferUploadThreadFactory);
		
	}
	
	private void generateLodBuffersThread(LodRenderer renderer, LodDimension lodDim, int playerX, int playerY,
			int playerZ, boolean fullRegen) {
		//ArrayList<RenderRegion> regionsToCleanup = new ArrayList<RenderRegion>();
		try {
			regionsListLock.lockInterruptibly();
			if (ENABLE_EVENT_LOGGING)
				ApiShared.LOGGER.info("BufferBuilderStarter locked the region lock! LodDim: [{}], RenderRegion: [{}]",
					lodDim, renderRegions==null ? "NULL" : renderRegions.toString());
			long startTime = System.currentTimeMillis();

			try {
				updateRingList(playerX, playerZ, lodDim.getWidth());

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
				
				MovableGridRingList.Pos minPos = renderRegions.getMinInRange();
				MovableGridRingList.Pos maxPos = renderRegions.getMaxInRange();
				CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
				
				try {
					int numOfJobs = 0;
					for (int regX = minPos.x; regX < maxPos.x; regX++) {
						for (int regZ = minPos.y; regZ < maxPos.y; regZ++) {
							RenderRegion r = renderRegions.get(regX, regZ);
							RegionPos regPos = new RegionPos(regX, regZ);
							if (r!=null && !r.canRender(lodDim, regPos)) {
								renderRegions.set(regX, regZ, null);
								r.close();
								r = null;
							}
							
							if (r==null) {
								r = new RenderRegion(regPos, lodDim);
								renderRegions.set(regX, regZ, r);
							}
							
							CompletableFuture<Void> newFuture =
									r.updateStatus(bufferUploadThread, bufferBuilderThreads, fullRegen, playerX, playerZ).orElse(null);
							if (newFuture != null) {
								future = CompletableFuture.allOf(future, newFuture);
								numOfJobs++;
							}
						}
					}

					// ================================//
					//        wait on completion       //
					// ================================//
					
					long executeStart = System.currentTimeMillis();
					try {
						future.get(1, TimeUnit.MINUTES);
					} catch (InterruptedException | TimeoutException ie) {
						throw ie;
					} catch (CancellationException ce) {
						throw new InterruptedException("Future interrupted");
					} catch (ExecutionException ee) {
						ApiShared.LOGGER.error("LodBufferBuilder ran into trouble: ", ee.getCause());
					}
					long executeEnd = System.currentTimeMillis();
					
					long endTime = System.currentTimeMillis();
					long buildTime = endTime - startTime;
					long executeTime = executeEnd - executeStart;
					if (ENABLE_BUFFER_PERF_LOGGING)
						ApiShared.LOGGER.info("Thread Build&Upload(" + numOfJobs + "/"
								+ (lodDim.getWidth() * lodDim.getWidth()) + (fullRegen ? "FULL" : "") + ") time: " + buildTime
								+ " ms" + '\n' + "thread execute time: " + executeTime + " ms");
				
				} catch (InterruptedException ie) {
					resetThreadPools(false);
					try {
						future.get();
					} catch (Throwable t) {}
					throw ie;
				} catch (TimeoutException te) {
					ApiShared.LOGGER.error("LodBufferBuilder timed out: ", te);
					resetThreadPools(true);
				}
			} catch (InterruptedException ie) {
			} catch (Exception e) {
				ApiShared.LOGGER.error("\"LodNodeBufferBuilder.generateLodBuffersAsync\" ran into trouble: ", e);
			} finally {
				if (ENABLE_EVENT_LOGGING) ApiShared.LOGGER.info("BufferBuilderStarter unlocked the region lock!");
				regionsListLock.unlock();
			}
		} catch (InterruptedException ie) {
		} finally {
			builderThreadRunning = false;
		}
	}

	private final SpamReducedLogger ramLogger = new SpamReducedLogger(1);

	public void dumpBufferMemoryUsage() {
		if (!ramLogger.canMaybeLog())
			return;
		ramLogger.info("Dumping Ram Usage for buffer usage...");
		StatsMap statsMap = new StatsMap();
		
		if (renderRegions == null) {
			ramLogger.info("Buildable VBOs are null!");
		} else
			for (RenderRegion buffers : renderRegions) {
				if (buffers == null)
					continue;
				buffers.debugDumpStats(statsMap);
			}
		ramLogger.info("================================================");
		ramLogger.info("Stats: {}", statsMap);
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
		ApiShared.LOGGER.info("LodBufferBuilder Destroy");
		mainGenThread.shutdownNow();
		mainGenThread = Executors.newSingleThreadExecutor(mainGenThreadFactory);
		regionsListLock.lock();
		try {
			if (renderRegions != null) renderRegions.clear(RenderRegion::close);
			renderRegions = null;
		} finally {
			regionsListLock.unlock();
		}
	}

	/** Get the newly created VBOs
	 *  Note: SHOULD NEVER MODIFY THE LIST */
	public MovableGridRingList<RenderRegion> getRenderRegions() {
		return renderRegions;
	}

	@Deprecated
	public void triggerReset() {
		
	}
}
