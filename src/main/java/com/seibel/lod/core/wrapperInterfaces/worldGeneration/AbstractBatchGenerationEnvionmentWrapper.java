package com.seibel.lod.core.wrapperInterfaces.worldGeneration;

import com.seibel.lod.core.objects.opengl.builders.lodBuilding.LodBuilder;
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

	public abstract boolean tryAddPoint(int chunkX, int chunkZ, int genSize, Steps targetStep, boolean genAllDetails);

	public abstract void stop(boolean blocking);
}
