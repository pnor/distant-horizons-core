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
public interface DataContainer
{
	public short getPositionData(int posX, int posZ);
	
	public int getVerticalData(int posX, int posZ, int verticalIndex);
	
	public int getColorData(int posX, int posZ, int verticalIndex);
	
	public byte getLightData(int posX, int posZ, int verticalIndex);
	
	public void setPositionData(short positionData, int posX, int posZ);
	
	public void setVerticalData(int verticalData, int posX, int posZ, int verticalIndex);
	public void setColorData(int colorData, int posX, int posZ, int verticalIndex);
	
	public void setLightData(byte lightData, int posX, int posZ, int verticalIndex);
	
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
}
