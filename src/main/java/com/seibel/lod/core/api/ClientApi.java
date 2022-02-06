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

import java.lang.ref.WeakReference;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

import com.seibel.lod.core.ModInfo;
import com.seibel.lod.core.enums.config.DistanceGenerationMode;
import com.seibel.lod.core.objects.lod.LodDimension;
import com.seibel.lod.core.objects.math.Mat4f;
import com.seibel.lod.core.render.GLProxy;
import com.seibel.lod.core.render.LodRenderer;
import com.seibel.lod.core.util.DetailDistanceUtil;
import com.seibel.lod.core.util.SingletonHandler;
import com.seibel.lod.core.util.SpamReducedLogger;
import com.seibel.lod.core.wrapperInterfaces.IWrapperFactory;
import com.seibel.lod.core.wrapperInterfaces.chunk.IChunkWrapper;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftRenderWrapper;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftWrapper;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IProfilerWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IWorldWrapper;

/**
 * This holds the methods that should be called
 * by the host mod loader (Fabric, Forge, etc.).
 * Specifically for the client.
 * 
 * @author James Seibel
 * @version 12-8-2021
 */
public class ClientApi
{	
	public static boolean prefLoggerEnabled = false;
	public static List<WeakReference<SpamReducedLogger>> spamReducedLoggers
		= Collections.synchronizedList(new LinkedList<WeakReference<SpamReducedLogger>>());
	
	public static final ClientApi INSTANCE = new ClientApi();
	public static final Logger LOGGER = LogManager.getLogger(ModInfo.NAME);
	
	public static LodRenderer renderer = new LodRenderer(ApiShared.lodBufferBuilderFactory);
	
	private static final IMinecraftWrapper MC = SingletonHandler.get(IMinecraftWrapper.class);
	private static final IMinecraftRenderWrapper MC_RENDER = SingletonHandler.get(IMinecraftRenderWrapper.class);
	private static final ILodConfigWrapperSingleton CONFIG = SingletonHandler.get(ILodConfigWrapperSingleton.class);
	private static final IWrapperFactory FACTORY = SingletonHandler.get(IWrapperFactory.class);
	private static final EventApi EVENT_API = EventApi.INSTANCE;

	public static final boolean ENABLE_LAG_SPIKE_LOGGING = false;
	public static final long LAG_SPIKE_THRESOLD_NS = TimeUnit.NANOSECONDS.convert(16, TimeUnit.MILLISECONDS);
	
	public static final long SPAM_LOGGER_FLUSH_NS = TimeUnit.NANOSECONDS.convert(1, TimeUnit.SECONDS);
	
	public static class LagSpikeCatcher {

		long timer = System.nanoTime();
		public LagSpikeCatcher() {}
		public void end(String source) {
			if (!ENABLE_LAG_SPIKE_LOGGING) return;
			timer = System.nanoTime() - timer;
			if (timer > LAG_SPIKE_THRESOLD_NS) {
				ClientApi.LOGGER.info("LagSpikeCatcher: "+source+" took "+Duration.ofNanos(timer)+"!");
			}
		}
	}
	
	/**
	 * there is some setup that should only happen once,
	 * once this is true that setup has completed
	 */
	private boolean firstTimeSetupComplete = false;
	private boolean configOverrideReminderPrinted = false;
	
	public boolean rendererDisabledBecauseOfExceptions = false;
	
	
	private ClientApi()
	{
		
	}
	
	private void flushSpamLoggersState() {
		synchronized(spamReducedLoggers) {
			spamReducedLoggers.removeIf((logger) -> logger.get()==null);
			spamReducedLoggers.forEach((logger) -> {
				SpamReducedLogger l = logger.get();
				if (l!=null) l.reset();
			});
		}
	}

	private final ConcurrentHashMap.KeySetView<Long,Boolean> generating = ConcurrentHashMap.newKeySet();
	public final ConcurrentHashMap.KeySetView<Long,Boolean> toBeLoaded = ConcurrentHashMap.newKeySet();
	
	public void clientChunkLoadEvent(IChunkWrapper chunk, IWorldWrapper world)
	{
		LagSpikeCatcher clientChunkLoad = new LagSpikeCatcher();
		//ClientApi.LOGGER.info("Lod Generating add: "+chunk.getLongChunkPos());
		toBeLoaded.add(chunk.getLongChunkPos());
		clientChunkLoad.end("clientChunkLoad");
	}

	private long lastFlush = 0;
	
	public void renderLods(Mat4f mcModelViewMatrix, Mat4f mcProjectionMatrix, float partialTicks)
	{
		// comment out when creating a release
		applyConfigOverrides();

		// clear any out of date objects
		MC.clearFrameObjectCache();
		
		try
		{
			if (System.nanoTime() - lastFlush >= SPAM_LOGGER_FLUSH_NS) {
				lastFlush = System.nanoTime();
				flushSpamLoggersState();
			}
			
			
			// only run the first time setup once
			if (!firstTimeSetupComplete)
				firstFrameSetup();

			if (!MC.playerExists() || ApiShared.lodWorld.getIsWorldNotLoaded())
				return;
			
			IWorldWrapper world = MC.getWrappedClientWorld();
			if (world == null) return;
			LodDimension lodDim = ApiShared.lodWorld.getLodDimension(world.getDimensionType());
			
			// Make the LodDim if it does not exist
			if (lodDim == null)
			{
				lodDim = new LodDimension(world.getDimensionType(), ApiShared.lodWorld,
						ApiShared.lodBuilder.defaultDimensionWidthInRegions);
				ApiShared.lodWorld.addLodDimension(lodDim);
			}
			if (prefLoggerEnabled) lodDim.dumpRamUsage();

			LagSpikeCatcher updateToBeLoadedChunk = new LagSpikeCatcher();
			for (long pos : toBeLoaded) {
				if (generating.size() >= 8) {
					//ClientApi.LOGGER.info("Lod Generating Full! Remining: "+toBeLoaded.size());
					break;
				}
				IChunkWrapper chunk = world.tryGetChunk(FACTORY.createChunkPos(pos));
				if (chunk == null) {
					toBeLoaded.remove(pos);
					continue;
				}
				//if (!chunk.isLightCorrect()) continue;
				toBeLoaded.remove(pos);
				generating.add(pos);
				//ClientApi.LOGGER.info("Lod Generation trying "+pos+". Remining: " +toBeLoaded.size());
				ApiShared.lodBuilder.generateLodNodeAsync(chunk, ApiShared.lodWorld,
						world.getDimensionType(), DistanceGenerationMode.FULL, true, () -> {
							//ClientApi.LOGGER.info("Lod Generation for "+pos+" done. Remining: " +toBeLoaded.size());
							generating.remove(pos);
						}, () -> {
							generating.remove(pos);
							toBeLoaded.add(pos);
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
			
			
			
			if (CONFIG.client().advanced().debugging().getDrawLods())
			{
				// Note to self:
				// if "unspecified" shows up in the pie chart, it is
				// possibly because the amount of time between sections
				// is too small for the profiler to measure
				IProfilerWrapper profiler = MC.getProfiler();
				profiler.pop(); // get out of "terrain"
				profiler.push("LOD");
				
				if (!rendererDisabledBecauseOfExceptions) {
					try {
						ClientApi.renderer.drawLODs(lodDim, mcModelViewMatrix, mcProjectionMatrix, partialTicks, MC.getProfiler());
					} catch (RuntimeException e) {
						rendererDisabledBecauseOfExceptions = true;
						try {
							//ClientApi.renderer.ma  ();
						} catch (RuntimeException welpLookLikeWeWillLeakResource) {}
						throw e;
					}
				}
				profiler.pop(); // end LOD
				profiler.push("terrain"); // go back into "terrain"
			}
			
			
			
			// these can't be set until after the buffers are built (in renderer.drawLODs)
			// otherwise the buffers may be set to the wrong size, or not changed at all
			ApiShared.previousChunkRenderDistance = MC_RENDER.getRenderDistance();
			ApiShared.previousLodRenderDistance = CONFIG.client().graphics().quality().getLodChunkRenderDistance();
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
			MC.sendChatMessage(ModInfo.READABLE_NAME + " experimental build " + ModInfo.VERSION);
			MC.sendChatMessage("You are running an unsupported version of the mod!");
			MC.sendChatMessage("Here be dragons!");
			
			configOverrideReminderPrinted = true;
		}
		
//		CONFIG.client().worldGenerator().setDistanceGenerationMode(DistanceGenerationMode.FULL);
		
//		CONFIG.client().worldGenerator().setGenerationPriority(GenerationPriority.AUTO);		
		
//		CONFIG.client().graphics().advancedGraphics().setGpuUploadMethod(GpuUploadMethod.BUFFER_STORAGE);
//		CONFIG.client().graphics().quality().setLodChunkRenderDistance(128);
		
//		CONFIG.client().graphics().fogQuality().setFogDrawMode(FogDrawMode.FOG_ENABLED);
//		CONFIG.client().graphics().fogQuality().setFogDistance(FogDistance.FAR);
//		CONFIG.client().graphics().fogQuality().setDisableVanillaFog(true);
		
//		CONFIG.client().advanced().buffers().setRebuildTimes(BufferRebuildTimes.FREQUENT);
		
		
		CONFIG.client().advanced().debugging().setDebugKeybindingsEnabled(true);
	}

	//=================//
	//    DUBUG USE    //
	//=================//
	// Trigger once on key press, with CLIENT PLAYER.
	public void keyPressedEvent(int glfwKey) {
		ClientApi.LOGGER.info("DEBUG: Key Pressed: {}",glfwKey);
		
		if (glfwKey == GLFW.GLFW_KEY_F8) {
					CONFIG.client().advanced().debugging()
					.setDebugMode(CONFIG.client().advanced().debugging().getDebugMode().getNext());
			MC.sendChatMessage("F8: Set debug mode to " + CONFIG.client().advanced().debugging().getDebugMode());
		}
		
		if (glfwKey == GLFW.GLFW_KEY_F6) {
			CONFIG.client().advanced().debugging()
					.setDrawLods(!CONFIG.client().advanced().debugging().getDrawLods());
			MC.sendChatMessage("F6: Set rendering to " + CONFIG.client().advanced().debugging().getDrawLods());
		}

		if (glfwKey == GLFW.GLFW_KEY_P) {
			prefLoggerEnabled = !prefLoggerEnabled;
			MC.sendChatMessage("P: Debug Pref Logger is " + (prefLoggerEnabled ? "enabled" : "disabled"));
		}
		
	}
	
	
	
	//=================//
	// Lod maintenance //
	//=================//
	
	// FIXME: I need a onLastFrameCleanup() callback in Render Thread... Which calls renderer.cleanup()
	
	/** This event is called once during the first frame Minecraft renders in the world. */
	public void firstFrameSetup()
	{
		// make sure the GLProxy is created before the LodBufferBuilder needs it
		GLProxy.getInstance();
		
		firstTimeSetupComplete = true;
	}
	
	
	
	
	

	
	
	
}
