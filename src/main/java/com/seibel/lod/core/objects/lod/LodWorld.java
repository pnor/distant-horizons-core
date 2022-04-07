/*
 *    This file is part of the Distant Horizon mod (formerly the LOD Mod),
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

package com.seibel.lod.core.objects.lod;

import java.util.Hashtable;
import java.util.Map;

import com.seibel.lod.core.api.ApiShared;
import com.seibel.lod.core.wrapperInterfaces.world.IDimensionTypeWrapper;

/**
 * This stores all LODs for a given world.
 * @author James Seibel
 * @author Leonardo Amato
 * @version 2022-3-29
 */
public class LodWorld
{
	/** name of this world */
	private String worldName;
	
	/** dimensions in this world */
	private Map<IDimensionTypeWrapper, LodDimension> lodDimensions;
	
	/** If true then the LOD world is setup and ready to use */
	private boolean isWorldLoaded = false;
	
	/** the name given to the world if it isn't loaded */
	public static final String NO_WORLD_LOADED = "";
	
	
	
	public LodWorld()
	{
		worldName = NO_WORLD_LOADED;
	}
	
	
	
	/**
	 * Set up the LodWorld with the given newWorldName. <br>
	 * This should be done whenever loading a new world. <br><br>
	 * <p>
	 * Note a System.gc() call may be in order after calling this <Br>
	 * since a lot of LOD data is now homeless. <br>
	 * @param newWorldName name of the world
	 */
	public void selectWorld(String newWorldName)
	{
		ApiShared.LOGGER.info("Selecting world {} while in world {}", newWorldName, worldName);
		if (worldName.equals(newWorldName))
			// don't recreate everything if we
			// didn't actually change worlds
			return;
		
		deselectWorld();
		worldName = newWorldName;
		lodDimensions = new Hashtable<>();
		isWorldLoaded = true;
	}
	
	/**
	 * Clear the lodDimensions Map. <br>
	 * This should be done whenever unloaded a world. <br><br>
	 * <p>
	 * Note a System.gc() call may be in order after calling this <Br>
	 * since a lot of LOD data is now homeless. <br>
	 */
	public void deselectWorld()
	{
		ApiShared.LOGGER.info("Deselecting world {}", worldName);
		worldName = NO_WORLD_LOADED;
		saveAllDimensions(true); // Make sure all dims are saved. This will block threads
		lodDimensions = null;
		isWorldLoaded = false;
	}
	
	
	/**
	 * Adds newDimension to this world, if a LodDimension
	 * already exists for the given dimension it is replaced.
	 */
	public void addLodDimension(LodDimension newDimension)
	{
		if (lodDimensions == null)
			return;
		ApiShared.LOGGER.info("Adding dim {} to world {}", newDimension, worldName);
		
		LodDimension oldDim = lodDimensions.put(newDimension.dimension, newDimension);
		if (oldDim != null)
			oldDim.saveDirtyRegionsToFile(true);
	}
	
	/**
	 * Returns null if no LodDimension exists for the given dimension
	 */
	public LodDimension getLodDimension(IDimensionTypeWrapper dimType)
	{
		if (lodDimensions == null)
			return null;
		
		return lodDimensions.get(dimType);
	}
	
	/**
	 * Resizes the max width in regions that each LodDimension
	 * should use.
	 */
	public void resizeDimensionRegionWidth(int newRegionWidth)
	{
		if (lodDimensions == null)
			return;
		
		saveAllDimensions(true); //block until saving is done
		
		for (IDimensionTypeWrapper key : lodDimensions.keySet())
			lodDimensions.get(key).setRegionWidth(newRegionWidth);
	}
	
	/**
	 * Requests all dimensions save any dirty regions they may have.
	 */
	public void saveAllDimensions(boolean isBlocking)
	{
		if (lodDimensions == null)
			return;
		
		// TODO we should only print this if lods were actually saved to file
		// but that requires a LodDimension.hasDirtyRegions() method or something similar
		ApiShared.LOGGER.info("Saving LODs");
		
		for (IDimensionTypeWrapper key : lodDimensions.keySet())
		{
			lodDimensions.get(key).saveDirtyRegionsToFile(isBlocking);
		}
		//FIXME: This should block until file is saved.
	}
	
	/**
	 * Requests all dimensions to shutdown
	 */
	public void shutdownAllDimensions()
	{
		if (lodDimensions == null)
			return;
		
		// TODO: Add parallel shutdowns.
		for (IDimensionTypeWrapper key : lodDimensions.keySet())
		{
			lodDimensions.get(key).shutdown();
		}
	}
	
	
	public boolean getIsWorldNotLoaded()
	{
		return !isWorldLoaded;
	}
	
	public String getWorldName()
	{
		return worldName;
	}
	
	@Override
	public String toString()
	{
		return "World name: " + worldName;
	}
}

