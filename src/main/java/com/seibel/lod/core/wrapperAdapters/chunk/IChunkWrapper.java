package com.seibel.lod.core.wrapperAdapters.chunk;

import com.seibel.lod.core.wrapperAdapters.block.IBlockColorWrapper;
import com.seibel.lod.core.wrapperAdapters.block.IBlockShapeWrapper;
import com.seibel.lod.wrappers.block.BlockPosWrapper;
import com.seibel.lod.wrappers.chunk.ChunkPosWrapper;
import com.seibel.lod.wrappers.world.BiomeWrapper;

/**
 * 
 * @author ??
 * @version 11-17-2021
 */
public interface IChunkWrapper
{
	public int getHeight();
	
	public boolean isPositionInWater(BlockPosWrapper blockPos);
	
	public int getHeightMapValue(int xRel, int zRel);
	
	public BiomeWrapper getBiome(int xRel, int yAbs, int zRel);
	
	public IBlockColorWrapper getBlockColorWrapper(BlockPosWrapper blockPos);
	
	public IBlockShapeWrapper getBlockShapeWrapper(BlockPosWrapper blockPos);
	
	public ChunkPosWrapper getPos();
	
	public boolean isLightCorrect();
	
	public boolean isWaterLogged(BlockPosWrapper blockPos);
	
	public int getEmittedBrightness(BlockPosWrapper blockPos);
}
