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

package com.seibel.lod.core.objects.lod;

import com.seibel.lod.core.objects.LodDataView;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * A level container is a quad tree level
 */
public interface LevelContainer
{
	/**
	 * With this you can add data to the level container
	 * @param data actual data to add in an array of long format.
	 * @param posX x position in the detail level
	 * @param posZ z position in the detail level
	 * @param index vertical position in the detail level
	 * @return true if correctly added, false otherwise
	 */
	boolean addData(long data, int posX, int posZ, int index);

	/**
	 * With this you can add data to the level container
	 * @param data actual data to add in an array of long[] format.
	 * @param posX x position in the detail level
	 * @param posZ z position in the detail level
	 * @return true if correctly changed, false otherwise
	 */
	@Deprecated
	boolean addVerticalData(long[] data, int posX, int posZ, boolean override);
	boolean copyVerticalData(LodDataView data, int posX, int posZ, boolean override);
	
	/**
	 * With this you can add a square of data to the level container
	 * @return true if anything changed, false otherwise
	 */
	@Deprecated
	boolean addChunkOfData(long[] data, int posX, int posZ, int widthX, int widthZ, boolean override);
	boolean copyChunkOfData(LodDataView data, int posX, int posZ, int widthX, int widthZ, boolean override);

	/**
	 * With this you can get data from the level container
	 * @param posX x position in the detail level
	 * @param posZ z position in the detail level
	 * @return the data in long array format
	 */
	long getData(int posX, int posZ, int index);

	/**
	 * With this you can get data from the level container
	 * @param posX x position in the detail level
	 * @param posZ z position in the detail level
	 * @return the data in long array format
	 */
	@Deprecated
	long[] getAllData(int posX, int posZ);
	LodDataView getVerticalDataView(int posX, int posZ);
	
	/**
	 * With this you can get data from the level container
	 * @param posX x position in the detail level
	 * @param posZ z position in the detail level
	 * @return the data in long array format
	 */
	long getSingleData(int posX, int posZ);
	
	/**
	 * @param posX x position in the detail level
	 * @param posZ z position in the detail level
	 * @return true only if the data exist
	 */
	boolean doesItExist(int posX, int posZ);
	
	/**
	 * @return return the detailLevel of this level container
	 */
	byte getDetailLevel();
	
	
	int getVerticalSize();
	
	/** Clears the dataPoint at the given array index */
	void clear(int posX, int posZ);
	
	/**
	 * This return a level container with detail level lower than the current level.
	 * The new level container may use information of this level.
	 * @return the new level container
	 */
	LevelContainer expand();
	
	/**
	 * @param lowerLevelContainer lower level where we extract the data
	 * @param posX x position in the detail level to update
	 * @param posZ z position in the detail level to update
	 */
	void updateData(LevelContainer lowerLevelContainer, int posX, int posZ);

	/**
	 * This will write the raw data with metadata to the output stream
	 * @return isAllGenerated whether the data is all generated
	 * @throws IOException 
	 */
	boolean writeData(DataOutputStream output) throws IOException;
	
	/**
	 * This will give the data to save in the file
	 * @return data as a String
	 */
	int getMaxNumberOfLods();

	/**
	 * This will return a ram usage estimation of this object
	 * @return long as byte
	 */
	long getRoughRamUsage();

}
