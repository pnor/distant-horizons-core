/*
 *    This file is part of the Distant Horizons mod (formerly the LOD Mod),
 *    licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2020-2022  James Seibel
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

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.seibel.lod.core.builders.lodBuilding.LodBuilder;
import com.seibel.lod.core.enums.rendering.RendererType;
import com.seibel.lod.core.logging.ConfigBasedLogger;
import com.seibel.lod.core.logging.ConfigBasedSpamLogger;
import com.seibel.lod.core.render.RenderSystemTest;
import com.seibel.lod.core.wrapperInterfaces.chunk.AbstractChunkPosWrapper;
import org.apache.logging.log4j.Level;
import com.seibel.lod.core.handlers.LodDimensionFinder;
import org.lwjgl.glfw.GLFW;

import com.seibel.lod.core.ModInfo;
import com.seibel.lod.core.builders.lodBuilding.bufferBuilding.LodBufferBuilderFactory;
import com.seibel.lod.core.enums.config.DistanceGenerationMode;
import com.seibel.lod.core.handlers.dependencyInjection.SingletonHandler;
import com.seibel.lod.core.objects.lod.LodDimension;
import com.seibel.lod.core.objects.math.Mat4f;
import com.seibel.lod.core.render.GLProxy;
import com.seibel.lod.core.render.LodRenderer;
import com.seibel.lod.core.util.DetailDistanceUtil;
import com.seibel.lod.core.logging.SpamReducedLogger;
import com.seibel.lod.core.wrapperInterfaces.IWrapperFactory;
import com.seibel.lod.core.wrapperInterfaces.chunk.IChunkWrapper;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftRenderWrapper;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IProfilerWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IWorldWrapper;

/**
 * This holds the methods that should be called by the host mod loader (Fabric,
 * Forge, etc.). Specifically for the client.
 *
 * @author James Seibel
 * @version 2022-3-26
 */
public class ClientApi {
	public static boolean prefLoggerEnabled = false;

	public static final ClientApi INSTANCE = new ClientApi();

	public static final LodBufferBuilderFactory lodBufferBuilderFactory = new LodBufferBuilderFactory();
	public static LodRenderer renderer = new LodRenderer(lodBufferBuilderFactory);
	public static RenderSystemTest testRenderer = new RenderSystemTest();

	private static final IMinecraftClientWrapper MC = SingletonHandler.get(IMinecraftClientWrapper.class);
	private static final IMinecraftRenderWrapper MC_RENDER = SingletonHandler.get(IMinecraftRenderWrapper.class);
	private static final ILodConfigWrapperSingleton CONFIG = SingletonHandler.get(ILodConfigWrapperSingleton.class);
	private static final IWrapperFactory FACTORY = SingletonHandler.get(IWrapperFactory.class);
	private static final EventApi EVENT_API = EventApi.INSTANCE;

	public static final boolean ENABLE_LAG_SPIKE_LOGGING = false;
	public static final long LAG_SPIKE_THRESOLD_NS = TimeUnit.NANOSECONDS.convert(16, TimeUnit.MILLISECONDS);

	public static final long SPAM_LOGGER_FLUSH_NS = TimeUnit.NANOSECONDS.convert(1, TimeUnit.SECONDS);

	public static LodDimensionFinder DIMENSION_FINDER = new LodDimensionFinder();;

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

	/**
	 * there is some setup that should only happen once, once this is true that
	 * setup has completed
	 */
	private boolean firstTimeSetupComplete = false;
	private boolean configOverrideReminderPrinted = false;

	public boolean rendererDisabledBecauseOfExceptions = false;

	private ClientApi() {
	}

	public static void logToChat(Level logLevel, String str) {
		String prefix = "[" + ModInfo.READABLE_NAME + "] ";
		if (logLevel == Level.ERROR) {
			prefix += "\u00A74";
		} else if (logLevel == Level.WARN) {
			prefix += "\u00A76";
		} else if (logLevel == Level.INFO) {
			prefix += "\u00A7f";
		} else if (logLevel == Level.DEBUG) {
			prefix += "\u00A77";
		} else if (logLevel == Level.TRACE) {
			prefix += "\u00A78";
		} else {
			prefix += "\u00A7f";
		}
		prefix += "\u00A7l\u00A7u";
		prefix += logLevel.name();
		prefix += ":\u00A7r ";
		if (MC != null)
			MC.sendChatMessage(prefix + str);
	}

	private final ConcurrentHashMap.KeySetView<Long, Boolean> generating = ConcurrentHashMap.newKeySet();
	public final ConcurrentHashMap.KeySetView<Long, Boolean> toBeLoaded = ConcurrentHashMap.newKeySet();

	public void clientChunkLoadEvent(IChunkWrapper chunk, IWorldWrapper world) {
		LagSpikeCatcher clientChunkLoad = new LagSpikeCatcher();
		// ApiShared.LOGGER.info("Lod Generating add: "+chunk.getLongChunkPos());
		toBeLoaded.add(chunk.getLongChunkPos());
		clientChunkLoad.end("clientChunkLoad");
	}

	private long lastFlush = 0;

	public void renderLods(Mat4f mcModelViewMatrix, Mat4f mcProjectionMatrix, float partialTicks) {
		// comment out when creating a release
		// applyConfigOverrides();

		// clear any out of date objects
		MC.clearFrameObjectCache();

		try {
			boolean doFlush = System.nanoTime() - lastFlush >= SPAM_LOGGER_FLUSH_NS;
			if (doFlush) {
				lastFlush = System.nanoTime();
				SpamReducedLogger.flushAll();
			}
			ConfigBasedLogger.updateAll();
			ConfigBasedSpamLogger.updateAll(doFlush);

			if (ApiShared.previousVertQual != CONFIG.client().graphics().quality().getVerticalQuality()) {
				ApiShared.previousVertQual = CONFIG.client().graphics().quality().getVerticalQuality();
				EventApi.INSTANCE.worldUnloadEvent(MC.getWrappedServerWorld());
				EventApi.INSTANCE.worldLoadEvent(MC.getWrappedClientWorld());
				return;
			}

			// only run the first time setup once
			if (!firstTimeSetupComplete)
				firstFrameSetup();

			if (!MC.playerExists() || ApiShared.lodWorld.getIsWorldNotLoaded())
				return;

			IWorldWrapper world = MC.getWrappedClientWorld();
			if (world == null)
				return;
			LodDimension lodDim = ApiShared.lodWorld.getLodDimension(world.getDimensionType());
			// lodDim = null;

			// Make sure the player's data is up-to-date
			DIMENSION_FINDER.updatePlayerData();

			// Make the LodDim if it does not exist
			if (lodDim == null) {
				if (DIMENSION_FINDER.isDone()) {
					lodDim = DIMENSION_FINDER.getAndClearFoundLodDimension();
					ApiShared.lodWorld.addLodDimension(lodDim);
				} else {
					DIMENSION_FINDER.AttemptToDetermineSubDimensionAsync(MC.getCurrentDimension());
					return;
				}
			}

			if (prefLoggerEnabled) {
				lodDim.dumpRamUsage();
				lodBufferBuilderFactory.dumpBufferMemoryUsage();
			}

			LagSpikeCatcher updateToBeLoadedChunk = new LagSpikeCatcher();
			for (long pos : toBeLoaded) {
				if (generating.size() >= 1) {
					// ApiShared.LOGGER.info("Lod Generating Full! Remining: "+toBeLoaded.size());
					break;
				}
				IChunkWrapper chunk = world.tryGetChunk(FACTORY.createChunkPos(pos));
				if (chunk == null) {
					toBeLoaded.remove(pos);
					LodBuilder.EVENT_LOGGER.debug("Manual Chunk: {} not ready. Remaining queue: {}",
							FACTORY.createChunkPos(pos), toBeLoaded.size());
					continue;
				}
				if (!chunk.isLightCorrect())
					continue;
				if (!chunk.doesNearbyChunksExist())
					continue;
				toBeLoaded.remove(pos);
				generating.add(pos);
				// ApiShared.LOGGER.info("Lod Generation trying "+pos+". Remining: "
				// +toBeLoaded.size());
				ApiShared.lodBuilder.generateLodNodeAsync(chunk, ApiShared.lodWorld, world.getDimensionType(),
						DistanceGenerationMode.FULL, true, true, () -> {
							generating.remove(pos);
							LodBuilder.EVENT_LOGGER.debug("Manual Chunk: {} done. Remaining queue: {}",
									FACTORY.createChunkPos(pos), toBeLoaded.size());
						}, () -> {
							generating.remove(pos);
							toBeLoaded.add(pos);
							LodBuilder.EVENT_LOGGER.debug("Manual Chunk: {} not ready. Remaining queue: {}",
									FACTORY.createChunkPos(pos), toBeLoaded.size());
						});
			}
			updateToBeLoadedChunk.end("updateToBeLoadedChunk");

			LagSpikeCatcher updateSettings = new LagSpikeCatcher();
			DetailDistanceUtil.updateSettings();
			EVENT_API.viewDistanceChangedEvent();
			updateSettings.end("updateSettings");
			LagSpikeCatcher updatePlayerMove = new LagSpikeCatcher();
			EVENT_API.playerMoveEvent(lodDim);
			updatePlayerMove.end("updatePlayerMove");

			LagSpikeCatcher cutAndExpendAsync = new LagSpikeCatcher();
			lodDim.cutRegionNodesAsync(MC.getPlayerBlockPos().getX(), MC.getPlayerBlockPos().getZ());
			lodDim.expandOrLoadRegionsAsync(MC.getPlayerBlockPos().getX(), MC.getPlayerBlockPos().getZ());
			cutAndExpendAsync.end("cutAndExpendAsync");

			if (CONFIG.client().advanced().debugging().getRendererType() == RendererType.DEFAULT) {
				// Note to self:
				// if "unspecified" shows up in the pie chart, it is
				// possibly because the amount of time between sections
				// is too small for the profiler to measure
				IProfilerWrapper profiler = MC.getProfiler();
				profiler.pop(); // get out of "terrain"
				profiler.push("LOD");

				if (!rendererDisabledBecauseOfExceptions) {
					try {
						if (lodDim.dimension.getDimensionName().equals("overworld")) {
							ClientApi.renderer.drawLODs(lodDim, mcModelViewMatrix, mcProjectionMatrix, partialTicks,
									MC.getProfiler());
						}
					} catch (RuntimeException e) {
						rendererDisabledBecauseOfExceptions = true;
						ApiShared.LOGGER.error("Renderer thrown an uncaught exception: ", e);
						try {
							MC.sendChatMessage("\u00A74\u00A7l\u00A7uERROR: Distant Horizons"
									+ " renderer has encountered an exception!");
							MC.sendChatMessage("\u00A74Renderer is now disabled to prevent futher issues.");
							MC.sendChatMessage("\u00A74Exception detail: " + e.toString());
						} catch (RuntimeException ignored) {
						}
					}
				}
				profiler.pop(); // end LOD
				profiler.push("terrain"); // go back into "terrain"
			} else if (CONFIG.client().advanced().debugging().getRendererType() == RendererType.DEBUG) {
				IProfilerWrapper profiler = MC.getProfiler();
				profiler.pop(); // get out of "terrain"
				profiler.push("LODTestRendering");
				ClientApi.testRenderer.render();
				profiler.pop(); // end LODTestRendering
				profiler.push("terrain"); // go back into "terrain"
			}

			// these can't be set until after the buffers are built (in renderer.drawLODs)
			// otherwise the buffers may be set to the wrong size, or not changed at all
			ApiShared.previousChunkRenderDistance = MC_RENDER.getRenderDistance();
			ApiShared.previousLodRenderDistance = CONFIG.client().graphics().quality().getLodChunkRenderDistance();
		} catch (Exception e) {
			ApiShared.LOGGER.error("client proxy uncaught exception: ", e);
		}
	}

	/** used in a development environment to change settings on the fly */
	private void applyConfigOverrides() {
		// remind the developer(s) that the config override is active
		if (!configOverrideReminderPrinted) {
			MC.sendChatMessage(ModInfo.READABLE_NAME + " experimental build " + ModInfo.VERSION);
			MC.sendChatMessage("You are running an unsupported version of the mod!");
			MC.sendChatMessage("Here be dragons!");

			configOverrideReminderPrinted = true;
		}

		// CONFIG.client().worldGenerator().setDistanceGenerationMode(DistanceGenerationMode.FULL);

		// CONFIG.client().worldGenerator().setGenerationPriority(GenerationPriority.AUTO);

		// CONFIG.client().graphics().advancedGraphics().setGpuUploadMethod(GpuUploadMethod.BUFFER_STORAGE);
		// CONFIG.client().graphics().quality().setLodChunkRenderDistance(128);

		// CONFIG.client().graphics().fogQuality().setFogDrawMode(FogDrawMode.FOG_ENABLED);
		// CONFIG.client().graphics().fogQuality().setFogDistance(FogDistance.FAR);
		// CONFIG.client().graphics().fogQuality().setDisableVanillaFog(true);

		// CONFIG.client().advanced().buffers().setRebuildTimes(BufferRebuildTimes.FREQUENT);

		// CONFIG.client().advanced().debugging().setDebugKeybindingsEnabled(true);
	}

	// =================//
	// DUBUG USE //
	// =================//
	// Trigger once on key press, with CLIENT PLAYER.
	public void keyPressedEvent(int glfwKey) {

		if (!CONFIG.client().advanced().debugging().getDebugKeybindingsEnabled())
			return;

		if (glfwKey == GLFW.GLFW_KEY_F8) {
			CONFIG.client().advanced().debugging()
					.setDebugMode(CONFIG.client().advanced().debugging().getDebugMode().getNext());
			MC.sendChatMessage("F8: Set debug mode to " + CONFIG.client().advanced().debugging().getDebugMode());
		}

		if (glfwKey == GLFW.GLFW_KEY_F6) {
			CONFIG.client().advanced().debugging()
					.setRendererType(RendererType.next(CONFIG.client().advanced().debugging().getRendererType()));
			MC.sendChatMessage("F6: Set rendering to " + CONFIG.client().advanced().debugging().getRendererType());
		}

		if (glfwKey == GLFW.GLFW_KEY_P) {
			prefLoggerEnabled = !prefLoggerEnabled;
			MC.sendChatMessage("P: Debug Pref Logger is " + (prefLoggerEnabled ? "enabled" : "disabled"));
		}
	}

	// =================//
	// Lod maintenance //
	// =================//

	// FIXME: I need a onLastFrameCleanup() callback in Render Thread... Which calls
	// renderer.cleanup()

	/**
	 * This event is called once during the first frame Minecraft renders in the
	 * world.
	 */
	public void firstFrameSetup() {
		// make sure the GLProxy is created before the LodBufferBuilder needs it
		GLProxy.getInstance();

		firstTimeSetupComplete = true;
	}
}
