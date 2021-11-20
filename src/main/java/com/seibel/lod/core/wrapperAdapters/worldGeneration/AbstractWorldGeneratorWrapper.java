package com.seibel.lod.core.wrapperAdapters.worldGeneration;

import com.seibel.lod.core.builders.lodBuilding.LodBuilder;
import com.seibel.lod.core.enums.config.DistanceGenerationMode;
import com.seibel.lod.core.objects.lod.LodDimension;
import com.seibel.lod.core.wrapperAdapters.chunk.AbstractChunkPosWrapper;
import com.seibel.lod.core.wrapperAdapters.world.IWorldWrapper;

/**
 * This is used for generating chunks
 * in a variety of detail and threading levels.
 * 
 * @author James Seibel
 * @version 11-20-2021
 */
public abstract class AbstractWorldGeneratorWrapper
{
	public AbstractWorldGeneratorWrapper(LodBuilder newLodBuilder, LodDimension newLodDimension, IWorldWrapper worldWrapper)
	{
		
	}
	
	
	public abstract void generateBiomesOnly(AbstractChunkPosWrapper pos, DistanceGenerationMode generationMode);
	
	public abstract void generateSurface(AbstractChunkPosWrapper pos);
	
	public abstract void generateFeatures(AbstractChunkPosWrapper pos);
	
	public abstract void generateFull(AbstractChunkPosWrapper pos);
}
