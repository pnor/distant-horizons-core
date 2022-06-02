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

package com.seibel.lod.core.wrapperInterfaces.minecraft;

import java.io.File;
import java.util.ArrayList;

import com.seibel.lod.core.enums.LodDirection;
import com.seibel.lod.core.handlers.dependencyInjection.IBindable;
import com.seibel.lod.core.objects.DHBlockPos;
import com.seibel.lod.core.objects.DHChunkPos;
import com.seibel.lod.core.wrapperInterfaces.world.IDimensionTypeWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IWorldWrapper;

/**
 * Contains everything related to the Minecraft object.
 * 
 * @author James Seibel
 * @version 3-5-2022
 */
public interface IMinecraftClientWrapper extends IBindable
{
	//================//
	// helper methods //
	//================//
	
	/**
	 * This should be called at the beginning of every frame to
	 * clear any Minecraft data that becomes out of date after a frame. <br> <br>
	 * <p>
	 * LightMaps and other time sensitive objects fall in this category. <br> <br>
	 * <p>
	 * This doesn't affect OpenGL objects in any way.
	 */
	void clearFrameObjectCache();
	
	
	
	//=================//
	// method wrappers //
	//=================//
	
	float getShade(LodDirection lodDirection);
	
	boolean hasSinglePlayerServer();
	
	String getCurrentServerName();
	String getCurrentServerIp();
	String getCurrentServerVersion();
	
	/** Returns the dimension the player is currently in */
	IDimensionTypeWrapper getCurrentDimension();

	@Deprecated // This should be moved to directly calling the function in core
	String getCurrentDimensionId();

	//=============//
	// Simple gets //
	//=============//
	
	boolean playerExists();
	
	DHBlockPos getPlayerBlockPos();
	
	DHChunkPos getPlayerChunkPos();
	
	/** 
	 * Attempts to get the ServerWorld for the dimension
	 * the user is currently in.
	 * @return null if no ServerWorld is available
	 */
	IWorldWrapper getWrappedServerWorld();
	
	IWorldWrapper getWrappedClientWorld();
	
	File getGameDirectory();
	
	IProfilerWrapper getProfiler();
	
	float getSkyDarken(float partialTicks);
	
	boolean connectedToServer();

	int getPlayerSkylight();
	
	/** Returns all worlds available to the server */
	ArrayList<IWorldWrapper> getAllServerWorlds();
	
	
	
	void sendChatMessage(String string);
	
	/**
	 * Crashes Minecraft, displaying the given errorMessage <br> <br>
	 * In the following format: <br>
	 * 
	 * The game crashed whilst <strong>errorMessage</strong>  <br>
	 * Error: <strong>ExceptionClass: exceptionErrorMessage</strong>  <br>
	 * Exit Code: -1  <br>
	 */
	void crashMinecraft(String errorMessage, Throwable exception);

    Object getOptionsObject();

	File getSinglePlayerServerFolder();
}
