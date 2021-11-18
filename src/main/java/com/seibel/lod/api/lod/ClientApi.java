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

import com.seibel.lod.core.builders.worldGeneration.LodGenWorker;
import com.seibel.lod.core.objects.lod.LodDimension;
import com.seibel.lod.core.objects.math.Mat4f;
import com.seibel.lod.core.render.GlProxy;
import com.seibel.lod.core.render.LodRenderer;
import com.seibel.lod.core.util.DetailDistanceUtil;
import com.seibel.lod.core.util.ThreadMapUtil;
import com.seibel.lod.core.wrapperAdapters.SingletonHandler;
import com.seibel.lod.core.wrapperAdapters.config.ILodConfigWrapperSingleton;
import com.seibel.lod.core.wrapperAdapters.minecraft.IMinecraftWrapper;
import com.seibel.lod.wrappers.minecraft.MinecraftWrapper;

import net.minecraft.profiler.IProfiler;

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
	
	private final IMinecraftWrapper mc = SingletonHandler.get(MinecraftWrapper.class);
	private final EventApi eventApi = EventApi.INSTANCE;
	private final ILodConfigWrapperSingleton config = SingletonHandler.get(ILodConfigWrapperSingleton.class);
	
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
		applyConfigOverrides();
		
		// clear any out of date objects
		mc.clearFrameObjectCache();
		
		try
		{
			// only run the first time setup once
			if (!firstTimeSetupComplete)
				firstFrameSetup();
			
			
			if (!mc.playerExists() || ApiShared.lodWorld.getIsWorldNotLoaded())
				return;
			
			LodDimension lodDim = ApiShared.lodWorld.getLodDimension(mc.getCurrentDimension());
			if (lodDim == null)
				return;
			
			DetailDistanceUtil.updateSettings();
			eventApi.viewDistanceChangedEvent();
			eventApi.playerMoveEvent(lodDim);
			
			lodDim.cutRegionNodesAsync(mc.getPlayerBlockPos().getX(), mc.getPlayerBlockPos().getZ());
			lodDim.expandOrLoadRegionsAsync(mc.getPlayerBlockPos().getX(), mc.getPlayerBlockPos().getZ());
			
			
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
			ApiShared.previousLodRenderDistance = config.client().graphics().quality().getLodChunkRenderDistance();
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
			
			mc.sendChatMessage("Debug settings enabled!");
			configOverrideReminderPrinted = true;
		}
		
		
		
		config.client().advanced().debugging().setDebugKeybindingsEnabled(true);
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
