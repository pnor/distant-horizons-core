/*
 *    This file is part of the Distant Horizons mod (formerly the LOD Mod),
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
 
package com.seibel.lod.core.wrapperInterfaces.worldGeneration;

import com.seibel.lod.core.builders.lodBuilding.LodBuilder;
import com.seibel.lod.core.objects.lod.LodDimension;
import com.seibel.lod.core.wrapperInterfaces.world.IWorldWrapper;

public abstract class AbstractBatchGenerationEnvionmentWrapper {
	public static enum Steps {
		Empty, StructureStart, StructureReference, Biomes, Noise, Surface, Carvers, LiquidCarvers, Features, Light,
	}

	public AbstractBatchGenerationEnvionmentWrapper(IWorldWrapper serverLevel, LodBuilder lodBuilder,
			LodDimension lodDim) {
	}

	public abstract void resizeThreadPool(int newThreadCount);

	public abstract void updateAllFutures();

	public abstract int getEventCount();

	public abstract boolean tryAddPoint(int chunkX, int chunkZ, int genSize, Steps targetStep, boolean genAllDetails, double runTimeRatio);

	public abstract void stop(boolean blocking);
}
