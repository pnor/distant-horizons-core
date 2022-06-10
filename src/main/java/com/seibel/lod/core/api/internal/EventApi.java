/*
 *    This file is part of the Distant Horizons mod (formerly the LOD Mod),
 *    licensed under the GNU LGPL v3 License.
 *
 *    Copyright (C) 2020-2022  James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.seibel.lod.core.api.internal;

import com.seibel.lod.core.builders.lodBuilding.LodBuilder;
import com.seibel.lod.core.builders.worldGeneration.BatchGenerator;
import com.seibel.lod.core.enums.EWorldType;
import com.seibel.lod.core.handlers.dependencyInjection.SingletonHandler;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.objects.DHChunkPos;
import com.seibel.lod.core.objects.lod.LodDimension;
import com.seibel.lod.core.objects.DHRegionPos;
import com.seibel.lod.core.render.GLProxy;
import com.seibel.lod.core.render.LodRenderer;
import com.seibel.lod.core.util.DetailDistanceUtil;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.wrapperInterfaces.IVersionConstants;
import com.seibel.lod.core.wrapperInterfaces.chunk.IChunkWrapper;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IDimensionTypeWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IWorldWrapper;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandles;

/**
 * This holds the methods that should be called by the host mod loader (Fabric,
 * Forge, etc.). Specifically server and client events.
 *
 * @author James Seibel
 * @version 2021-11-12
 */
@Deprecated
public class EventApi
{
	public static final boolean ENABLE_STACK_DUMP_LOGGING = false;
	public static final EventApi INSTANCE = new EventApi();
	
	private static final Logger LOGGER = DhLoggerBuilder.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());
	private static final IMinecraftClientWrapper MC = SingletonHandler.get(IMinecraftClientWrapper.class);
	private static final ILodConfigWrapperSingleton CONFIG = SingletonHandler.get(ILodConfigWrapperSingleton.class);
	private static final IVersionConstants VERSION_CONSTANTS = SingletonHandler.get(IVersionConstants.class);
	
	/**
	 * can be set if we want to recalculate variables related to the LOD view
	 * distance
	 */
	private boolean recalculateWidths = false;
	
	private boolean isCurrentlyOnSinglePlayerServer = false;
	
	
	private EventApi()
	{
	
	}
	
	// =============//
	// tick events //
	// =============//
	public BatchGenerator batchGenerator = null;

	private int lastWorldGenTickDelta = 0;
	public void serverTickEvent()
	{
		lastWorldGenTickDelta--;
		if (!MC.playerExists() || InternalApiShared.lodWorld.getIsWorldNotLoaded())
			return;
		
		LodDimension lodDim = InternalApiShared.lodWorld.getLodDimension(MC.getCurrentDimension());
		if (lodDim == null)
			return;
		if (InternalApiShared.isShuttingDown)
			return;

		if (CONFIG.client().worldGenerator().getEnableDistantGeneration())
		{
			if (lastWorldGenTickDelta <= 0) {
				lastWorldGenTickDelta = 20; // 20 ticks is 1 second. We don't need to refresh world gen status every tick.
				try {
					if (batchGenerator == null)
						batchGenerator = new BatchGenerator(InternalApiShared.lodBuilder, lodDim);
					batchGenerator.queueGenerationRequests(lodDim, InternalApiShared.lodBuilder);
				} catch (Exception e) {
					// Exception may happen if world got unloaded unorderly
					e.printStackTrace();
				}
			}
		}
		else
		{
			if (batchGenerator != null)
			{
				batchGenerator.stop(false);
				batchGenerator = null;
			}
		}
	}
	
	// ==============//
	// world events //
	// ==============//
	
	public void worldSaveEvent()
	{
		InternalApiShared.lodWorld.saveAllDimensions(false); // Do an async save.
	}
	
	/** This is also called when a new dimension loads */
	public void worldLoadEvent(IWorldWrapper world)
	{
		if (ENABLE_STACK_DUMP_LOGGING)
			LOGGER.info(
					"WorldLoadEvent called here for "
							+ (world.getWorldType() == EWorldType.ClientWorld ? "clientLevel" : "serverLevel"),
					new RuntimeException());
		// Always ignore ServerWorld event
		if (world.getWorldType() == EWorldType.ServerWorld)
			return;
		isCurrentlyOnSinglePlayerServer = MC.hasSinglePlayerServer();
		if (!InternalApiShared.isShuttingDown) LOGGER.warn("WorldLoadEvent called on {} while another world is loaded!",
				(world.getWorldType() == EWorldType.ClientWorld ? "clientLevel" : "serverLevel"));
		InternalApiShared.isShuttingDown = false;
		//DataPointUtil.WORLD_HEIGHT = world.getHeight();
		LodBuilder.MIN_WORLD_HEIGHT = world.getMinHeight(); // This updates the World height
		
		// LodNodeGenWorker.restartExecutorService();
		// ThreadMapUtil.clearMaps();
		
		// the player just loaded a new world/dimension
		String worldID = LodUtil.getWorldID(world);
		LOGGER.info("Loading new world/dimension: {}",worldID);
		InternalApiShared.lodWorld.selectWorld(worldID);
		LOGGER.info("World/dimension loaded: {}",worldID);
		
		// make sure the correct LODs are being rendered
		// (if this isn't done the previous world's LODs may be drawn)
		ClientApi.renderer.regenerateLODsNextFrame();
		InternalApiShared.previousVertQual = CONFIG.client().graphics().quality().getVerticalQuality();
	}
	
	/** This is also called when the user disconnects from a server+ */
	public void worldUnloadEvent(IWorldWrapper world)
	{
		if (ENABLE_STACK_DUMP_LOGGING)
			LOGGER.info(
					"WorldUnloadEvent called here for "
							+ (world.getWorldType() == EWorldType.ClientWorld ? "clientLevel" : "serverLevel"),
					new RuntimeException());
		
		// If it's single player, ignore the client side world unload event
		// Note: using isCurrentlyOnSinglePlayerServer as often API call unload event
		// AFTER setting MC to not be in a singlePlayerServer
		if (isCurrentlyOnSinglePlayerServer && world.getWorldType() == EWorldType.ClientWorld)
			return;

		// if this isn't done unfinished tasks may be left in the queue
		// preventing new LodChunks form being generated
		if (InternalApiShared.isShuttingDown) return; // Don't do this if we're already shutting down
		InternalApiShared.isShuttingDown = true;
		
		// TODO Better report on when world gen is stuck and timeout
		if (batchGenerator != null)
			batchGenerator.stop(true);
		batchGenerator = null;
		
		InternalApiShared.lodWorld.deselectWorld(); // This force a save and shutdown lodDim properly
		
		// prevent issues related to the buffer builder
		// breaking or retaining previous data when changing worlds.
		ClientApi.renderer.destroyBuffers();
		ClientApi.renderer.requestCleanup();
		GLProxy.ensureAllGLJobCompleted();
		recalculateWidths = true;
		InternalApiShared.previousVertQual = null;
		
		// TODO: Check if after the refactoring, is this still needed
		ClientApi.renderer = new LodRenderer(ClientApi.lodBufferBuilderFactory);
		ClientApi.INSTANCE.rendererDisabledBecauseOfExceptions = false;
		LOGGER.info("Distant Horizon unloaded");
	}
	
	public void blockChangeEvent(IChunkWrapper chunk, IDimensionTypeWrapper dimType)
	{
		if (dimType != MC.getCurrentDimension())
			return;
		// recreate the LOD where the blocks were changed
		ClientApi.LagSpikeCatcher blockChangeUpdate = new ClientApi.LagSpikeCatcher();
		ClientApi.INSTANCE.toBeLoaded.add(new DHChunkPos(chunk.getLongChunkPos()));
		blockChangeUpdate.end("clientChunkLoad");
	}
	
	// =============//
	// Misc Events //
	// =============//
	
	// NOTE: This is being called from Render Thread.
	
	/** Re-centers the given LodDimension if it needs to be. */
	public void playerMoveEvent(LodDimension lodDim)
	{
		// make sure the dimension is centered
		DHRegionPos playerRegionPos = new DHRegionPos(MC.getPlayerBlockPos());
		DHRegionPos center = lodDim.getCenterRegionPos();
		DHRegionPos worldRegionOffset = new DHRegionPos(playerRegionPos.x - center.x, playerRegionPos.z - center.z);
		if (worldRegionOffset.x != 0 || worldRegionOffset.z != 0)
		{
			lodDim.move(worldRegionOffset);
			// LOGGER.info("offset: " + worldRegionOffset.x + "," + worldRegionOffset.z +
			// "\t center: " + lodDim.getCenterX() + "," + lodDim.getCenterZ());
		}
	}
	
	/** Re-sizes all LodDimensions if they need to be. */
	public void viewDistanceChangedEvent()
	{
		// calculate how wide the dimension(s) should be in regions
		int chunksWide;
		if (MC.getWrappedClientWorld().getDimensionType().hasCeiling())
			chunksWide = Math.min(CONFIG.client().graphics().quality().getLodChunkRenderDistance(),
					LodUtil.CEILED_DIMENSION_MAX_RENDER_DISTANCE) * 2 + 1;
		else
			chunksWide = CONFIG.client().graphics().quality().getLodChunkRenderDistance() * 2 + 1;
		
		int newWidth = (int) Math.ceil(chunksWide / (float) LodUtil.REGION_WIDTH_IN_CHUNKS);
		// make sure we have an odd number of regions
		newWidth += (newWidth & 1) == 0 ? 1 : 0;
		
		// do the dimensions need to change in size?
		if (InternalApiShared.lodBuilder.defaultDimensionWidthInRegions != newWidth || recalculateWidths)
		{
			// update the dimensions to fit the new width
			InternalApiShared.lodWorld.resizeDimensionRegionWidth(newWidth);
			InternalApiShared.lodBuilder.defaultDimensionWidthInRegions = newWidth;
			ClientApi.renderer.setupBuffers();
			
			recalculateWidths = false;
			// LOGGER.info("new dimension width in regions: " + newWidth + "\t potential: "
			// + newWidth );
		}
		DetailDistanceUtil.updateSettings();
	}
	
}
