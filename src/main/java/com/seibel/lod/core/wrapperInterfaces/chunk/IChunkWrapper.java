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

package com.seibel.lod.core.wrapperInterfaces.chunk;

import com.seibel.lod.core.wrapperInterfaces.block.AbstractBlockPosWrapper;
import com.seibel.lod.core.wrapperInterfaces.block.IBlockColorWrapper;
import com.seibel.lod.core.wrapperInterfaces.block.IBlockShapeWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IBiomeWrapper;
import com.seibel.lod.forge.wrappers.block.BlockShapeWrapper;
import net.minecraft.block.Block;

/**
 * @author James Seibel
 * @version 11-17-2021
 */
public interface IChunkWrapper
{
	int getHeight();
	
	boolean isPositionInWater(int x, int y, int z);
	
	int getHeightMapValue(int xRel, int zRel);
	
	IBiomeWrapper getBiome(int x, int y, int z);
	IBlockColorWrapper getBlockColorWrapper(int x, int y, int z);
	IBlockShapeWrapper getBlockShapeWrapper(int x, int y, int z);
	
	int getChunkPosX();
	int getChunkPosZ();
	int getRegionPosX();
	int getRegionPosZ();
	int getMaxY(int x, int z);
	int getMaxX();
	int getMaxZ();
	int getMinX();
	int getMinZ();
	
	boolean isLightCorrect();
	
	boolean isWaterLogged(int x, int y, int z);
	
	int getEmittedBrightness(int x, int y, int z);
}
