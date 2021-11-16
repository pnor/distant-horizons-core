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

import org.lwjgl.glfw.GLFW;

import com.seibel.lod.api.forge.ForgeConfig;
import com.seibel.lod.core.builders.worldGeneration.LodWorldGenerator;
import com.seibel.lod.core.enums.config.DistanceGenerationMode;
import com.seibel.lod.core.objects.lod.LodDimension;
import com.seibel.lod.core.objects.lod.RegionPos;
import com.seibel.lod.core.render.LodRenderer;
import com.seibel.lod.core.util.DataPointUtil;
import com.seibel.lod.core.util.DetailDistanceUtil;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.util.ThreadMapUtil;
import com.seibel.lod.core.wrapperAdapters.world.IDimensionTypeWrapper;
import com.seibel.lod.core.wrapperAdapters.world.IWorldWrapper;
import com.seibel.lod.wrappers.MinecraftWrapper;
import com.seibel.lod.wrappers.chunk.ChunkWrapper;

/**
 * This holds the methods that should be called
 * by the host mod loader (Fabric, Forge, etc.).
 * Specifically server and client events.
 * 
 * @author James Seibel
 * @version 11-12-2021
 */
public class EventApi
{
	public static final EventApi INSTANCE = new EventApi();
	
	private final MinecraftWrapper mc = MinecraftWrapper.INSTANCE;
	
	/**
	 * can be set if we want to recalculate variables related
	 * to the LOD view distance
	 */
	private boolean recalculateWidths = false;
	
	
	private EventApi()
	{
		
	}
	
	
	
	
	//=============//
	// tick events //
	//=============//
	
	public void serverTickEvent()
	{
		if (mc.getPlayer() == null || ApiShared.lodWorld.getIsWorldNotLoaded())
			return;
		
		LodDimension lodDim = ApiShared.lodWorld.getLodDimension(mc.getCurrentDimension());
		if (lodDim == null)
			return;
		
		LodWorldGenerator.INSTANCE.queueGenerationRequests(lodDim, ClientApi.renderer, ApiShared.lodBuilder);
	}
	
	
	
	
	//==============//
	// world events //
	//==============//
	
	public void chunkLoadEvent(ChunkWrapper chunk, IDimensionTypeWrapper dimType)
	{
		ApiShared.lodBuilder.generateLodNodeAsync(chunk, ApiShared.lodWorld, dimType, DistanceGenerationMode.SERVER);
	}
	
	public void worldSaveEvent()
	{
		ApiShared.lodWorld.saveAllDimensions();
	}
	
	/** This is also called when a new dimension loads */
	public void worldLoadEvent(IWorldWrapper world)
	{
		DataPointUtil.worldHeight = world.getHeight();
		//LodNodeGenWorker.restartExecutorService();
		//ThreadMapUtil.clearMaps();
		
		// the player just loaded a new world/dimension
		ApiShared.lodWorld.selectWorld(LodUtil.getWorldID(world));
		
		// make sure the correct LODs are being rendered
		// (if this isn't done the previous world's LODs may be drawn)
		ClientApi.renderer.regenerateLODsNextFrame();
	}
	
	public void worldUnloadEvent()
	{
		// the player just unloaded a world/dimension
		ThreadMapUtil.clearMaps();
		
		
		if (mc.getConnection().getLevel() == null)
		{
			// the player just left the server
			
			// TODO should "resetMod()" be called here? -James
			
			// if this isn't done unfinished tasks may be left in the queue
			// preventing new LodChunks form being generated
			//LodNodeGenWorker.restartExecutorService(); // TODO why was this commented out? -James
			//ThreadMapUtil.clearMaps();
			
			LodWorldGenerator.INSTANCE.numberOfChunksWaitingToGenerate.set(0);
			ApiShared.lodWorld.deselectWorld();
			
			
			// prevent issues related to the buffer builder
			// breaking when changing worlds.
			ClientApi.renderer.destroyBuffers();
			recalculateWidths = true;
			ClientApi.renderer = new LodRenderer(ApiShared.lodBufferBuilderFactory);
			
			
			// make sure the nulled objects are freed.
			// (this prevents an out of memory error when
			// changing worlds)
			System.gc();
		}
	}
	
	public void blockChangeEvent(ChunkWrapper chunk, IDimensionTypeWrapper dimType)
	{
		// recreate the LOD where the blocks were changed
		ApiShared.lodBuilder.generateLodNodeAsync(chunk, ApiShared.lodWorld, dimType);		
	}
	
	
	
	
	//=============//
	// Misc Events //
	//=============//
	
	public void onKeyInput(int key, int keyAction)
	{
		if (ForgeConfig.CLIENT.advancedModOptions.debugging.enableDebugKeybindings.get()
				&& key == GLFW.GLFW_KEY_F4 && keyAction == GLFW.GLFW_PRESS)
		{
			ForgeConfig.CLIENT.advancedModOptions.debugging.debugMode.set(ForgeConfig.CLIENT.advancedModOptions.debugging.debugMode.get().getNext());
		}
		
		if (ForgeConfig.CLIENT.advancedModOptions.debugging.enableDebugKeybindings.get()
				&& key == GLFW.GLFW_KEY_F6 && keyAction == GLFW.GLFW_PRESS)
		{
			ForgeConfig.CLIENT.advancedModOptions.debugging.drawLods.set(!ForgeConfig.CLIENT.advancedModOptions.debugging.drawLods.get());
		}
	}
	
	/** Re-centers the given LodDimension if it needs to be. */
	public void playerMoveEvent(LodDimension lodDim)
	{
		// make sure the dimension is centered
		RegionPos playerRegionPos = new RegionPos(mc.getPlayerBlockPos());
		RegionPos worldRegionOffset = new RegionPos(playerRegionPos.x - lodDim.getCenterRegionPosX(), playerRegionPos.z - lodDim.getCenterRegionPosZ());
		if (worldRegionOffset.x != 0 || worldRegionOffset.z != 0)
		{
			ApiShared.lodWorld.saveAllDimensions();
			lodDim.move(worldRegionOffset);
			//LOGGER.info("offset: " + worldRegionOffset.x + "," + worldRegionOffset.z + "\t center: " + lodDim.getCenterX() + "," + lodDim.getCenterZ());
		}
	}
	
	/** Re-sizes all LodDimensions if they need to be. */
	public void viewDistanceChangedEvent()
	{
		// calculate how wide the dimension(s) should be in regions
		int chunksWide;
		if (mc.getClientWorld().dimensionType().hasCeiling())
			chunksWide = Math.min(ForgeConfig.CLIENT.graphics.qualityOption.lodChunkRenderDistance.get(), LodUtil.CEILED_DIMENSION_MAX_RENDER_DISTANCE) * 2 + 1;
		else
			chunksWide = ForgeConfig.CLIENT.graphics.qualityOption.lodChunkRenderDistance.get() * 2 + 1;
		
		int newWidth = (int) Math.ceil(chunksWide / (float) LodUtil.REGION_WIDTH_IN_CHUNKS);
		// make sure we have an odd number of regions
		newWidth += (newWidth & 1) == 0 ? 1 : 2;
		
		// do the dimensions need to change in size?
		if (ApiShared.lodBuilder.defaultDimensionWidthInRegions != newWidth || recalculateWidths)
		{
			ApiShared.lodWorld.saveAllDimensions();
			
			// update the dimensions to fit the new width
			ApiShared.lodWorld.resizeDimensionRegionWidth(newWidth);
			ApiShared.lodBuilder.defaultDimensionWidthInRegions = newWidth;
			ClientApi.renderer.setupBuffers(ApiShared.lodWorld.getLodDimension(mc.getCurrentDimension()));
			
			recalculateWidths = false;
			//LOGGER.info("new dimension width in regions: " + newWidth + "\t potential: " + newWidth );
		}
		DetailDistanceUtil.updateSettings();
	}
	
	
}
