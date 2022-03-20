package com.seibel.lod.core.objects.opengl;

import java.util.ConcurrentModificationException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.seibel.lod.core.api.ApiShared;
import com.seibel.lod.core.api.ClientApi;
import com.seibel.lod.core.builders.bufferBuilding.CubicLodTemplate;
import com.seibel.lod.core.builders.lodBuilding.LodBuilder;
import com.seibel.lod.core.enums.LodDirection;
import com.seibel.lod.core.enums.config.GpuUploadMethod;
import com.seibel.lod.core.enums.config.VanillaOverdraw;
import com.seibel.lod.core.enums.rendering.DebugMode;
import com.seibel.lod.core.enums.rendering.GLProxyContext;
import com.seibel.lod.core.handlers.dependencyInjection.SingletonHandler;
import com.seibel.lod.core.objects.BoolType;
import com.seibel.lod.core.objects.PosToRenderContainer;
import com.seibel.lod.core.objects.lod.LodDimension;
import com.seibel.lod.core.objects.lod.LodRegion;
import com.seibel.lod.core.objects.lod.RegionPos;
import com.seibel.lod.core.objects.math.Vec3d;
import com.seibel.lod.core.objects.math.Vec3f;
import com.seibel.lod.core.render.GLProxy;
import com.seibel.lod.core.render.LodRenderProgram;
import com.seibel.lod.core.render.RenderUtil;
import com.seibel.lod.core.util.DataPointUtil;
import com.seibel.lod.core.util.DetailDistanceUtil;
import com.seibel.lod.core.util.LevelPosUtil;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.util.gridList.MovableCenteredGridList;
import com.seibel.lod.core.util.StatsMap;
import com.seibel.lod.core.util.gridList.PosArrayGridList;
import com.seibel.lod.core.wrapperInterfaces.block.AbstractBlockPosWrapper;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton;

public class RenderRegion implements AutoCloseable
{
	public static final boolean ENABLE_EVENT_LOGGING = false;
	public static final boolean ENABLE_EVENT_STEP_LOGGING = false;
	public static final boolean ENABLE_VERBOSE_LOGGING = false;
	private static final ILodConfigWrapperSingleton CONFIG = SingletonHandler.get(ILodConfigWrapperSingleton.class);
	
	/** stores if the region at the given x and z index needs to be regenerated */
	// Use int because I need Tri state:
	private AtomicInteger needRegen = new AtomicInteger(2);
	
	private enum BackState {
		Unused,
		Building,
		Complete,
	}
	private enum FrontState {
		Unused,
		Rendering,
		Invalidated,
	}
	
	final RegionPos regionPos;
	RenderBuffer renderBufferBack = null;
	AtomicReference<BackState> backState =
			new AtomicReference<BackState>(BackState.Unused);
	AtomicReference<FrontState> frontState =
			new AtomicReference<FrontState>(FrontState.Unused);
	RenderBuffer renderBufferFront = null;
	final LodDimension lodDim;
	
	public RenderRegion(RegionPos regPos, LodDimension lodDim) {
		regionPos = regPos;
		this.lodDim = lodDim;
	}
	
	public boolean canRender(LodDimension lodDim, RegionPos regPos) {
		return lodDim == this.lodDim && regPos.equals(regionPos);
	}
	
	public void setNeedRegen() {
		needRegen.set(2);
	} 
	
	public Optional<CompletableFuture<Void>> updateStatus(Executor bufferUploader, Executor bufferBuilder, boolean alwaysRegen, int playerPosX, int playerPosZ) {
		if (alwaysRegen) setNeedRegen();
		
		BackState state = backState.get();
		if (state != BackState.Unused) {
			if (ENABLE_VERBOSE_LOGGING) ApiShared.LOGGER.info("{}: UpdateStatus rejected. Cause: BackState is {}", regionPos, state);
			return Optional.empty();
		}

		LodRegion r = lodDim.getRegion(regionPos.x, regionPos.z);
		if (r==null) {
			if (ENABLE_VERBOSE_LOGGING) ApiShared.LOGGER.info("{}: UpdateStatus rejected. Cause: Region is null", regionPos);
			return Optional.empty();
		}
		if (needRegen.get() == 0) {
			if (ENABLE_VERBOSE_LOGGING) ApiShared.LOGGER.info("{}: UpdateStatus rejected. Cause: Region doesn't need regen", regionPos);
			return Optional.empty();
		}
		
		if (!backState.compareAndSet(BackState.Unused, BackState.Building)) {
			if (ENABLE_VERBOSE_LOGGING) ApiShared.LOGGER.info("{}: UpdateStatus rejected. Cause: CAS on BackState failed: ", backState.get());
			return Optional.empty();
		}
		needRegen.decrementAndGet();
		return Optional.of(startBuid(bufferUploader, bufferBuilder, r, lodDim, playerPosX, playerPosZ));
	}
	
	public boolean render(LodDimension renderDim,
			Vec3d cameraPos, AbstractBlockPosWrapper cameraBlockPos, Vec3f cameraDir,
			boolean enableDirectionalCulling, LodRenderProgram program) {
		if (!frontState.compareAndSet(FrontState.Unused, FrontState.Rendering)) return false;
		try {
		if (renderDim != lodDim) return false;
		if (enableDirectionalCulling &&
				!RenderUtil.isRegionInViewFrustum(cameraBlockPos,
						cameraDir, regionPos.x, regionPos.z)) return false;
		BackState state = backState.get();
		if (state == BackState.Complete) {
			if (renderBufferBack != null) {
				if (ENABLE_EVENT_LOGGING) ApiShared.LOGGER.info("RenderRegion swap @ {}", regionPos);
				boolean shouldKeep = renderBufferFront != null && renderBufferFront.onSwapToBack();
				RenderBuffer temp = shouldKeep ? renderBufferFront : null;
				renderBufferFront = renderBufferBack;
				renderBufferBack = temp;
				if (renderBufferFront != null) renderBufferFront.onSwapToFront();
			}
			if (!backState.compareAndSet(BackState.Complete, BackState.Unused)) {
				ApiShared.LOGGER.error("RenderRegion.render() got illegal state on swapping buffer!");
			}
		}
		if (renderBufferFront == null) return false;
		program.setModelPos(new Vec3f(
				(float) ((regionPos.x * LodUtil.REGION_WIDTH) - cameraPos.x),
				(float) (LodBuilder.MIN_WORLD_HEIGHT - cameraPos.y),
				(float) ((regionPos.z * LodUtil.REGION_WIDTH) - cameraPos.z)));

		return renderBufferFront.render(program);
		} finally {
			frontState.compareAndSet(FrontState.Rendering, FrontState.Unused);
		}
		 
	}
	
	private void recreateBuffer(LodQuadBuilder builder) {
		if (renderBufferBack != null) throw new RuntimeException("Assert Error");
		boolean useSimpleBuffer = (builder.getCurrentNeededVertexBuffers() <= 6) || true;
		renderBufferBack = useSimpleBuffer ?
				new SimpleRenderBuffer()
				: null; //new ComplexRenderRegion(regPos);
	}
	
	private CompletableFuture<Void> startBuid(Executor bufferUploader, Executor bufferBuilder, LodRegion region, LodDimension lodDim, int playerPosX, int playerPosZ) {
		if (ENABLE_EVENT_LOGGING) ApiShared.LOGGER.info("RenderRegion startBuild @ {}", regionPos);
		LodRegion[] adjRegions = new LodRegion[4];
		try {
			if (renderBufferBack != null) renderBufferBack.onReuse();
			for (LodDirection dir : LodDirection.ADJ_DIRECTIONS) {
				adjRegions[dir.ordinal() - 2] = lodDim.getRegion(regionPos.x+dir.getNormal().x, regionPos.z+dir.getNormal().z);
			}
		} catch (Throwable t) {
			setNeedRegen();
			if (!backState.compareAndSet(BackState.Building, BackState.Unused)) {
				ApiShared.LOGGER.error("\"Lod Builder Starter\""
					+ " encountered error on catching exceptions and fallback on starting build task: ",
					new ConcurrentModificationException("RenderRegion Illegal State"));
			}
			throw t;
		}
		return CompletableFuture.supplyAsync(() -> {
			try {
				if (ENABLE_EVENT_STEP_LOGGING) ApiShared.LOGGER.info("RenderRegion start QuadBuild @ {}", regionPos);
				boolean useSkylightCulling = CONFIG.client().graphics().advancedGraphics().getEnableCaveCulling();
				useSkylightCulling &= !lodDim.dimension.hasCeiling();
				useSkylightCulling &= lodDim.dimension.hasSkyLight();
				LodQuadBuilder builder = new LodQuadBuilder(10, useSkylightCulling);
				Runnable buildRun = ()->{
					makeLodRenderData(builder, region, adjRegions, playerPosX, playerPosZ);
				};
				if (renderBufferBack != null)
					renderBufferBack.build(buildRun);
				else
					buildRun.run();
				if (ENABLE_EVENT_STEP_LOGGING) ApiShared.LOGGER.info("RenderRegion end QuadBuild @ {}", regionPos);
				return builder;
			} catch (Throwable e3) {
				ApiShared.LOGGER.error("\"LodNodeBufferBuilder\" was unable to build quads: ", e3);
				throw e3;
			}
		}, bufferBuilder)
				
				.thenAcceptAsync((builder) -> {
			try {
				if (ENABLE_EVENT_STEP_LOGGING) ApiShared.LOGGER.info("RenderRegion start Upload @ {}", regionPos);
				GLProxy glProxy = GLProxy.getInstance();
				GpuUploadMethod method = GLProxy.getInstance().getGpuUploadMethod();
				GLProxyContext oldContext = glProxy.getGlContext();
				glProxy.setGlContext(GLProxyContext.LOD_BUILDER);
				try {
					if (renderBufferBack == null) recreateBuffer(builder);
					if (!renderBufferBack.tryUploadBuffers(builder, method)) {
						renderBufferBack = null;
						recreateBuffer(builder);
						if (!renderBufferBack.tryUploadBuffers(builder, method)) {
							throw new RuntimeException("Newly created renderBuffer "
									+ "is still returning false on tryUploadBuffers!");
						}
					}
				} finally {
					glProxy.setGlContext(oldContext);
				}
				if (ENABLE_EVENT_STEP_LOGGING) ApiShared.LOGGER.info("RenderRegion end Upload @ {}", regionPos);
			} catch (Throwable e3) {
				ApiShared.LOGGER.error("\"LodNodeBufferBuilder\" was unable to upload buffer: ", e3);
				throw e3;
			}
		}, bufferUploader).handle((v, e) -> {
			if (e != null) {
				setNeedRegen();
				if (!backState.compareAndSet(BackState.Building, BackState.Unused)) {
					ApiShared.LOGGER.error("\"LodNodeBufferBuilder\""
						+ " encountered error on exit: ",
						new ConcurrentModificationException("RenderRegion Illegal State"));
				}
			} else {
				if (!backState.compareAndSet(BackState.Building, BackState.Complete)) {
					ApiShared.LOGGER.error("\"LodNodeBufferBuilder\""
							+ " encountered error on exit: ",
							new ConcurrentModificationException("RenderRegion Illegal State"));
				}
			}
			return (Void) null;
		});
	}

	private static final int ADJACENT8[][] = {
			{-1,-1},
			{-1, 0},
			{-1, 1},
			{ 0,-1},
			//{ 0, 0},
			{ 0, 1},
			{ 1,-1},
			{ 1, 0},
			{ 1, 1}
	};

	private static void makeLodRenderData(LodQuadBuilder quadBuilder, LodRegion region, LodRegion[] adjRegions, int playerX,
			int playerZ) {
		byte minDetail = region.getMinDetailLevel();
		
		// Variable initialization
		DebugMode debugMode = CONFIG.client().advanced().debugging().getDebugMode();

		// We ask the lod dimension which block we have to render given the player
		// position
		PosToRenderContainer posToRender = new PosToRenderContainer(minDetail, region.regionPosX, region.regionPosZ);
		region.getPosToRender(posToRender, playerX, playerZ);
		PosArrayGridList<BoolType> chunkGrid = ClientApi.renderer.vanillaChunks;

		for (int index = 0; index < posToRender.getNumberOfPos(); index++) {

			byte detailLevel = posToRender.getNthDetailLevel(index);
			int posX = posToRender.getNthPosX(index);
			int posZ = posToRender.getNthPosZ(index);
			
			// TODO: In the future, We don't need to ignore rendered chunks! Just build it
			// and leave it for the renderer to decide!
			// We don't want to render this fake block if
			// The block is inside the render distance with, is not bigger than a chunk and
			// is positioned in a chunk set as vanilla rendered

			// The block is in the player chunk or in a chunk adjacent to the player
			if (detailLevel <= LodUtil.CHUNK_DETAIL_LEVEL) {
				int chunkX = LevelPosUtil.getChunkPos(detailLevel, posX);
				int chunkZ = LevelPosUtil.getChunkPos(detailLevel, posZ);
				// skip any chunks that Minecraft is going to render
				if (chunkGrid.get(chunkX, chunkZ) != null) continue;
			}

			long[] posData = region.getAllData(detailLevel, posX, posZ);
			if (posData == null || posData.length == 0 || !DataPointUtil.doesItExist(posData[0])
					|| DataPointUtil.isVoid(posData[0]))
				continue;
			
			long[][][] adjData = new long[4][1][];

			// We extract the adj data in the four cardinal direction

			// we first reset the adjShadeDisabled. This is used to disable the shade on the
			// border when we have transparent block like water or glass
			// to avoid having a "darker border" underground
			// Arrays.fill(adjShadeDisabled, false);
			
			// We check every adj block in each direction
			
			// If the adj block is rendered in the same region and with same detail
			// and is positioned in a place that is not going to be rendered by vanilla game
			// then we can set this position as adj
			// We avoid cases where the adjPosition is in player chunk while the position is
			// not
			// to always have a wall underwater
			for (LodDirection lodDirection : LodDirection.ADJ_DIRECTIONS) {
				try {
					int xAdj = posX + lodDirection.getNormal().x;
					int zAdj = posZ + lodDirection.getNormal().z;
					int chunkXAdj = LevelPosUtil.getChunkPos(detailLevel, xAdj);
					int chunkZAdj = LevelPosUtil.getChunkPos(detailLevel, zAdj);
					if (chunkGrid.get(chunkXAdj, chunkZAdj)!=null) continue;

					boolean isCrossRegionBoundary = LevelPosUtil.getRegion(detailLevel, xAdj) != region.regionPosX ||
							LevelPosUtil.getRegion(detailLevel, zAdj) != region.regionPosZ;

					LodRegion adjRegion;
					byte adjDetail;
					int childXAdj = xAdj*2 + (lodDirection.getNormal().x<0 ? 1 : 0);
					int childZAdj = zAdj*2 + (lodDirection.getNormal().z<0 ? 1 : 0);

					//we check if the detail of the adjPos is equal to the correct one (region border fix)
					//or if the detail is wrong by 1 value (region+circle border fix)
					if (isCrossRegionBoundary) {
						//we compute at which detail that position should be rendered
						adjRegion = adjRegions[lodDirection.ordinal()-2];
						if(adjRegion == null) continue;
						adjDetail = adjRegion.getRenderDetailLevelAt(playerX, playerZ, detailLevel, xAdj, zAdj);
					} else {
						adjRegion = region;
						if (posToRender.contains(detailLevel, xAdj, zAdj)) adjDetail = detailLevel;
						else if (detailLevel>0 &&
								posToRender.contains((byte) (detailLevel-1), childXAdj, childZAdj))
							adjDetail = (byte) (detailLevel-1);
						else if (detailLevel<LodUtil.REGION_DETAIL_LEVEL &&
								posToRender.contains((byte) (detailLevel+1), xAdj/2, zAdj/2))
							adjDetail = (byte) (detailLevel+1);
						else continue;
					}

					if (adjDetail < detailLevel-1 || adjDetail > detailLevel+1) {
						continue;
					}

					if (adjDetail == detailLevel || adjDetail > detailLevel) {
						adjData[lodDirection.ordinal() - 2][0] = adjRegion.getAllData(adjDetail,
								LevelPosUtil.convert(detailLevel, xAdj, adjDetail),
								LevelPosUtil.convert(detailLevel, zAdj, adjDetail));
					} else {
						adjData[lodDirection.ordinal() - 2] = new long[2][];
						adjData[lodDirection.ordinal() - 2][0] = adjRegion.getAllData(adjDetail,
								childXAdj, childZAdj);
						adjData[lodDirection.ordinal() - 2][1] = adjRegion.getAllData(adjDetail,
								childXAdj + (lodDirection.getAxis()==LodDirection.Axis.X ? 0 : 1),
								childZAdj + (lodDirection.getAxis()==LodDirection.Axis.Z ? 0 : 1));
					}
				} catch (RuntimeException e) {
					ApiShared.LOGGER.warn("Failed to get adj data for [{}:{},{}] at [{}]", detailLevel, posX, posZ, lodDirection);
					ApiShared.LOGGER.warn("Detail exception: ", e);
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
		// Merge all quads
		quadBuilder.mergeQuads();
	}

	
	@Override
	public void close()
	{
		if (renderBufferBack != null) renderBufferBack.close();
		while (frontState.get() != FrontState.Invalidated && !frontState.compareAndSet(FrontState.Unused, FrontState.Invalidated)) {
			Thread.yield(); //FIXME: If on java 9, use Thread.onSpinWait();
		}
		if (renderBufferFront != null) renderBufferFront.close();
	}

	public void debugDumpStats(StatsMap statsMap)
	{
		statsMap.incStat("RenderRegions");
		RenderBuffer front = renderBufferFront;
		if (front!=null) {
			statsMap.incStat("FrontBuffers");
			front.debugDumpStats(statsMap);
		}
		
		RenderBuffer back = renderBufferBack;
		if (back!=null) {
			statsMap.incStat("BackBuffers");
			back.debugDumpStats(statsMap);
		}
		
		
	}
}
