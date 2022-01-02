/*
 *    This file is part of the Distant Horizon mod (formerly the LOD Mod),
 *    licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2021  James Seibel
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

package com.seibel.lod.core.render;

import java.awt.Color;
import java.time.Duration;
import java.util.Set;

import org.lwjgl.opengl.GL32;

import com.seibel.lod.core.api.ApiShared;
import com.seibel.lod.core.api.ClientApi;
import com.seibel.lod.core.builders.bufferBuilding.LodBufferBuilderFactory;
import com.seibel.lod.core.enums.rendering.DebugMode;
import com.seibel.lod.core.enums.rendering.FogColorMode;
import com.seibel.lod.core.enums.rendering.FogDistance;
import com.seibel.lod.core.handlers.IReflectionHandler;
import com.seibel.lod.core.objects.lod.LodDimension;
import com.seibel.lod.core.objects.math.Mat4f;
import com.seibel.lod.core.objects.math.Vec3d;
import com.seibel.lod.core.objects.math.Vec3f;
import com.seibel.lod.core.objects.opengl.LodVertexBuffer;
import com.seibel.lod.core.render.objects.LightmapTexture;
import com.seibel.lod.core.util.DetailDistanceUtil;
import com.seibel.lod.core.util.GridList;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.util.MovableGridList;
import com.seibel.lod.core.util.SingletonHandler;
import com.seibel.lod.core.wrapperInterfaces.block.AbstractBlockPosWrapper;
import com.seibel.lod.core.wrapperInterfaces.chunk.AbstractChunkPosWrapper;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftRenderWrapper;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftWrapper;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IProfilerWrapper;

/**
 * This is where all the magic happens. <br>
 * This is where LODs are draw to the world.
 * 
 * @author James Seibel
 * @version 12-12-2021
 */
public class LodRenderer
{
	public static class VanillaRenderedChunksList extends GridList<Boolean> {
		private static final long serialVersionUID = -5448501880911391315L;
		
		public final int centerX;
		public final int centerZ;
		
		public VanillaRenderedChunksList(int range, int centerX, int centerZ) {
			super(range);
			this.centerX = centerX;
			this.centerZ = centerZ;
			for (int i=0; i<gridSize*gridSize; i++) {
				add(i, false);
			}
		}
	}
	public static class LagSpikeCatcher {

		long timer = System.nanoTime();
		public LagSpikeCatcher() {}
		public void end(String source) {
			timer = System.nanoTime() - timer;
			if (timer> 16000000) { //16 ms
				ClientApi.LOGGER.info("NOTE: "+source+" took "+Duration.ofNanos(timer)+"!");
			}
			
		}
	}
	
	private static final IMinecraftWrapper MC = SingletonHandler.get(IMinecraftWrapper.class);
	private static final IMinecraftRenderWrapper MC_RENDER = SingletonHandler.get(IMinecraftRenderWrapper.class);
	private static final ILodConfigWrapperSingleton CONFIG = SingletonHandler.get(ILodConfigWrapperSingleton.class);
	private static final IReflectionHandler REFLECTION_HANDLER = SingletonHandler.get(IReflectionHandler.class);

	public static final int VANILLA_REFRESH_TIMEOUT = 60;
	/**
	 * If true the LODs colors will be replaced with
	 * a checkerboard, this can be used for debugging.
	 */
	public DebugMode previousDebugMode = DebugMode.OFF;
	
	// This tells us if the renderer is enabled or not. If in a world, it should be enabled.
	private boolean isSetupComplete = false;
	
	/** This is used to generate the buildable buffers */
	private final LodBufferBuilderFactory lodBufferBuilderFactory;
	
	// The shader program
	LodRenderProgram shaderProgram = null;
	
	/** This is used to determine if the LODs should be regenerated */
	private AbstractBlockPosWrapper previousPos = null;
	
	// these variables are used to determine if the buffers should be rebuilt
	private int prevRenderDistance = 0;
	private long prevPlayerPosTime = 0;
	private long prevVanillaChunkTime = 0;
	private long prevChunkTime = 0;
	
	
	/** This is used to determine if the LODs should be regenerated */
	private FogDistance prevFogDistance = FogDistance.NEAR_AND_FAR;
	
	/**
	 * if this is true the LOD buffers should be regenerated,
	 * provided they aren't already being regenerated.
	 */
	private volatile boolean partialRegen = false;
	private volatile boolean fullRegen = true;
	private volatile boolean markToCleanup = false;
	
	/**
	 * This HashSet contains every chunk that Vanilla Minecraft
	 * is going to render
	 */
	public VanillaRenderedChunksList vanillaRenderedChunks;
	public int vanillaRenderedChunksCenterX;
	public int vanillaRenderedChunksCenterZ;
	public int vanillaRenderedChunksRefreshTimer;
	
	private boolean canVanillaFogBeDisabled = true;

	public void requestCleanup() {markToCleanup = true;}
	
	public LodRenderer(LodBufferBuilderFactory newLodNodeBufferBuilder)
	{
		lodBufferBuilderFactory = newLodNodeBufferBuilder;
	}
	
	/**
	 * Besides drawing the LODs this method also starts
	 * the async process of generating the Buffers that hold those LODs.
	 * @param lodDim The dimension to draw, if null doesn't replace the current dimension.
	 * @param mcModelViewMatrix This matrix stack should come straight from MC's renderChunkLayer (or future equivalent) method
	 * @param mcProjectionMatrix 
	 * @param partialTicks how far into the current tick this method was called.
	 */
	public void drawLODs(LodDimension lodDim, Mat4f mcModelViewMatrix, Mat4f mcProjectionMatrix, float partialTicks, IProfilerWrapper profiler)
	{
		//=================================//
		// determine if LODs should render //
		//=================================//
		
		if (lodDim == null)
		{
			// if there aren't any loaded LodChunks
			// don't try drawing anything
			return;
		}
		
		if (MC_RENDER.playerHasBlindnessEffect())
		{
			// if the player is blind, don't render LODs,
			// and don't change minecraft's fog
			// which blindness relies on.
			return;
		}

		// get MC's shader program
		// Save all MC render state
		int currentProgram = GL32.glGetInteger(GL32.GL_CURRENT_PROGRAM);
		int currentVBO = GL32.glGetInteger(GL32.GL_ARRAY_BUFFER_BINDING);
		int currentVAO = GL32.glGetInteger(GL32.GL_VERTEX_ARRAY_BINDING);
		int currentActiveText = GL32.glGetInteger(GL32.GL_ACTIVE_TEXTURE);
		boolean currentBlend = GL32.glGetBoolean(GL32.GL_BLEND);
		
		GLProxy glProxy = GLProxy.getInstance();
		if (canVanillaFogBeDisabled && CONFIG.client().graphics().fogQuality().getDisableVanillaFog())
			if (!glProxy.disableLegacyFog())
				if (!MC_RENDER.tryDisableVanillaFog())
					canVanillaFogBeDisabled = false;
		
		// TODO move the buffer regeneration logic into its own class (probably called in the client api instead)
		// starting here...
		LagSpikeCatcher updateStatue = new LagSpikeCatcher();
		updateRegenStatus(lodDim, partialTicks);
		updateStatue.end("LodDrawSetup:UpdateStatus");
		

		// FIXME: Currently, we check for last Lod Dimension so that we can trigger a cleanup() if dimension has changed
		// The better thing to do is to call cleanup() on leaving dimensions in the EventApi, but only for client-side.
		if (markToCleanup) {
			markToCleanup = false;
			cleanup(); // This will unset the isSetupComplete, causing a setup() call.
		}
		
		//=================//
		// create the LODs //
		//=================//
		
		// only regenerate the LODs if:
		// 1. we want to regenerate LODs
		// 2. we aren't already regenerating the LODs
		// 3. we aren't waiting for the build and draw buffers to swap
		//		(this is to prevent thread conflicts)
		if (lodBufferBuilderFactory.updateAndSwapLodBuffersAsync(this, lodDim, MC.getPlayerBlockPos().getX(),
				MC.getPlayerBlockPos().getY(), MC.getPlayerBlockPos().getZ(), partialRegen, fullRegen)) {
			// the regen process has been started,
			// it will be done when lodBufferBuilder.newBuffersAvailable() is true
			fullRegen = false;
			partialRegen = false;
		}
		
		// Get the front buffers to draw
		MovableGridList<LodVertexBuffer[]> vbos = lodBufferBuilderFactory.getFrontBuffers();
		int vbosCenterX = lodBufferBuilderFactory.getFrontBuffersCenterX();
		int vbosCenterZ = lodBufferBuilderFactory.getFrontBuffersCenterZ();
		
		// @Unused
		if (vbos == null) {
			// There is still no vbos, which means nothing needs to be drawn. So no rendering needed
			// (Vbos should be setup by now)
			return;
		}
		
		//===================//
		// draw params setup //
		//===================//
		
		profiler.push("LOD draw setup");
		LagSpikeCatcher drawSetup = new LagSpikeCatcher();

		/*---------Set GL State--------*/
		// Make sure to unbind current VBO so we don't mess up vanilla settings
		GL32.glBindBuffer(GL32.GL_ARRAY_BUFFER, 0);
		
		// set the required open GL settings
		if (CONFIG.client().advanced().debugging().getDebugMode() == DebugMode.SHOW_DETAIL_WIREFRAME)
			GL32.glPolygonMode(GL32.GL_FRONT_AND_BACK, GL32.GL_LINE);
		else
			GL32.glPolygonMode(GL32.GL_FRONT_AND_BACK, GL32.GL_FILL);

		GL32.glEnable(GL32.GL_CULL_FACE);
		GL32.glEnable(GL32.GL_DEPTH_TEST);
		
		// enable transparent rendering
		// GL32.glBlendFunc(GL32.GL_SRC_ALPHA, GL32.GL_ONE_MINUS_SRC_ALPHA);
		// GL32.glEnable(GL32.GL_BLEND);
		
		/*---------Bind required objects--------*/
		// Setup LodRenderProgram and the LightmapTexture if it has not yet been done
		// also binds LightmapTexture, VAO, and ShaderProgram
		if (!isSetupComplete) {
			setup();
		} else {
			shaderProgram.bind();
		}
		GL32.glActiveTexture(GL32.GL_TEXTURE0);
		LightmapTexture lightmapTexture = new LightmapTexture();
		
		/*---------Get required data--------*/
		// Get the matrixs for rendering
		Mat4f modelViewMatrix = translateModelViewMatrix(mcModelViewMatrix, partialTicks, vbosCenterX, vbosCenterZ);
		int vanillaBlockRenderedDistance = MC_RENDER.getRenderDistance() * LodUtil.CHUNK_WIDTH;
		int farPlaneBlockDistance;
		// required for setupFog and setupProjectionMatrix
		if (MC.getWrappedClientWorld().getDimensionType().hasCeiling())
			farPlaneBlockDistance = Math.min(CONFIG.client().graphics().quality().getLodChunkRenderDistance(), LodUtil.CEILED_DIMENSION_MAX_RENDER_DISTANCE) * LodUtil.CHUNK_WIDTH;
		else
			farPlaneBlockDistance = CONFIG.client().graphics().quality().getLodChunkRenderDistance() * LodUtil.CHUNK_WIDTH;
		Mat4f projectionMatrix = createProjectionMatrix(mcProjectionMatrix, vanillaBlockRenderedDistance, farPlaneBlockDistance);
		LodFogConfig fogSettings = new LodFogConfig(CONFIG, REFLECTION_HANDLER, farPlaneBlockDistance, vanillaBlockRenderedDistance);

		/*---------Fill uniform data--------*/
		// Fill the uniform data. Note: GL33.GL_TEXTURE0 == texture bindpoint 0
		shaderProgram.fillUniformData(modelViewMatrix, projectionMatrix, getTranslatedCameraPos(vbosCenterX, vbosCenterZ),
				MC_RENDER.isFogStateInUnderWater() ? getUnderWaterFogColor(partialTicks) : getFogColor(partialTicks), (int) (MC.getSkyDarken(partialTicks) * 15), 0);
		// Previous guy said fog setting may be different from region to region, but the fogSettings never changed... soooooo...
		shaderProgram.fillUniformDataForFog(fogSettings, MC_RENDER.isFogStateInUnderWater());
		// Note: Since lightmapTexture is changing every frame, it's faster to recreate it than to reuse the old one.
		lightmapTexture.fillData(MC_RENDER.getLightmapTextureWidth(), MC_RENDER.getLightmapTextureHeight(), MC_RENDER.getLightmapPixels());

		//===========//
		// rendering //
		//===========//
		drawSetup.end("LodDrawSetup");
		profiler.popPush("LOD draw");
		LagSpikeCatcher draw = new LagSpikeCatcher();
		
		boolean cullingDisabled = CONFIG.client().graphics().advancedGraphics().getDisableDirectionalCulling();
		
		// where the center of the buffers is (needed when culling regions)
		// render each of the buffers
		int lowRegionX = vbos.getCenterX() - vbos.gridCentreToEdge;
		int lowRegionZ = vbos.getCenterY() - vbos.gridCentreToEdge;
		int drawCall = 0;
		for (int regionX=lowRegionX; regionX<vbos.gridSize; regionX++) {
			for (int regionZ=lowRegionZ; regionZ<vbos.gridSize; regionZ++) {
				if (vbos.get(regionX, regionZ) == null) continue;
				if (cullingDisabled || RenderUtil.isRegionInViewFrustum(MC_RENDER.getCameraBlockPosition(),
						MC_RENDER.getLookAtVector(), regionX, regionZ)) {
					for (LodVertexBuffer vbo : vbos.get(regionX, regionZ)) {
						if (vbo == null) continue;
						if (vbo.vertexCount == 0) continue;
						GL32.glBindBuffer(GL32.GL_ARRAY_BUFFER, vbo.id);
						shaderProgram.bindVertexBuffer(vbo.id);
						drawCall++;
						GL32.glDrawArrays(GL32.GL_TRIANGLES, 0, vbo.vertexCount);
					}
					
				}
			}
			
		}
		//if (drawCall!=0)
		//	ClientApi.LOGGER.info("DrawCall Count: "+drawCall);
		
		//================//
		// render cleanup //
		//================//
		draw.end("LodDraw");
		profiler.popPush("LOD cleanup");
		LagSpikeCatcher drawCleanup = new LagSpikeCatcher();
		
		GL32.glBindBuffer(GL32.GL_ARRAY_BUFFER, 0);

		shaderProgram.unbind();
		lightmapTexture.free();

		GL32.glPolygonMode(GL32.GL_FRONT_AND_BACK, GL32.GL_FILL);
		if (currentBlend)
			GL32.glEnable(GL32.GL_BLEND);
		else 
			GL32.glDisable(GL32.GL_BLEND);

		// if this cleanup isn't done MC will crash
		// when trying to render its own terrain
		// And may causes mod compat issue
		GL32.glUseProgram(currentProgram);
		GL32.glBindBuffer(GL32.GL_ARRAY_BUFFER, currentVBO);
		GL32.glBindVertexArray(currentVAO);
		GL32.glActiveTexture(currentActiveText);
		
		// clear the depth buffer so everything is drawn over the LODs
		GL32.glClear(GL32.GL_DEPTH_BUFFER_BIT);
		drawCleanup.end("LodDrawCleanup");
		// end of internal LOD profiling
		profiler.pop();
	}
	
	//=================//
	// Setup Functions //
	//=================//
	
	/** Setup all render objects - REQUIRES to be in render thread */
	private void setup() {
		if (isSetupComplete) {
			ClientApi.LOGGER.warn("Renderer setup called but it has already completed setup!");
			return;
		}
		if (!GLProxy.hasInstance()) {
			ClientApi.LOGGER.warn("Renderer setup called but GLProxy has not yet been setup!");
			return;
		}
		
		isSetupComplete = true;
		shaderProgram = new LodRenderProgram();
	}
	
	/** Create all buffers that will be used. */
	public void setupBuffers(LodDimension lodDim)
	{
		lodBufferBuilderFactory.allBuffersRequireReset = true;
	}

	private Color getFogColor(float partialTicks)
	{
		Color fogColor;
		
		if (CONFIG.client().graphics().fogQuality().getFogColorMode() == FogColorMode.USE_SKY_COLOR)
			fogColor = MC_RENDER.getSkyColor();
		else
			fogColor = MC_RENDER.getFogColor(partialTicks);
		
		return fogColor;
	}
	private Color getUnderWaterFogColor(float partialTicks)
	{
		return MC_RENDER.getUnderWaterFogColor(partialTicks);
	}
	
	/**
	 * Translate the camera relative to the LodDimension's center,
	 * this is done since all LOD buffers are created in world space
	 * instead of object space.
	 * (since AxisAlignedBoundingBoxes (LODs) use doubles and thus have a higher
	 * accuracy vs the model view matrix, which only uses floats)
	 */
	private Mat4f translateModelViewMatrix(Mat4f mcModelViewMatrix, float partialTicks, int vbosCenterX, int vbosCenterZ)
	{
		// get all relevant camera info
		Vec3d projectedView = MC_RENDER.getCameraExactPosition();
		
		// translate the camera relative to the regions' center
		// (AxisAlignedBoundingBoxes (LODs) use doubles and thus have a higher
		// accuracy vs the model view matrix, which only uses floats)
		//int bufferPosX = LevelPosUtil.convert(LodUtil.CHUNK_DETAIL_LEVEL, vbosCenterX, LodUtil.BLOCK_DETAIL_LEVEL);
		//int bufferPosZ = LevelPosUtil.convert(LodUtil.CHUNK_DETAIL_LEVEL, vbosCenterZ, LodUtil.BLOCK_DETAIL_LEVEL);
		int bufferPosX = vbosCenterX;
		int bufferPosZ = vbosCenterZ;
		double xDiff = projectedView.x - bufferPosX;
		double zDiff = projectedView.z - bufferPosZ;
		mcModelViewMatrix.multiplyTranslationMatrix(-xDiff, -projectedView.y, -zDiff);
		
		return mcModelViewMatrix;
	}
	
	/** 
	 * Similar to translateModelViewMatrix (above),
	 * but for the camera position
	 */
	private Vec3f getTranslatedCameraPos(int vbosCenterX, int vbosCenterZ)
	{
		//int worldCenterX = LevelPosUtil.convert(LodUtil.CHUNK_DETAIL_LEVEL, vbosCenterX, LodUtil.BLOCK_DETAIL_LEVEL);
		//int worldCenterZ = LevelPosUtil.convert(LodUtil.CHUNK_DETAIL_LEVEL, vbosCenterZ, LodUtil.BLOCK_DETAIL_LEVEL);
		int worldCenterX = vbosCenterX;
		int worldCenterZ = vbosCenterZ;
		Vec3d cameraPos = MC_RENDER.getCameraExactPosition();
		return new Vec3f((float)cameraPos.x - worldCenterX, (float)cameraPos.y, (float)cameraPos.z - worldCenterZ);
	}

	/**
	 * create and return a new projection matrix based on MC's projection matrix
	 * @param currentProjectionMatrix this is Minecraft's current projection matrix
	 * @param vanillaBlockRenderedDistance Minecraft's vanilla far plane distance
	 */
	private static Mat4f createProjectionMatrix(Mat4f currentProjectionMatrix, float vanillaBlockRenderedDistance, int farPlaneBlockDistance)
	{
		//Create a copy of the current matrix, so the current matrix isn't modified.
		Mat4f lodProj = currentProjectionMatrix.copy();

		//Set new far and near clip plane values.
		lodProj.setClipPlanes(
				CONFIG.client().graphics().advancedGraphics().getUseExtendedNearClipPlane() ? vanillaBlockRenderedDistance / 5 : 1,
				farPlaneBlockDistance * LodUtil.CHUNK_WIDTH / 2);

		return lodProj;
	}
	
	//======================//
	// Cleanup Functions    //
	//======================//

	/** cleanup and free all render objects. REQUIRES to be in render thread
	 *  (Many objects are Native, outside of JVM, and need manual cleanup)  */ 
	private void cleanup() {
		if (!isSetupComplete) {
			ClientApi.LOGGER.warn("Renderer cleanup called but Renderer has not completed setup!");
			return;
		}
		if (!GLProxy.hasInstance()) {
			ClientApi.LOGGER.warn("Renderer Cleanup called but the GLProxy has never been inited!");
			return;
		}
		isSetupComplete = false;
		ClientApi.LOGGER.info("Renderer Cleanup Started");
		shaderProgram.free();
		ClientApi.LOGGER.info("Renderer Cleanup Complete");
	}

	/** Calls the BufferBuilder's destroyBuffers method. */
	public void destroyBuffers()
	{
		lodBufferBuilderFactory.destroyBuffers();
	}
	
	//======================//
	// Other Misc Functions //
	//======================//
	
	/**
	 * If this is called then the next time "drawLODs" is called
	 * the LODs will be regenerated; the same as if the player moved.
	 */
	public void regenerateLODsNextFrame()
	{
		fullRegen = true;
	}
	
	// returns whether anything changed
	private boolean updateVanillaRenderedChunks(LodDimension lodDim, boolean recreateChunks) {
		short chunkRenderDistance = (short) MC_RENDER.getRenderDistance();
		int chunkX = Math.floorDiv(previousPos.getX(), 16);
		int chunkZ = Math.floorDiv(previousPos.getZ(), 16);
		// if the player is high enough, draw all LODs
		if (previousPos.getY() > 256) {
			vanillaRenderedChunks = new VanillaRenderedChunksList(
					chunkRenderDistance, chunkX, chunkZ);
			return true;
		}
		VanillaRenderedChunksList chunkList;
		
		if (recreateChunks) {
			vanillaRenderedChunks = new VanillaRenderedChunksList(chunkRenderDistance, chunkX, chunkZ);
			return true;
		} else {
			chunkList = vanillaRenderedChunks;
			chunkX = chunkList.centerX;
			chunkZ = chunkList.centerZ;
			chunkRenderDistance = (short) vanillaRenderedChunks.gridCentreToEdge;
		}

		boolean anyChanged = false;
		LagSpikeCatcher getChunks = new LagSpikeCatcher();
		Set<AbstractChunkPosWrapper> chunkPosToSkip = LodUtil.getNearbyLodChunkPosToSkip(lodDim, previousPos);
		getChunks.end("LodDrawSetup:UpdateStatus:UpdateVanillaChunks:getChunks");
		for (AbstractChunkPosWrapper pos : chunkPosToSkip)
		{
			int xIndex = (pos.getX() - chunkX) + (chunkRenderDistance + 1);
			int zIndex = (pos.getZ() - chunkZ) + (chunkRenderDistance + 1);
			
			// sometimes we are given chunks that are outside the render distance,
			// This prevents index out of bounds exceptions
			if (xIndex >= 0 && zIndex >= 0
						&& xIndex < vanillaRenderedChunks.gridSize
						&& zIndex < vanillaRenderedChunks.gridSize)
			{
				if (!chunkList.get(chunkList.calculateOffset(xIndex, zIndex)))
				{
					chunkList.set(chunkList.calculateOffset(xIndex, zIndex), true);
					anyChanged = true;
					lodDim.markRegionBufferToRegen(pos.getRegionX(), pos.getRegionZ());
				}
			}
		}
		vanillaRenderedChunks = chunkList;
		return anyChanged;
	}
	
	private void updateRegenStatus(LodDimension lodDim, float partialTicks) {
		short chunkRenderDistance = (short) MC_RENDER.getRenderDistance();
		long newTime = System.currentTimeMillis();
		AbstractBlockPosWrapper newPos = MC.getPlayerBlockPos();
		boolean shouldUpdateChunks = false;
		boolean posUpdated = false;
		boolean tryPartialGen = false;
		boolean tryFullGen = false;

		// check if the view distance or config changed
		if (ApiShared.previousLodRenderDistance != CONFIG.client().graphics().quality().getLodChunkRenderDistance()
					|| chunkRenderDistance != prevRenderDistance
					|| prevFogDistance != CONFIG.client().graphics().fogQuality().getFogDistance())
		{
			DetailDistanceUtil.updateSettings(); // FIXME: This should NOT be here!
			prevFogDistance = CONFIG.client().graphics().fogQuality().getFogDistance();
			prevRenderDistance = chunkRenderDistance;
			tryFullGen = true;
		} else if (CONFIG.client().advanced().debugging().getDebugMode() != previousDebugMode)
		{ // did the user change the debug setting?
			previousDebugMode = CONFIG.client().advanced().debugging().getDebugMode();
			tryFullGen = true;
		}
		
		// check if the player has moved
		if (newTime - prevPlayerPosTime > CONFIG.client().advanced().buffers().getRebuildTimes().playerMoveTimeout) { 
			if (previousPos == null
				|| Math.abs(newPos.getX() - previousPos.getX()) > CONFIG.client().advanced().buffers().getRebuildTimes().playerMoveDistance*16
				|| Math.abs(newPos.getZ() - previousPos.getZ()) > CONFIG.client().advanced().buffers().getRebuildTimes().playerMoveDistance*16)
			{
				tryPartialGen = true;
				previousPos = newPos;
				posUpdated = true;
			}
			prevPlayerPosTime = newTime;
		}

		// check if the vanilla rendered chunks changed
		if (newTime - prevVanillaChunkTime > CONFIG.client().advanced().buffers().getRebuildTimes().renderedChunkTimeout)
		{
			shouldUpdateChunks = true;
			prevVanillaChunkTime = newTime;
		}

		// check if there is any newly generated terrain to show
		if (newTime - prevChunkTime > CONFIG.client().advanced().buffers().getRebuildTimes().chunkChangeTimeout)
		{
			tryPartialGen = true;
			prevChunkTime = newTime;
		}
		
		
		if (tryFullGen && !posUpdated) {
			previousPos = newPos;
			posUpdated = true;
		}
		shouldUpdateChunks |= posUpdated;
		if (shouldUpdateChunks) {
			tryPartialGen |= updateVanillaRenderedChunks(lodDim, posUpdated);
		}
		
		if (tryFullGen) {
			fullRegen = true;
			lodDim.regenDimensionBuffers = false;
		} else if (tryPartialGen) {
			if (lodDim.regenDimensionBuffers)
			{
				partialRegen = true;
				lodDim.regenDimensionBuffers = false;
			}
		}
	}

}
