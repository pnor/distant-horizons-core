package com.seibel.lod.core.wrapperAdapters.chunk;

import com.seibel.lod.core.wrapperAdapters.block.AbstractBlockPosWrapper;
import com.seibel.lod.core.wrapperAdapters.block.IBlockColorWrapper;
import com.seibel.lod.core.wrapperAdapters.block.IBlockShapeWrapper;
import com.seibel.lod.forge.wrappers.world.BiomeWrapper;

/**
 * 
 * @author ??
 * @version 11-17-2021
 */
public interface IChunkWrapper
{
	public int getHeight();
	
	public boolean isPositionInWater(AbstractBlockPosWrapper blockPos);
	
	public int getHeightMapValue(int xRel, int zRel);
	
	public BiomeWrapper getBiome(int xRel, int yAbs, int zRel);
	
	public IBlockColorWrapper getBlockColorWrapper(AbstractBlockPosWrapper blockPos);
	
	public IBlockShapeWrapper getBlockShapeWrapper(AbstractBlockPosWrapper blockPos);
	
	public AbstractChunkPosWrapper getPos();
	
	public boolean isLightCorrect();
	
	public boolean isWaterLogged(AbstractBlockPosWrapper blockPos);
	
	public int getEmittedBrightness(AbstractBlockPosWrapper blockPos);
}
