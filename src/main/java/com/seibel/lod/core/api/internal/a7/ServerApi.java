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

import com.seibel.lod.core.handlers.dependencyInjection.SingletonHandler;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.a7.DHWorld;
import com.seibel.lod.core.a7.Server;
import com.seibel.lod.core.wrapperInterfaces.IVersionConstants;
import com.seibel.lod.core.wrapperInterfaces.chunk.IChunkWrapper;
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
public class ServerApi
{
	public static final boolean ENABLE_STACK_DUMP_LOGGING = false;
	public static final ServerApi INSTANCE = new ServerApi();
	private static final Logger LOGGER = DhLoggerBuilder.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());
	private static final IVersionConstants VERSION_CONSTANTS = SingletonHandler.get(IVersionConstants.class);

	private boolean isCurrentlyOnSinglePlayerServer = false;

	private ServerApi()
	{
	}
	
	// =============//
	// tick events  //
	// =============//

	private int lastWorldGenTickDelta = 0;
	public void serverTickEvent()
	{
		lastWorldGenTickDelta--;
		if (SharedApi.currentWorld != null && lastWorldGenTickDelta <= 0) {
			lastWorldGenTickDelta = 20;
			DHWorld dhWorld = SharedApi.currentWorld;
			dhWorld.tick();
		}
	}

	//TODO: rename to serverLoadEvent
	public void serverWorldLoadEvent() {
		SharedApi.currentServer = new Server(!SharedApi.inDedicatedEnvironment);
		SharedApi.currentWorld = new DHWorld();
		//TODO: Setup the network handler
	}

	//TODO: rename to serverUnloadEvent
	public void serverWorldUnloadEvent() {
		//TODO: Close the network handler
		SharedApi.currentWorld.close();
		SharedApi.currentWorld = null;
		SharedApi.currentServer = null;
	}

	public void serverLevelLoadEvent(IWorldWrapper world) {
		//TODO: Maybe make DHLevel init no longer depend on needing player entity in single player
		if (SharedApi.currentServer.isSinglePlayer) return;
		SharedApi.currentWorld.getOrLoadLevel(world);
	}
	public void serverLevelUnloadEvent(IWorldWrapper world) {
		SharedApi.currentWorld.unloadLevel(world);
	}

	@Deprecated
	public void serverSaveEvent() {
		SharedApi.currentWorld.save();
	}


	
	public void chunkSaveEvent(IChunkWrapper chunk, IWorldWrapper world) {
		//TODO
	}

	public void serverChunkLoadEvent(IChunkWrapper chunk, IWorldWrapper world) {
	}
	public void serverChunkSaveEvent(IChunkWrapper chunk, IWorldWrapper world) {
	}
}
