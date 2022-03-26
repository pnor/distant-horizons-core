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

package com.seibel.lod.core.wrapperInterfaces;

import com.seibel.lod.core.builders.lodBuilding.LodBuilder;
import com.seibel.lod.core.handlers.dependencyInjection.IBindable;
import com.seibel.lod.core.objects.lod.LodDimension;
import com.seibel.lod.core.wrapperInterfaces.block.AbstractBlockPosWrapper;
import com.seibel.lod.core.wrapperInterfaces.chunk.AbstractChunkPosWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IWorldWrapper;
import com.seibel.lod.core.wrapperInterfaces.worldGeneration.AbstractBatchGenerationEnvionmentWrapper;

/**
 * This handles creating abstract wrapper objects.
 * 
 * @author James Seibel
 * @version 3-5-2022
 */
public interface IWrapperFactory extends IBindable 
{
	AbstractBlockPosWrapper createBlockPos();
	AbstractBlockPosWrapper createBlockPos(int x, int y, int z);

	AbstractChunkPosWrapper createChunkPos();
	AbstractChunkPosWrapper createChunkPos(long xAndZPositionCombined);
	AbstractChunkPosWrapper createChunkPos(int x, int z);
	AbstractChunkPosWrapper createChunkPos(AbstractChunkPosWrapper newChunkPos);
	AbstractChunkPosWrapper createChunkPos(AbstractBlockPosWrapper blockPos);

	AbstractBatchGenerationEnvionmentWrapper createBatchGenerator(LodBuilder newLodBuilder,
			LodDimension newLodDimension, IWorldWrapper worldWrapper);
}
