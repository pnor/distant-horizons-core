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
 
package com.seibel.lod.core.handlers.dimensionFinder;


import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.seibel.lod.core.handlers.dependencyInjection.SingletonHandler;
import com.seibel.lod.core.objects.DHBlockPos;
import com.seibel.lod.core.wrapperInterfaces.IWrapperFactory;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import org.lwjgl.system.CallbackI;

import javax.annotation.Nullable;
import java.io.File;

/**
 * Data container for any player data we can use to differentiate one dimension from another.
 *
 * @author James Seibel
 * @version 2022-3-26
 */
public class PlayerData
{
	public static final IWrapperFactory FACTORY = SingletonHandler.get(IWrapperFactory.class);
	
	private static final String playerDataFileName = "_playerData.toml";
	
	
	public static final String PLAYER_BLOCK_POS_X_PATH = "playerBlockPosX";
	public static final String PLAYER_BLOCK_POS_Y_PATH = "playerBlockPosY";
	public static final String PLAYER_BLOCK_POS_Z_PATH = "playerBlockPosZ";
	public DHBlockPos playerBlockPos;
	
	// not implemented yet
	public static final String WORLD_SPAWN_POS_X_PATH = "worldSpawnBlockPosX";
	public static final String WORLD_SPAWN_POS_Y_PATH = "worldSpawnBlockPosY";
	public static final String WORLD_SPAWN_POS_Z_PATH = "worldSpawnBlockPosZ";
	/**
	 * The client world has access to a spawn point, so this should be possible to fill in.
	 * I'm not sure what this will look like for worlds that don't have a spawn point.
	 */
	public DHBlockPos worldSpawnPointBlockPos;
	@Nullable
	public static PlayerData tryGetPlayerData(IMinecraftClientWrapper mcClient) {
		if (!mcClient.playerExists()) return null;
		try {
			return new PlayerData(mcClient);
		} catch (RuntimeException e) {
			// Player no longer exists due to concurrency. FIXME: Remember here is called not on main thread!!!
			return null;
		}
	}
	
	private PlayerData(IMinecraftClientWrapper mc)
	{
		updateData(mc);
	}
	
	public PlayerData(File dimensionFolder)
	{
		File file = getFileForDimensionFolder(dimensionFolder);
		try (CommentedFileConfig toml = CommentedFileConfig.builder(file).build()) {
			toml.load();

			// get the player block pos if it is specified
			if (toml.contains(PLAYER_BLOCK_POS_X_PATH)
					&& toml.contains(PLAYER_BLOCK_POS_Y_PATH)
					&& toml.contains(PLAYER_BLOCK_POS_Z_PATH))
			{
				int x = toml.getIntOrElse(PLAYER_BLOCK_POS_X_PATH, 0);
				int y = toml.getIntOrElse(PLAYER_BLOCK_POS_Y_PATH, 0);
				int z = toml.getIntOrElse(PLAYER_BLOCK_POS_Z_PATH, 0);
				this.playerBlockPos = new DHBlockPos(x, y, z);
			}
			else
			{
				this.playerBlockPos = new DHBlockPos(0, 0, 0);
			}
		}
	}
	
	
	
	public static File getFileForDimensionFolder(File file)
	{
		return new File(file.getPath() + File.separatorChar + playerDataFileName);
	}
	
	
	/** Should be called often to make sure this object is up to date with the player's info */
	public void updateData(IMinecraftClientWrapper mc)
	{
		this.playerBlockPos = mc.getPlayerBlockPos();
		if (playerBlockPos == null) throw new RuntimeException("No player block pos!");
	}
	
	/** Writes everything from this object to the file given. */
	public void toTomlFile(CommentedFileConfig toml)
	{
		// player block pos
		toml.add(PLAYER_BLOCK_POS_X_PATH, playerBlockPos.getX());
		toml.add(PLAYER_BLOCK_POS_Y_PATH, playerBlockPos.getY());
		toml.add(PLAYER_BLOCK_POS_Z_PATH, playerBlockPos.getZ());
		
		toml.save();
	}
	
	
	@Override
	public String toString()
	{
		return "PlayerBlockPos: [" + playerBlockPos.getX() + "," + playerBlockPos.getY() + "," + playerBlockPos.getZ() + "]";
	}
}

