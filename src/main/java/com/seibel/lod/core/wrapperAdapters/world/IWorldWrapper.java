package com.seibel.lod.core.wrapperAdapters.world;

import java.io.File;

import com.seibel.lod.core.wrapperAdapters.block.AbstractBlockPosWrapper;

/**
 * 
 * @author James Seibel
 * @author ??
 * @version 11-15-2021
 */
public interface IWorldWrapper
{
	public IDimensionTypeWrapper getDimensionType();
	
	public int getBlockLight(AbstractBlockPosWrapper blockPos);
	
	public int getSkyLight(AbstractBlockPosWrapper blockPos);
	
	public IBiomeWrapper getBiome(AbstractBlockPosWrapper blockPos);
	
	public boolean hasCeiling();
	
	public boolean hasSkyLight();
	
	public boolean isEmpty();
	
	public int getHeight();
	
	public int getSeaLevel();
	
	/** @throws UnsupportedOperationException if the WorldWrapper isn't for a ServerWorld */
	public File getSaveFolder() throws UnsupportedOperationException;

	
}
