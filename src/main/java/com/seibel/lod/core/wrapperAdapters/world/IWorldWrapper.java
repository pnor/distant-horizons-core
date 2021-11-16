package com.seibel.lod.core.wrapperAdapters.world;

import java.io.File;

import com.seibel.lod.wrappers.block.BlockPosWrapper;

/**
 * 
 * @author James Seibel
 * @author ??
 * @version 11-15-2021
 */
public interface IWorldWrapper
{
	public IDimensionTypeWrapper getDimensionType();
	
	public int getBlockLight(BlockPosWrapper blockPos);
	
	public int getSkyLight(BlockPosWrapper blockPos);
	
	public IBiomeWrapper getBiome(BlockPosWrapper blockPos);
	
	public boolean hasCeiling();
	
	public boolean hasSkyLight();
	
	public boolean isEmpty();
	
	public int getHeight();
	
	/** @throws UnsupportedOperationException if the WorldWrapper isn't for a ServerWorld */
	public File getSaveFolder() throws UnsupportedOperationException;
	
	
}
