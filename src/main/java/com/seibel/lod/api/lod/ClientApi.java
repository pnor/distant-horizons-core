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

package com.seibel.lod.api.lod;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.seibel.lod.api.forge.ForgeConfig;
import com.seibel.lod.builders.worldGeneration.LodGenWorker;
import com.seibel.lod.objects.lod.LodDimension;
import com.seibel.lod.objects.math.Mat4f;
import com.seibel.lod.render.GlProxy;
import com.seibel.lod.render.LodRenderer;
import com.seibel.lod.util.DetailDistanceUtil;
import com.seibel.lod.util.ThreadMapUtil;
import com.seibel.lod.wrappers.MinecraftWrapper;

import net.minecraft.profiler.IProfiler;
import net.minecraft.util.text.StringTextComponent;

/**
 * This holds the methods that should be called
 * by the host mod loader (Fabric, Forge, etc.).
 * Specifically for the client.
 * 
 * @author James Seibel
 * @version 11-12-2021
 */
public class ClientApi
{
	public static final ClientApi INSTANCE = new ClientApi();
	public static final Logger LOGGER = LogManager.getLogger("LOD");
	
	public static LodRenderer renderer = new LodRenderer(ApiShared.lodBufferBuilderFactory);
	
	
	private final MinecraftWrapper mc = MinecraftWrapper.INSTANCE;
	private final EventApi eventApi = EventApi.INSTANCE;
	
	/**
	 * there is some setup that should only happen once,
	 * once this is true that setup has completed
	 */
	private boolean firstTimeSetupComplete = false;
	private boolean configOverrideReminderPrinted = false;
	
	
	
	private ClientApi()
	{
		
	}
	
	
	
	
	public void renderLods(Mat4f mcModelViewMatrix, Mat4f mcProjectionMatrix, float partialTicks)
	{
		// comment out when creating a release
		// applyConfigOverrides();
		
		// clear any out of date objects
		mc.clearFrameObjectCache();
		
		try
		{
			// only run the first time setup once
			if (!firstTimeSetupComplete)
				firstFrameSetup();
			
			
			if (mc.getPlayer() == null || ApiShared.lodWorld.getIsWorldNotLoaded())
				return;
			
			LodDimension lodDim = ApiShared.lodWorld.getLodDimension(mc.getCurrentDimension());
			if (lodDim == null)
				return;
			
			DetailDistanceUtil.updateSettings();
			eventApi.viewDistanceChangedEvent();
			eventApi.playerMoveEvent(lodDim);
			
			lodDim.cutRegionNodesAsync((int) mc.getPlayer().getX(), (int) mc.getPlayer().getZ());
			lodDim.expandOrLoadRegionsAsync((int) mc.getPlayer().getX(), (int) mc.getPlayer().getZ());
			
			
			// Note to self:
			// if "unspecified" shows up in the pie chart, it is
			// possibly because the amount of time between sections
			// is too small for the profiler to measure
			IProfiler profiler = mc.getProfiler();
			profiler.pop(); // get out of "terrain"
			profiler.push("LOD");
			
			
			ClientApi.renderer.drawLODs(lodDim, mcModelViewMatrix, mcProjectionMatrix, partialTicks, mc.getProfiler());
			
			profiler.pop(); // end LOD
			profiler.push("terrain"); // go back into "terrain"
			
			
			// these can't be set until after the buffers are built (in renderer.drawLODs)
			// otherwise the buffers may be set to the wrong size, or not changed at all
			ApiShared.previousChunkRenderDistance = mc.getRenderDistance();
			ApiShared.previousLodRenderDistance = ForgeConfig.CLIENT.graphics.qualityOption.lodChunkRenderDistance.get();
		}
		catch (Exception e)
		{
			ClientApi.LOGGER.error("client proxy: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	/** used in a development environment to change settings on the fly */
	private void applyConfigOverrides()
	{
		// remind the developer(s) that the config override is active
		if (!configOverrideReminderPrinted)
		{
			// TODO add a send message method to the MC wrapper
//			mc.getPlayer().sendMessage(new StringTextComponent("LOD experimental build 1.5.1"), mc.getPlayer().getUUID());
//			mc.getPlayer().sendMessage(new StringTextComponent("Here be dragons!"), mc.getPlayer().getUUID());
			
			mc.getPlayer().sendMessage(new StringTextComponent("Debug settings enabled!"), mc.getPlayer().getUUID());
			configOverrideReminderPrinted = true;
		}
		
//		LodConfig.CLIENT.graphics.drawResolution.set(HorizontalResolution.BLOCK);
//		LodConfig.CLIENT.worldGenerator.generationResolution.set(HorizontalResolution.BLOCK);
		// requires a world restart?
//		LodConfig.CLIENT.worldGenerator.lodQualityMode.set(VerticalQuality.VOXEL);
		
//		LodConfig.CLIENT.graphics.fogQualityOption.fogDistance.set(FogDistance.FAR);
//		LodConfig.CLIENT.graphics.fogQualityOption.fogDrawOverride.set(FogDrawOverride.FANCY);
//		LodConfig.CLIENT.graphics.fogQualityOption.disableVanillaFog.set(true);
//		LodConfig.CLIENT.graphics.shadingMode.set(ShadingMode.DARKEN_SIDES);
		
//		LodConfig.CLIENT.graphics.advancedGraphicsOption.vanillaOverdraw.set(VanillaOverdraw.DYNAMIC);
		
//		LodConfig.CLIENT.graphics.advancedGraphicsOption.gpuUploadMethod.set(GpuUploadMethod.BUFFER_STORAGE);
		
//		LodConfig.CLIENT.worldGenerator.distanceGenerationMode.set(DistanceGenerationMode.SURFACE);
//		LodConfig.CLIENT.graphics.qualityOption.lodChunkRenderDistance.set(128);
//		LodConfig.CLIENT.worldGenerator.lodDistanceCalculatorType.set(DistanceCalculatorType.LINEAR);
//		LodConfig.CLIENT.worldGenerator.allowUnstableFeatureGeneration.set(false);
		
//		LodConfig.CLIENT.buffers.rebuildTimes.set(BufferRebuildTimes.FREQUENT);
		
		ForgeConfig.CLIENT.advancedModOptions.debugging.enableDebugKeybindings.set(true);
//		LodConfig.CLIENT.debugging.debugMode.set(DebugMode.SHOW_DETAIL);
	}
	
	
	
	
	//=================//
	// Lod maintenance //
	//=================//
	
	/** This event is called once during the first frame Minecraft renders in the world. */
	public void firstFrameSetup()
	{
		// make sure the GlProxy is created before the LodBufferBuilder needs it
		GlProxy.getInstance();
		
		firstTimeSetupComplete = true;
	}
	
	/** this method reset some static data every time we change world */
	private void resetMod()
	{
		// TODO when should this be used?
		ThreadMapUtil.clearMaps();
		LodGenWorker.restartExecutorService();
	}
	
	
	
	
	

	
	
	
}
