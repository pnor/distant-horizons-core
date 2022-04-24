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

package com.seibel.lod.core.wrapperInterfaces.world;

import java.io.File;

import com.seibel.lod.core.enums.WorldType;
import com.seibel.lod.core.handlers.dependencyInjection.IBindable;
import com.seibel.lod.core.wrapperInterfaces.chunk.AbstractChunkPosWrapper;
import com.seibel.lod.core.wrapperInterfaces.chunk.IChunkWrapper;

/**
 * Can be either a Server world or a Client world.
 * 
 * @author James Seibel
 * @version 3-5-2022
 */
public interface IWorldWrapper extends IBindable
{
	IDimensionTypeWrapper getDimensionType();
	
	WorldType getWorldType();
	
	int getBlockLight(int x, int y, int z);
	
	int getSkyLight(int x, int y, int z);
	
	boolean hasCeiling();
	
	boolean hasSkyLight();
	
	int getHeight();
	
	int getSeaLevel();
	
	default short getMinHeight()
	{
		return 0;
	}
	
	/** @throws UnsupportedOperationException if the WorldWrapper isn't for a ServerWorld */
	File getSaveFolder() throws UnsupportedOperationException;

	default IChunkWrapper tryGetChunk(AbstractChunkPosWrapper pos) {return null;}

    boolean hasChunkLoaded(int chunkX, int chunkZ);
}
