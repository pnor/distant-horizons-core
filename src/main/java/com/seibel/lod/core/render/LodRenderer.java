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
import java.util.HashSet;

import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import com.seibel.lod.core.api.ApiShared;
import com.seibel.lod.core.api.ClientApi;
import com.seibel.lod.core.builders.bufferBuilding.LodBufferBuilderFactory;
import com.seibel.lod.core.builders.bufferBuilding.LodBufferBuilderFactory.VertexBuffersAndOffset;
import com.seibel.lod.core.enums.config.GpuUploadMethod;
import com.seibel.lod.core.enums.rendering.DebugMode;
import com.seibel.lod.core.enums.rendering.FogColorMode;
import com.seibel.lod.core.enums.rendering.FogDistance;
import com.seibel.lod.core.enums.rendering.FogDrawMode;
import com.seibel.lod.core.handlers.IReflectionHandler;
import com.seibel.lod.core.objects.lod.LodDimension;
import com.seibel.lod.core.objects.math.Mat4f;
import com.seibel.lod.core.objects.math.Vec3d;
import com.seibel.lod.core.objects.math.Vec3f;
import com.seibel.lod.core.objects.opengl.LodVertexBuffer;
import com.seibel.lod.core.render.objects.LightmapTexture;
import com.seibel.lod.core.util.DetailDistanceUtil;
import com.seibel.lod.core.util.LevelPosUtil;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.util.SingletonHandler;
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
	private static final IMinecraftWrapper MC = SingletonHandler.get(IMinecraftWrapper.class);
	private static final IMinecraftRenderWrapper MC_RENDER = SingletonHandler.get(IMinecraftRenderWrapper.class);
	private static final ILodConfigWrapperSingleton CONFIG = SingletonHandler.get(ILodConfigWrapperSingleton.class);
	private static final IReflectionHandler REFLECTION_HANDLER = SingletonHandler.get(IReflectionHandler.class);
	
	/**
	 * If true the LODs colors will be replaced with
	 * a checkerboard, this can be used for debugging.
	 */
	public DebugMode previousDebugMode = DebugMode.OFF;
	
	// This tells us if the renderer is enabled or not. If in a world, it should be enabled.
	private boolean isSetupComplete = false;
	private volatile boolean markToCleanup = false;
	
	/** This is used to generate the buildable buffers */
	private final LodBufferBuilderFactory lodBufferBuilderFactory;
	
	/** Each VertexBuffer represents 1 region */
	private LodVertexBuffer[][][] vbos;
	/**
	 * the OpenGL IDs for the vbos of the same indices.
	 * These have to be separate because we can't override the
	 * buffers in the VBOs (and we don't want to)
	 */
	private int[][][] storageBufferIds = null;
	
	// The shader program
	LodRenderProgram shaderProgram = null;
	LightmapTexture lightmapTexture = null;
	
	private int vbosCenterX = 0;
	private int vbosCenterZ = 0;
	
	/** This is used to determine if the LODs should be regenerated */
	private int[] previousPos = new int[] { 0, 0, 0 };
	
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
	
	/**
	 * This HashSet contains every chunk that Vanilla Minecraft
	 * is going to render
	 */
	public boolean[][] vanillaRenderedChunks;
	public boolean vanillaRenderedChunksChanged;
	public boolean vanillaRenderedChunksEmptySkip = false;

	
	public LodRenderer(LodBufferBuilderFactory newLodNodeBufferBuilder)
	{
		lodBufferBuilderFactory = newLodNodeBufferBuilder;
	}
	
	public void markForCleanup() {
		markToCleanup = true;
	}
	
	
	private LodDimension lastLodDimension = null;
	
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

		GLProxy glProxy = GLProxy.getInstance();
		if (CONFIG.client().graphics().fogQuality().getDisableVanillaFog())
			glProxy.disableLegacyFog();

		
		// TODO move the buffer regeneration logic into its own class (probably called in the client api instead)
		// starting here...
		determineIfLodsShouldRegenerate(lodDim, partialTicks);

		
		//=================//
		// create the LODs //
		//=================//
		
		// only regenerate the LODs if:
		// 1. we want to regenerate LODs
		// 2. we aren't already regenerating the LODs
		// 3. we aren't waiting for the build and draw buffers to swap
		//		(this is to prevent thread conflicts)
		if ((partialRegen || fullRegen) && !lodBufferBuilderFactory.generatingBuffers && !lodBufferBuilderFactory.newBuffersAvailable())
		{
			// generate the LODs on a separate thread to prevent stuttering or freezing
			lodBufferBuilderFactory.generateLodBuffersAsync(this, lodDim, MC.getPlayerBlockPos().getX(), MC.getPlayerBlockPos().getY(), MC.getPlayerBlockPos().getZ(), fullRegen);
			
			// the regen process has been started,
			// it will be done when lodBufferBuilder.newBuffersAvailable()
			// is true
			fullRegen = false;
			partialRegen = false;
		}
		
		// TODO move the buffer regeneration logic into its own class (probably called in the client api instead)
		// ...ending here
		
		if (lodBufferBuilderFactory.newBuffersAvailable())
		{
			swapBuffers();
		}
		
		if (vbos == null) {
			// There is still no vbos, which means nothing needs to be drawn. So no rendering needed
			// (Vbos should be setup by now)
			return;
		}

		// FIXME: Currently, we check for last Lod Dimension so that we can trigger a cleanup() if dimension has changed
		// The better thing to do is to call cleanup() on leaving dimensions in the EventApi, but only for client-side.
		if (markToCleanup || (lastLodDimension != null && lodDim != lastLodDimension)) {
			markToCleanup = false;
			cleanup(); // This will unset the isSetupComplete, causing a setup() call.
			lastLodDimension = lodDim;
			//fullRegen = true;
		}
		
		//===================//
		// draw params setup //
		//===================//
		
		profiler.push("LOD draw setup");
		
		// Get or setup the gl proxy and the needed objects
		if (!GLProxy.hasInstance() && isSetupComplete)
			ClientApi.LOGGER.warn("GLProxy has not yet been inited yet renderer state is enabled!");

		// Setup LodRenderProgram and the LightmapTexture if it has not yet been done
		if (!isSetupComplete) setup();
		
		
		// set the required open GL settings
		
		if (CONFIG.client().advanced().debugging().getDebugMode() == DebugMode.SHOW_DETAIL_WIREFRAME)
			GL15.glPolygonMode(GL15.GL_FRONT_AND_BACK, GL15.GL_LINE);
		else
			GL15.glPolygonMode(GL15.GL_FRONT_AND_BACK, GL15.GL_FILL);
		
		GL15.glEnable(GL15.GL_CULL_FACE);
		GL15.glEnable(GL15.GL_DEPTH_TEST);
		
		// enable transparent rendering
		GL15.glBlendFunc(GL15.GL_SRC_ALPHA, GL15.GL_ONE_MINUS_SRC_ALPHA);
		GL15.glEnable(GL15.GL_BLEND);
		
		// get MC's shader program
		int currentProgram = GL20.glGetInteger(GL20.GL_CURRENT_PROGRAM);
		
		// Get the matrixs for rendering
		Mat4f modelViewMatrix = translateModelViewMatrix(mcModelViewMatrix, partialTicks);
		int vanillaBlockRenderedDistance = MC_RENDER.getRenderDistance() * LodUtil.CHUNK_WIDTH;
		int farPlaneBlockDistance;
		// required for setupFog and setupProjectionMatrix
		if (MC.getWrappedClientWorld().getDimensionType().hasCeiling())
			farPlaneBlockDistance = Math.min(CONFIG.client().graphics().quality().getLodChunkRenderDistance(), LodUtil.CEILED_DIMENSION_MAX_RENDER_DISTANCE) * LodUtil.CHUNK_WIDTH;
		else
			farPlaneBlockDistance = CONFIG.client().graphics().quality().getLodChunkRenderDistance() * LodUtil.CHUNK_WIDTH;
		
		Mat4f projectionMatrix = createProjectionMatrix(mcProjectionMatrix, vanillaBlockRenderedDistance, farPlaneBlockDistance);
		LodFogConfig fogSettings = new LodFogConfig(CONFIG, REFLECTION_HANDLER, farPlaneBlockDistance, vanillaBlockRenderedDistance);

		//==============//
		// shader setup //
		//==============//
		
		// Bind and update the lightmap data
		GL20.glActiveTexture(GL20.GL_TEXTURE0);
		
		shaderProgram.bind();
		// Fill the uniform data. Note: GL_TEXTURE_2D == texture bindpoint 0
		shaderProgram.fillUniformData(modelViewMatrix, projectionMatrix, getTranslatedCameraPos(),
				getFogColor(), (int) (MC.getSkyDarken(partialTicks) * 15), 0);

		lightmapTexture = new LightmapTexture();
		lightmapTexture.bind();
		lightmapTexture.fillData(MC_RENDER.getLightmapTextureWidth(), MC_RENDER.getLightmapTextureHeight(), MC_RENDER.getLightmapPixels());

		// Previous guy said fog setting may be different from region to region, but the fogSettings never changed... soooooo...
		shaderProgram.fillUniformDataForFog(fogSettings);

		//===========//
		// rendering //
		//===========//
		
		profiler.popPush("LOD draw");
		
		boolean cullingDisabled = CONFIG.client().graphics().advancedGraphics().getDisableDirectionalCulling();
		boolean renderBufferStorage = CONFIG.client().advanced().buffers().getGpuUploadMethod() == GpuUploadMethod.BUFFER_STORAGE && glProxy.bufferStorageSupported;
		
		// where the center of the buffers is (needed when culling regions)
		// render each of the buffers
		for (int x = 0; x < vbos.length; x++)
		{
			for (int z = 0; z < vbos.length; z++)
			{
				//int tempX = LodUtil.convertLevelPos(x + vbosCenterX - (lodDim.getWidth() / 2), LodUtil.REGION_DETAIL_LEVEL , LodUtil.BLOCK_DETAIL_LEVEL);
				//int tempY = LodUtil.convertLevelPos(z + vbosCenterZ - (lodDim.getWidth() / 2), LodUtil.REGION_DETAIL_LEVEL, LodUtil.BLOCK_DETAIL_LEVEL);
				if (cullingDisabled || RenderUtil.isRegionInViewFrustum(MC_RENDER.getCameraBlockPosition(),
						MC_RENDER.getLookAtVector(),
						vbosCenterX + LodUtil.convertLevelPos(x - (lodDim.getWidth() / 2), LodUtil.REGION_DETAIL_LEVEL , LodUtil.BLOCK_DETAIL_LEVEL),
						vbosCenterZ + LodUtil.convertLevelPos(z - (lodDim.getWidth() / 2), LodUtil.REGION_DETAIL_LEVEL , LodUtil.BLOCK_DETAIL_LEVEL)))
				{
					
					// actual rendering
					int bufferId = 0;
					for (int i = 0; i < vbos[x][z].length; i++)
					{
						bufferId = (storageBufferIds != null && renderBufferStorage) ? storageBufferIds[x][z][i] : vbos[x][z][i].id;
						if (bufferId==0) continue;
						GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, bufferId);
						shaderProgram.bind();
						shaderProgram.bindVertexBuffer(bufferId);
						GL30.glDrawArrays(GL30.GL_TRIANGLES, 0, vbos[x][z][i].vertexCount);
						shaderProgram.unbindVertexBuffer();
						
					}
					
				}
			}
		}
		
		//================//
		// render cleanup //
		//================//
		
		// if this cleanup isn't done MC will crash
		// when trying to render its own terrain
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		
		lightmapTexture.unbind();
		shaderProgram.unbind();
		lightmapTexture.free();
		
		profiler.popPush("LOD cleanup");
		
		GL15.glPolygonMode(GL15.GL_FRONT_AND_BACK, GL15.GL_FILL);
		GL15.glDisable(GL15.GL_BLEND); // TODO: what should this be reset to?
		
		GL20.glUseProgram(currentProgram);
		
		// clear the depth buffer so everything is drawn over the LODs
		GL15.glClear(GL15.GL_DEPTH_BUFFER_BIT);
		
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
		//lightmapTexture = new LightmapTexture();
	}
	
	/** Create all buffers that will be used. */
	public void setupBuffers(LodDimension lodDim)
	{
		lodBufferBuilderFactory.setupBuffers(lodDim);
	}

	private Color getFogColor()
	{
		Color fogColor;
		
		if (CONFIG.client().graphics().fogQuality().getFogColorMode() == FogColorMode.USE_SKY_COLOR)
			fogColor = MC_RENDER.getSkyColor();
		else
			fogColor = MC_RENDER.getFogColor();
		
		return fogColor;
	}
	
	/**
	 * Translate the camera relative to the LodDimension's center,
	 * this is done since all LOD buffers are created in world space
	 * instead of object space.
	 * (since AxisAlignedBoundingBoxes (LODs) use doubles and thus have a higher
	 * accuracy vs the model view matrix, which only uses floats)
	 */
	private Mat4f translateModelViewMatrix(Mat4f mcModelViewMatrix, float partialTicks)
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
	private Vec3f getTranslatedCameraPos()
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
		//GLProxy.getInstance().setGlContext(GLProxyContext.LOD_BUILDER);
		
		shaderProgram.free();
		//lightmapTexture.free();
		//GLProxy.getInstance().setGlContext(GLProxyContext.NONE);
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
	
	/**
	 * Replace the current Vertex Buffers with the newly
	 * created buffers from the lodBufferBuilder. <br><br>
	 * <p>
	 * For some reason this has to be called after the frame has been rendered,
	 * otherwise visual stuttering/rubber banding may happen. I'm not sure why...
	 */
	private void swapBuffers()
	{
		// replace the drawable buffers with
		// the newly created buffers from the lodBufferBuilder
		VertexBuffersAndOffset result = lodBufferBuilderFactory.getVertexBuffers();
		vbos = result.vbos;
		storageBufferIds = result.storageBufferIds;
		vbosCenterX = result.drawableCenterBlockPosX;
		vbosCenterZ = result.drawableCenterBlockPosZ;
	}
	
	/** Determines if the LODs should have a fullRegen or partialRegen */
	private void determineIfLodsShouldRegenerate(LodDimension lodDim, float partialTicks)
	{
		short chunkRenderDistance = (short) MC_RENDER.getRenderDistance();
		int vanillaRenderedChunksWidth = chunkRenderDistance * 2 + 2;
		
		//=============//
		// full regens //
		//=============//
		
		// check if the view distance changed
		if (ApiShared.previousLodRenderDistance != CONFIG.client().graphics().quality().getLodChunkRenderDistance()
					|| chunkRenderDistance != prevRenderDistance
					|| prevFogDistance != CONFIG.client().graphics().fogQuality().getFogDistance())
		{
			
			vanillaRenderedChunks = new boolean[vanillaRenderedChunksWidth][vanillaRenderedChunksWidth];
			DetailDistanceUtil.updateSettings();
			fullRegen = true;
			previousPos = LevelPosUtil.createLevelPos((byte) 4, MC.getPlayerChunkPos().getZ(), MC.getPlayerChunkPos().getZ());
			prevFogDistance = CONFIG.client().graphics().fogQuality().getFogDistance();
			prevRenderDistance = chunkRenderDistance;
		}
		
		// did the user change the debug setting?
		if (CONFIG.client().advanced().debugging().getDebugMode() != previousDebugMode)
		{
			previousDebugMode = CONFIG.client().advanced().debugging().getDebugMode();
			fullRegen = true;
		}
		
		
		long newTime = System.currentTimeMillis();
		
		// check if the player has moved
		if (newTime - prevPlayerPosTime > CONFIG.client().advanced().buffers().getRebuildTimes().playerMoveTimeout)
		{
			if (LevelPosUtil.getDetailLevel(previousPos) == 0
						|| Math.abs(MC.getPlayerChunkPos().getX() - LevelPosUtil.getPosX(previousPos)) > CONFIG.client().advanced().buffers().getRebuildTimes().playerMoveDistance
						|| Math.abs(MC.getPlayerChunkPos().getZ() - LevelPosUtil.getPosZ(previousPos)) > CONFIG.client().advanced().buffers().getRebuildTimes().playerMoveDistance)
			{
				vanillaRenderedChunks = new boolean[vanillaRenderedChunksWidth][vanillaRenderedChunksWidth];
				fullRegen = true;
				previousPos = LevelPosUtil.createLevelPos((byte) 4, MC.getPlayerChunkPos().getX(), MC.getPlayerChunkPos().getZ());
			}
			prevPlayerPosTime = newTime;
		}
		
		
		/*
		// determine how far the lighting has to 
		// change in order to rebuild the buffers
		
		// the max brightness is 1 and the minimum is 0.2
		float skyBrightness = lodDim.dimension.hasSkyLight() ? MC.getSkyDarken(partialTicks) : 0.2f;
		float minLightingDifference;
		switch (CONFIG.client().advanced().buffers().getRebuildTimes())
		{
		case FREQUENT:
			minLightingDifference = 0.025f;
			break;
		case NORMAL:
			minLightingDifference = 0.05f;
			break;
		default:
		case RARE:
			minLightingDifference = 0.1f;
			break;
		}
		
		// check if the lighting changed
		if (Math.abs(skyBrightness - prevSkyBrightness) > minLightingDifference
					// make sure the lighting gets to the max/minimum value
					// (just in case the minLightingDifference is too large to notice the change)
					|| (skyBrightness == 1.0f && prevSkyBrightness != 1.0f) // noon
					|| (skyBrightness == 0.2f && prevSkyBrightness != 0.2f) // midnight
					|| MC_RENDER.getGamma() != prevBrightness)
		{
			fullRegen = true;
			prevBrightness = MC_RENDER.getGamma();
			prevSkyBrightness = skyBrightness;
		}*/
		
		//================//
		// partial regens //
		//================//
		
		
		// check if the vanilla rendered chunks changed
		if (newTime - prevVanillaChunkTime > CONFIG.client().advanced().buffers().getRebuildTimes().renderedChunkTimeout)
		{
			if (vanillaRenderedChunksChanged)
			{
				partialRegen = true;
				vanillaRenderedChunksChanged = false;
			}
			prevVanillaChunkTime = newTime;
		}
		
		
		// check if there is any newly generated terrain to show
		if (newTime - prevChunkTime > CONFIG.client().advanced().buffers().getRebuildTimes().chunkChangeTimeout)
		{
			if (lodDim.regenDimensionBuffers)
			{
				partialRegen = true;
				lodDim.regenDimensionBuffers = false;
			}
			prevChunkTime = newTime;
		}
		
		
		
		//==============//
		// LOD skipping //
		//==============//
		
		// determine which LODs should not be rendered close to the player
		HashSet<AbstractChunkPosWrapper> chunkPosToSkip = LodUtil.getNearbyLodChunkPosToSkip(lodDim, MC.getPlayerBlockPos());
		int xIndex;
		int zIndex;
		for (AbstractChunkPosWrapper pos : chunkPosToSkip)
		{
			vanillaRenderedChunksEmptySkip = false;
			
			xIndex = (pos.getX() - MC.getPlayerChunkPos().getX()) + (chunkRenderDistance + 1);
			zIndex = (pos.getZ() - MC.getPlayerChunkPos().getZ()) + (chunkRenderDistance + 1);
			
			// sometimes we are given chunks that are outside the render distance,
			// This prevents index out of bounds exceptions
			if (xIndex >= 0 && zIndex >= 0
						&& xIndex < vanillaRenderedChunks.length
						&& zIndex < vanillaRenderedChunks.length)
			{
				if (!vanillaRenderedChunks[xIndex][zIndex])
				{
					vanillaRenderedChunks[xIndex][zIndex] = true;
					vanillaRenderedChunksChanged = true;
					lodDim.markRegionBufferToRegen(pos.getRegionX(), pos.getRegionZ());
				}
			}
		}
		
		
		// if the player is high enough, draw all LODs
		if (chunkPosToSkip.isEmpty() && MC.getPlayerBlockPos().getY() > 256 && !vanillaRenderedChunksEmptySkip)
		{
			vanillaRenderedChunks = new boolean[vanillaRenderedChunksWidth][vanillaRenderedChunksWidth];
			vanillaRenderedChunksChanged = true;
			vanillaRenderedChunksEmptySkip = true;
		}
		
		vanillaRenderedChunks = new boolean[vanillaRenderedChunksWidth][vanillaRenderedChunksWidth];
	}
	
}
