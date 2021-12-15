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

package com.seibel.lod.core.objects.lod;

/**
 * A level container is a quad tree level
 */
public interface LevelContainer extends DataContainer
{
	
	void addData(int posZ, int posX, short[] inputPositionData, int[] inputVerticalData, int[] inputColorData, byte[] inputLightData, byte inputDetailLevel, int inputVerticalSize);
	
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
	void updateData(DataContainer lowerLevelContainer, int posX, int posZ);
	
	/**
	 * This will give the data to save in the file
	 * @return data as a String
	 */
	byte[] toDataString();
}
