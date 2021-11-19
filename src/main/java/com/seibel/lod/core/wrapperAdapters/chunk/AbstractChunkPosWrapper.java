package com.seibel.lod.core.wrapperAdapters.chunk;

import com.seibel.lod.core.wrapperAdapters.block.AbstractBlockPosWrapper;


/**
 * This class wraps minecraft's ChunkPos class
 * 
 * @author James Seibel
 * @version 11-18-2021
 */
public abstract class AbstractChunkPosWrapper
{
	public AbstractChunkPosWrapper(AbstractChunkPosWrapper newChunkPos) { }
	public AbstractChunkPosWrapper(AbstractBlockPosWrapper blockPos) { }
	public AbstractChunkPosWrapper(int chunkX, int chunkZ) { }
	public AbstractChunkPosWrapper() { }
	
	
	
	public abstract int getX();
	public abstract int getZ();
	
	public abstract int getMinBlockX();
	public abstract int getMinBlockZ();
	
	public abstract int getRegionX();
	public abstract int getRegionZ();
	
	public abstract AbstractBlockPosWrapper getWorldPosition();
	
}
