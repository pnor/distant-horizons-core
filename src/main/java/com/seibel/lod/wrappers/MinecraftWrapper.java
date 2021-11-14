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

package com.seibel.lod.wrappers;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;

import com.seibel.lod.ModInfo;
import com.seibel.lod.enums.LodDirection;
import com.seibel.lod.lodApi.ClientApi;
import com.seibel.lod.util.LodUtil;
import com.seibel.lod.wrappers.block.BlockPosWrapper;
import com.seibel.lod.wrappers.chunk.ChunkPosWrapper;
import com.seibel.lod.wrappers.world.DimensionTypeWrapper;
import com.seibel.lod.wrappers.world.WorldWrapper;

import net.minecraft.client.GameSettings;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.model.ModelManager;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.crash.CrashReport;
import net.minecraft.entity.Entity;
import net.minecraft.profiler.IProfiler;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.server.ServerWorld;

/**
 * A singleton that wraps the Minecraft class
 * to allow for easier movement between Minecraft versions.
 * 
 * @author James Seibel
 * @version 9-16-2021
 */
public class MinecraftWrapper
{
	public static final MinecraftWrapper INSTANCE = new MinecraftWrapper();
	
	private final Minecraft mc = Minecraft.getInstance();
	
	/**
	 * The lightmap for the current:
	 * Time, dimension, brightness setting, etc.
	 */
	private NativeImage lightMap = null;
	
	private MinecraftWrapper()
	{
		
	}
	
	
	
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
	public void clearFrameObjectCache()
	{
		lightMap = null;
	}
	
	
	
	//=================//
	// method wrappers //
	//=================//
	
	public float getShade(LodDirection lodDirection)
	{
		Direction mcDir = McObjectConverter.Convert(lodDirection);
		return mc.level.getShade(mcDir, true);
	}
	
	public boolean hasSinglePlayerServer()
	{
		return mc.hasSingleplayerServer();
	}
	
	public DimensionTypeWrapper getCurrentDimension()
	{
		return DimensionTypeWrapper.getDimensionTypeWrapper(mc.player.level.dimensionType());
	}
	
	public String getCurrentDimensionId()
	{
		return LodUtil.getDimensionIDFromWorld(WorldWrapper.getWorldWrapper(mc.level));
	}
	
	/**
	 * This texture changes every frame
	 */
	public NativeImage getCurrentLightMap()
	{
		// get the current lightMap if the cache is empty
		if (lightMap == null)
		{
			LightTexture tex = mc.gameRenderer.lightTexture();
			lightMap = tex.lightPixels;
		}
		return lightMap;
	}
	
	/**
	 * Returns the color int at the given pixel coordinates
	 * from the current lightmap.
	 * @param u x location in texture space
	 * @param v z location in texture space
	 */
	public int getColorIntFromLightMap(int u, int v)
	{
		if (lightMap == null)
		{
			// make sure the lightMap is up-to-date
			getCurrentLightMap();
		}
		
		return lightMap.getPixelRGBA(u, v);
	}
	
	/**
	 * Returns the Color at the given pixel coordinates
	 * from the current lightmap.
	 * @param u x location in texture space
	 * @param v z location in texture space
	 */
	public Color getColorFromLightMap(int u, int v)
	{
		return LodUtil.intToColor(lightMap.getPixelRGBA(u, v));
	}
	
	
	
	
	//=============//
	// Simple gets //
	//=============//
	
	public ClientPlayerEntity getPlayer()
	{
		return mc.player;
	}
	
	public BlockPosWrapper getPlayerBlockPos()
	{
		BlockPos playerPos = getPlayer().blockPosition();
		return new BlockPosWrapper(playerPos.getX(), playerPos.getY(), playerPos.getZ());
	}
	
	public ChunkPosWrapper getPlayerChunkPos()
	{
		return new ChunkPosWrapper(getPlayer().xChunk, getPlayer().zChunk);
	}
	
	public GameSettings getOptions()
	{
		return mc.options;
	}
	
	public ModelManager getModelManager()
	{
		return mc.getModelManager();
	}
	
	public ClientWorld getClientWorld()
	{
		return mc.level;
	}
	
	/** 
	 * Attempts to get the ServerWorld for the dimension
	 * the user is currently in.
	 * @returns null if no ServerWorld is available
	 */
	public WorldWrapper getWrappedServerWorld()
	{
		if (mc.level == null)
			return null;
		
		DimensionType dimension = mc.level.dimensionType();
		IntegratedServer server = mc.getSingleplayerServer();
		
		if (server == null)
			return null;
		
		ServerWorld serverWorld = null;
		Iterable<ServerWorld> worlds = server.getAllLevels();
		for (ServerWorld world : worlds)
		{
			if (world.dimensionType() == dimension)
			{
				serverWorld = world;
				break;
			}
		}
		return WorldWrapper.getWorldWrapper(serverWorld);
	}
	
	public WorldWrapper getWrappedClientWorld()
	{
		return WorldWrapper.getWorldWrapper(mc.level);
	}
	
	/** Measured in chunks */
	public int getRenderDistance()
	{
		return mc.options.renderDistance;
	}
	
	public File getGameDirectory()
	{
		return mc.gameDirectory;
	}
	
	public IProfiler getProfiler()
	{
		return mc.getProfiler();
	}
	
	public ClientPlayNetHandler getConnection()
	{
		return mc.getConnection();
	}
	
	public GameRenderer getGameRenderer()
	{
		return mc.gameRenderer;
	}
	
	public Entity getCameraEntity()
	{
		return mc.cameraEntity;
	}
	
	public MainWindow getWindow()
	{
		return mc.getWindow();
	}
	
	public float getSkyDarken(float partialTicks)
	{
		return mc.level.getSkyDarken(partialTicks);
	}
	
	public IntegratedServer getSinglePlayerServer()
	{
		return mc.getSingleplayerServer();
	}
	
	public ServerData getCurrentServer()
	{
		return mc.getCurrentServer();
	}
	
	public WorldRenderer getLevelRenderer()
	{
		return mc.levelRenderer;
	}
	
	/** Returns all worlds available to the server */
	public ArrayList<WorldWrapper> getAllServerWorlds()
	{
		ArrayList<WorldWrapper> worlds = new ArrayList<WorldWrapper>();
		
		Iterable<ServerWorld> serverWorlds = mc.getSingleplayerServer().getAllLevels();
		for (ServerWorld world : serverWorlds)
		{
			worlds.add(WorldWrapper.getWorldWrapper(world));
		}
		
		return worlds;
	}
	
	
	
	/**
	 * Crashes Minecraft, displaying the given errorMessage <br> <br>
	 * In the following format: <br>
	 * 
	 * The game crashed whilst <strong>errorMessage</strong>  <br>
	 * Error: <strong>ExceptionClass: exceptionErrorMessage</strong>  <br>
	 * Exit Code: -1  <br>
	 */
	public void crashMinecraft(String errorMessage, Throwable exception)
	{
		ClientApi.LOGGER.error(ModInfo.READABLE_NAME + " had the following error: [" + errorMessage + "]. Crashing Minecraft...");
		CrashReport report = new CrashReport(errorMessage, exception);
		Minecraft.crash(report);
	}



	
	
	
}
