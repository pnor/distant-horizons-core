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

package com.seibel.lod.core.api;

import org.lwjgl.glfw.GLFW;

import com.seibel.lod.core.api.ClientApi.LagSpikeCatcher;
import com.seibel.lod.core.builders.lodBuilding.LodBuilder;
import com.seibel.lod.core.builders.worldGeneration.LodWorldGenerator;
import com.seibel.lod.core.enums.WorldType;
import com.seibel.lod.core.objects.lod.LodDimension;
import com.seibel.lod.core.objects.lod.RegionPos;
import com.seibel.lod.core.render.GLProxy;
import com.seibel.lod.core.render.LodRenderer;
import com.seibel.lod.core.util.DataPointUtil;
import com.seibel.lod.core.util.DetailDistanceUtil;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.util.SingletonHandler;
import com.seibel.lod.core.wrapperInterfaces.chunk.IChunkWrapper;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IDimensionTypeWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IWorldWrapper;

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
	public static final boolean ENABLE_STACK_DUMP_LOGGING = false;
	public static final EventApi INSTANCE = new EventApi();
	
	private static final IMinecraftWrapper MC = SingletonHandler.get(IMinecraftWrapper.class);
	private static final ILodConfigWrapperSingleton CONFIG = SingletonHandler.get(ILodConfigWrapperSingleton.class);
	
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
		if (!MC.playerExists() || ApiShared.lodWorld.getIsWorldNotLoaded())
			return;
		
		LodDimension lodDim = ApiShared.lodWorld.getLodDimension(MC.getCurrentDimension());
		if (lodDim == null)
			return;
		if (ApiShared.isShuttingDown) return;
		
		LodWorldGenerator.INSTANCE.queueGenerationRequests(lodDim, ApiShared.lodBuilder);
	}
	
	
	
	
	//==============//
	// world events //
	//==============//
	
	public void worldSaveEvent()
	{
		ApiShared.lodWorld.saveAllDimensions(false); // Do an async save.
	}
	
	private boolean isCurrentlyOnSinglePlayerServer = false;
	
	/** This is also called when a new dimension loads */
	public void worldLoadEvent(IWorldWrapper world)
	{
		if (ENABLE_STACK_DUMP_LOGGING)
			ClientApi.LOGGER.info("WorldLoadEvent called here for "+ (world.getWorldType() == WorldType.ClientWorld ?
				"clientLevel" : "serverLevel"), new RuntimeException());
		// Always ignore ServerWorld event
		if (world.getWorldType() == WorldType.ServerWorld) return;
		isCurrentlyOnSinglePlayerServer = MC.hasSinglePlayerServer();
		ApiShared.isShuttingDown = false;
		DataPointUtil.WORLD_HEIGHT = world.getHeight();
		LodBuilder.MIN_WORLD_HEIGHT = world.getMinHeight(); // This updates the World height
		
		//LodNodeGenWorker.restartExecutorService();
		//ThreadMapUtil.clearMaps();
		
		// the player just loaded a new world/dimension
		ApiShared.lodWorld.selectWorld(LodUtil.getWorldID(world));
		
		
		// make sure the correct LODs are being rendered
		// (if this isn't done the previous world's LODs may be drawn)
		ClientApi.renderer.regenerateLODsNextFrame();
	}
	
	/** This is also called when the user disconnects from a server+ */
	public void worldUnloadEvent(IWorldWrapper world)
	{
		if (ENABLE_STACK_DUMP_LOGGING)
			ClientApi.LOGGER.info("WorldUnloadEvent called here for "+ (world.getWorldType() == WorldType.ClientWorld ? "clientLevel" : "serverLevel"), new RuntimeException());
		// If it's single player, ignore the client side world unload event
		// Note: using isCurrentlyOnSinglePlayerServer as often API call unload event AFTER setting MC to not be in a singlePlayerServer
		if (isCurrentlyOnSinglePlayerServer && world.getWorldType() == WorldType.ClientWorld) return;
		
		// the player just unloaded a world/dimension
		checkIfDisconnectedFromServer();
	}
	private void checkIfDisconnectedFromServer()
	{
		{
			// the player just left the server
			
			// TODO should "resetMod()" be called here? -James
			
			// if this isn't done unfinished tasks may be left in the queue
			// preventing new LodChunks form being generated
			ApiShared.isShuttingDown = true;
			
			// TODO Check why world gen is sometimes stuck and timeout
			LodWorldGenerator.INSTANCE.restartExecutorService();
			
			LodWorldGenerator.INSTANCE.numberOfChunksWaitingToGenerate.set(0);
			ApiShared.lodWorld.deselectWorld(); // This force a save
			
			// prevent issues related to the buffer builder
			// breaking or retaining previous data when changing worlds.
			ClientApi.renderer.destroyBuffers();
			ClientApi.renderer.requestCleanup();
			GLProxy.ensureAllGLJobCompleted();
			recalculateWidths = true;
			// TODO: Check if after the refactoring, is this still needed
			ClientApi.renderer = new LodRenderer(ApiShared.lodBufferBuilderFactory);
			ClientApi.INSTANCE.rendererDisabledBecauseOfExceptions = false;
			
			// make sure the nulled objects are freed.
			// (this prevents an out of memory error when
			// changing worlds)
			System.gc();
		}
	}
	
	public void blockChangeEvent(IChunkWrapper chunk, IDimensionTypeWrapper dimType)
	{
		if (dimType != MC.getCurrentDimension()) return;
		// recreate the LOD where the blocks were changed
		LagSpikeCatcher blockChangeUpdate = new LagSpikeCatcher();
		ClientApi.INSTANCE.toBeLoaded.add(chunk.getLongChunkPos());
		blockChangeUpdate.end("clientChunkLoad");
	}
	
	
	
	
	//=============//
	// Misc Events //
	//=============//
	
	public void onKeyInput(int key, int keyAction)
	{
		if (CONFIG.client().advanced().debugging().getDebugKeybindingsEnabled())
		{
			if (key == GLFW.GLFW_KEY_F8 && keyAction == GLFW.GLFW_PRESS)
			{
				CONFIG.client().advanced().debugging().setDebugMode(CONFIG.client().advanced().debugging().getDebugMode().getNext());
				MC.sendChatMessage("F8: Set debug mode " + CONFIG.client().advanced().debugging().getDebugMode());
			}
			
			if (key == GLFW.GLFW_KEY_F6 && keyAction == GLFW.GLFW_PRESS)
			{
				CONFIG.client().advanced().debugging().setDrawLods(!CONFIG.client().advanced().debugging().getDrawLods());
				MC.sendChatMessage("F6: Set rendering " + CONFIG.client().advanced().debugging().getDrawLods());
			}
		}
	}
	
	// NOTE: This is being called from Render Thread.
	/** Re-centers the given LodDimension if it needs to be. */
	public void playerMoveEvent(LodDimension lodDim)
	{
		// make sure the dimension is centered
		RegionPos playerRegionPos = new RegionPos(MC.getPlayerBlockPos());
		RegionPos worldRegionOffset = new RegionPos(playerRegionPos.x - lodDim.getCenterRegionPosX(), playerRegionPos.z - lodDim.getCenterRegionPosZ());
		if (worldRegionOffset.x != 0 || worldRegionOffset.z != 0)
		{
			lodDim.move(worldRegionOffset);
			//LOGGER.info("offset: " + worldRegionOffset.x + "," + worldRegionOffset.z + "\t center: " + lodDim.getCenterX() + "," + lodDim.getCenterZ());
		}
	}
	
	/** Re-sizes all LodDimensions if they need to be. */
	public void viewDistanceChangedEvent()
	{
		// calculate how wide the dimension(s) should be in regions
		int chunksWide;
		if (MC.getWrappedClientWorld().getDimensionType().hasCeiling())
			chunksWide = Math.min(CONFIG.client().graphics().quality().getLodChunkRenderDistance(), LodUtil.CEILED_DIMENSION_MAX_RENDER_DISTANCE) * 2 + 1;
		else
			chunksWide = CONFIG.client().graphics().quality().getLodChunkRenderDistance() * 2 + 1;
		
		int newWidth = (int) Math.ceil(chunksWide / (float) LodUtil.REGION_WIDTH_IN_CHUNKS);
		// make sure we have an odd number of regions
		newWidth += (newWidth & 1) == 0 ? 1 : 0;
		
		// do the dimensions need to change in size?
		if (ApiShared.lodBuilder.defaultDimensionWidthInRegions != newWidth || recalculateWidths)
		{
			// update the dimensions to fit the new width
			ApiShared.lodWorld.resizeDimensionRegionWidth(newWidth);
			ApiShared.lodBuilder.defaultDimensionWidthInRegions = newWidth;
			ClientApi.renderer.setupBuffers();
			
			recalculateWidths = false;
			//LOGGER.info("new dimension width in regions: " + newWidth + "\t potential: " + newWidth );
		}
		DetailDistanceUtil.updateSettings();
	}
	
	
}
