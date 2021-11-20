package com.seibel.lod.core.wrapperAdapters.worldGeneration;

import com.seibel.lod.core.enums.config.DistanceGenerationMode;
import com.seibel.lod.core.wrapperAdapters.chunk.AbstractChunkPosWrapper;

/**
 * This class contains all the information to generate
 * chunks.
 * 
 * @author James Seibel
 * @version 11-20-2021
 */
public interface IWorldGeneratorWrapper
{
	public void generateBiomesOnly(AbstractChunkPosWrapper pos, DistanceGenerationMode generationMode);
	
	public void generateSurface(AbstractChunkPosWrapper pos);
	
	public void generateFeatures(AbstractChunkPosWrapper pos);
	
	public void generateFull(AbstractChunkPosWrapper pos);
}
