package com.seibel.lod.core.wrapperAdapters.world;

/**
 * This class wraps the minecraft BlockPos.Mutable (and BlockPos) class
 * 
 * @author James Seibel
 * @version 11-15-2021
 */
public interface IBiomeWrapper
{
	/** Returns a color int for the given biome. */
	public int getColorForBiome(int x, int z);
	
	public int getGrassTint(int x, int z);
	
	public int getFolliageTint();
	
	public int getWaterTint();
	
}
