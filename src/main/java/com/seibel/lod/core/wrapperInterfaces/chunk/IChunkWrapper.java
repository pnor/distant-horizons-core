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

import com.seibel.lod.core.handlers.dependencyInjection.IBindable;
import com.seibel.lod.core.wrapperInterfaces.block.BlockDetail;
import com.seibel.lod.core.wrapperInterfaces.world.IBiomeWrapper;

/**
 * @author James Seibel
 * @version 11-17-2021
 */
public interface IChunkWrapper extends IBindable
{
	default int getHeight() {
		return getMaxBuildHeight()-getMinBuildHeight();
	}
	int getMinBuildHeight();
	int getMaxBuildHeight();
	
	int getHeightMapValue(int xRel, int zRel);
	
	IBiomeWrapper getBiome(int x, int y, int z);
	
	BlockDetail getBlockDetail(int x, int y, int z);
	
	int getChunkPosX();
	int getChunkPosZ();
	int getRegionPosX();
	int getRegionPosZ();
	int getMaxY(int x, int z);
	int getMaxX();
	int getMaxZ();
	int getMinX();
	int getMinZ();
	
	long getLongChunkPos();
	
	boolean isLightCorrect();
	
	boolean isWaterLogged(int x, int y, int z);
	
	int getEmittedBrightness(int x, int y, int z);
	
	default int getBlockLight(int x, int y, int z) {return -1;}
	
	default int getSkyLight(int x, int y, int z) {return -1;}
	
	default boolean blockPosInsideChunk(int x, int y, int z) {
		return (x>=getMinX() && x<=getMaxX()
				&& y>=getMinBuildHeight() && y<getMaxBuildHeight()
				&& z>=getMinZ() && z<=getMaxZ());
	}
	
	boolean doesNearbyChunksExist();
}
