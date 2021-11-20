package com.seibel.lod.core.wrapperAdapters;

import com.seibel.lod.core.builders.lodBuilding.LodBuilder;
import com.seibel.lod.core.objects.lod.LodDimension;
import com.seibel.lod.core.wrapperAdapters.block.AbstractBlockPosWrapper;
import com.seibel.lod.core.wrapperAdapters.chunk.AbstractChunkPosWrapper;
import com.seibel.lod.core.wrapperAdapters.world.IWorldWrapper;
import com.seibel.lod.core.wrapperAdapters.worldGeneration.AbstractWorldGeneratorWrapper;

/**
 * This handles creating abstract wrapper objects.
 * 
 * @author James Seibel
 * @version 11-18-2021
 */
public interface IWrapperFactory
{	
	public AbstractBlockPosWrapper createBlockPos();
	public AbstractBlockPosWrapper createBlockPos(int x, int y, int z);
	
	
	public AbstractChunkPosWrapper createChunkPos();
	public AbstractChunkPosWrapper createChunkPos(int x, int z);
	public AbstractChunkPosWrapper createChunkPos(AbstractChunkPosWrapper newChunkPos);
	public AbstractChunkPosWrapper createChunkPos(AbstractBlockPosWrapper blockPos);
	
	
	public AbstractWorldGeneratorWrapper createWorldGenerator(LodBuilder newLodBuilder, LodDimension newLodDimension, IWorldWrapper worldWrapper);
}
