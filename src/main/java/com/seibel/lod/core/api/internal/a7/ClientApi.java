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

package com.seibel.lod.core.api.internal.a7;

import com.seibel.lod.core.config.Config;
import com.seibel.lod.core.ModInfo;
import com.seibel.lod.core.enums.rendering.EDebugMode;
import com.seibel.lod.core.enums.rendering.ERendererType;
import com.seibel.lod.core.handlers.dependencyInjection.SingletonHandler;
import com.seibel.lod.core.logging.ConfigBasedLogger;
import com.seibel.lod.core.logging.ConfigBasedSpamLogger;
import com.seibel.lod.core.logging.SpamReducedLogger;
import com.seibel.lod.core.a7.level.DhClientServerLevel;
import com.seibel.lod.core.a7.world.DhWorld;
import com.seibel.lod.core.a7.Server;
import com.seibel.lod.core.objects.math.Mat4f;
import com.seibel.lod.core.render.GLProxy;
import com.seibel.lod.core.render.RenderSystemTest;
import com.seibel.lod.core.wrapperInterfaces.chunk.IChunkWrapper;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftRenderWrapper;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IProfilerWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.ILevelWrapper;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * This holds the methods that should be called
 * by the host mod loader (Fabric, Forge, etc.).
 * Specifically for the client.
 * 
 * @author James Seibel
 * @version 2022-4-27
 */
public class ClientApi
{
	public static final Logger LOGGER = LogManager.getLogger(ClientApi.class.getSimpleName());
	public static boolean prefLoggerEnabled = false;
	
	public static final ClientApi INSTANCE = new ClientApi();
	public static RenderSystemTest testRenderer = new RenderSystemTest();
	private static final IMinecraftClientWrapper MC = SingletonHandler.get(IMinecraftClientWrapper.class);
	private static final IMinecraftRenderWrapper MC_RENDER = SingletonHandler.get(IMinecraftRenderWrapper.class);

	public static final boolean ENABLE_LAG_SPIKE_LOGGING = false;
	public static final long LAG_SPIKE_THRESHOLD_NS = TimeUnit.NANOSECONDS.convert(16, TimeUnit.MILLISECONDS);
	
	public static final long SPAM_LOGGER_FLUSH_NS = TimeUnit.NANOSECONDS.convert(1, TimeUnit.SECONDS);

    public static class LagSpikeCatcher {
		long timer = System.nanoTime();
		public LagSpikeCatcher() {}
		public void end(String source) {
			if (!ENABLE_LAG_SPIKE_LOGGING) return;
			timer = System.nanoTime() - timer;
			if (timer > LAG_SPIKE_THRESHOLD_NS) {
				LOGGER.info("LagSpikeCatcher: "+source+" took "+Duration.ofNanos(timer)+"!");
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

	public static void logToChat(Level logLevel, String str) {
		String prefix = "["+ModInfo.READABLE_NAME+"] ";
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
		if (MC != null) MC.sendChatMessage(prefix + str);
	}

	public void clientServerConnected() {
		SharedApi.currentServer = new Server(false);
		SharedApi.currentWorld = new DhWorld(enviroment);
	}
	public void clientServerDisconnected() {
		SharedApi.currentWorld.close();
		SharedApi.currentWorld = null;
		SharedApi.currentServer = null;
	}
	
	public void clientChunkLoadEvent(IChunkWrapper chunk, ILevelWrapper world)
	{
		//TODO: Implement
	}
	public void clientChunkSaveEvent(IChunkWrapper chunk, ILevelWrapper world)
	{
		//TODO: Implement
	}

	public void clientLevelUnloadEvent(ILevelWrapper world)
	{
		if (SharedApi.currentWorld != null) {
			SharedApi.currentWorld.unloadLevel(world);
		}
	}
	public void clientLevelLoadEvent(ILevelWrapper world)
	{
		//TODO: Maybe make DHLevel init no longer depend on needing player entity in single player
		if (SharedApi.currentWorld != null) {
			SharedApi.currentWorld.getOrLoadLevel(world);
		}
	}

	private long lastFlush = 0;


	public void rendererShutdownEvent() {
		IProfilerWrapper profiler = MC.getProfiler();
		profiler.push("DH-RendererShutdown");

		profiler.pop();
	}
	public void rendererStartupEvent() {
		IProfilerWrapper profiler = MC.getProfiler();
		profiler.push("DH-RendererStartup");
		// make sure the GLProxy is created before the LodBufferBuilder needs it
		GLProxy.getInstance();
		profiler.pop();
	}

	public void clientTickEvent() {
		IProfilerWrapper profiler = MC.getProfiler();
		profiler.push("DH-ClientTick");

		boolean doFlush = System.nanoTime() - lastFlush >= SPAM_LOGGER_FLUSH_NS;
		if (doFlush) {
			lastFlush = System.nanoTime();
			SpamReducedLogger.flushAll();
		}
		ConfigBasedLogger.updateAll();
		ConfigBasedSpamLogger.updateAll(doFlush);

		if (SharedApi.currentWorld != null) {
			if (ModInfo.IS_DEV_BUILD) {
				// config overrides should only be used in the developer builds
				applyDeveloperConfigOverrides();
			}
			if (SharedApi.currentServer == null) {
				// In single player. Do client-side ticking system.
				SharedApi.currentWorld.asyncTick();
			}
		}
		profiler.pop();
	}
	
	public void renderLods(ILevelWrapper world, Mat4f mcModelViewMatrix, Mat4f mcProjectionMatrix, float partialTicks)
	{
		IProfilerWrapper profiler = MC.getProfiler();
		profiler.pop(); // get out of "terrain"
		profiler.push("DH-RenderLevel");
		try {
			if (!MC.playerExists()) return;
			if (world == null) return;
			DhWorld DhWorld = SharedApi.currentWorld;
			if (DhWorld == null) return;
			DhClientServerLevel level = (SharedApi.currentServer == null) ? DhWorld.getOrLoadLevel(world) : DhWorld.getLevel(world);
			if (level == null) return;

			if (prefLoggerEnabled) {
				level.dumpRamUsage();
			}

			if (SharedApi.currentServer == null) {
				// In multiplayer, without access to the server-side stuff. So we need to do some extra work.
				level.asyncTick();
			}

			if (Config.Client.Advanced.Debugging.rendererType.get() == ERendererType.DEFAULT) {
				if (MC_RENDER.playerHasBlindnessEffect()) {
					// if the player is blind, don't render LODs,
					// and don't change minecraft's fog
					// which blindness relies on.
					return;
				}
				if (MC_RENDER.getLightmapWrapper() == null)
					return;
				profiler.push("Render-Lods");
				if (!rendererDisabledBecauseOfExceptions) {
					try {
						level.render(mcModelViewMatrix, mcProjectionMatrix, partialTicks, profiler);
					} catch (RuntimeException e) {
						rendererDisabledBecauseOfExceptions = true;
						LOGGER.error("Renderer thrown an uncaught exception: ", e);
						try {
							MC.sendChatMessage("\u00A74\u00A7l\u00A7uERROR: Distant Horizons"
									+ " renderer has encountered an exception!");
							MC.sendChatMessage("\u00A74Renderer is now disabled to prevent further issues.");
							MC.sendChatMessage("\u00A74Exception detail: " + e.toString());
						} catch (RuntimeException ignored) {
						}
					}
				}
				profiler.pop(); // "Render-Lods"
			} else if (Config.Client.Advanced.Debugging.rendererType.get() == ERendererType.DEBUG) {
				profiler.push("Render-Test");
				try {
					ClientApi.testRenderer.render();
				} catch (RuntimeException e) {
					LOGGER.error("Renderer thrown an uncaught exception: ", e);
					try {
						MC.sendChatMessage("\u00A74\u00A7l\u00A7uERROR: Distant Horizons"
								+ " renderer has encountered an exception!");
						MC.sendChatMessage("\u00A74Renderer is now disabled to prevent further issues.");
						MC.sendChatMessage("\u00A74Exception detail: " + e.toString());
					} catch (RuntimeException ignored) {
					}
				}
				profiler.pop(); // end LODTestRendering
			}
		}
		catch (Exception e)
		{
			LOGGER.error("client level rendering uncaught exception: ", e);
		} finally {
			profiler.pop(); // end LOD
			profiler.push("terrain"); // go back into "terrain"
		}
	}
	
	/** used in a development environment to change settings on the fly */
	private void applyDeveloperConfigOverrides()
	{
		// remind the user that the config override is active
		if (!configOverrideReminderPrinted && MC.playerExists())
		{
			MC.sendChatMessage(ModInfo.READABLE_NAME + " experimental build " + ModInfo.VERSION);
			MC.sendChatMessage("You are running an unsupported version of the mod!");
			MC.sendChatMessage("Here be dragons!");
			configOverrideReminderPrinted = true;
		}
	}

	//=================//
	//    DEBUG USE    //
	//=================//
	
	// Trigger once on key press, with CLIENT PLAYER.
	public void keyPressedEvent(int glfwKey)
	{
		if (!Config.Client.Advanced.Debugging.enableDebugKeybindings.get())
			return;
		
		if (glfwKey == GLFW.GLFW_KEY_F8)
		{
			Config.Client.Advanced.Debugging.debugMode.set(EDebugMode.next(Config.Client.Advanced.Debugging.debugMode.get()));
			MC.sendChatMessage("F8: Set debug mode to " + Config.Client.Advanced.Debugging.debugMode.get());
		}
		if (glfwKey == GLFW.GLFW_KEY_F6)
		{
			Config.Client.Advanced.Debugging.rendererType.set(ERendererType.next(Config.Client.Advanced.Debugging.rendererType.get()));
			MC.sendChatMessage("F6: Set rendering to " + Config.Client.Advanced.Debugging.rendererType.get());
		}
		if (glfwKey == GLFW.GLFW_KEY_P)
		{
			prefLoggerEnabled = !prefLoggerEnabled;
			MC.sendChatMessage("P: Debug Pref Logger is " + (prefLoggerEnabled ? "enabled" : "disabled"));
		}
	}
	
	
}
