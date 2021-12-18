package com.seibel.lod.core.wrapperInterfaces.worldGeneration;

import com.seibel.lod.core.builders.lodBuilding.LodBuilder;
import com.seibel.lod.core.objects.lod.LodDimension;
import com.seibel.lod.core.wrapperInterfaces.world.IWorldWrapper;

public abstract class AbstractExperimentalWorldGeneratorWrapper {
	public AbstractExperimentalWorldGeneratorWrapper(LodBuilder newLodBuilder, LodDimension newLodDimension, IWorldWrapper worldWrapper) { }
	public abstract void queueGenerationRequests(LodDimension lodDim, LodBuilder lodBuilder);
	public abstract void stop();
}
