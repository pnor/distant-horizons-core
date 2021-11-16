package com.seibel.lod.core.wrapperAdapters.world;

/**
 * 
 * 
 * @author James Seibel
 * @version 11-15-2021
 */
public interface IDimensionTypeWrapper
{
	public String getDimensionName();
	
	public boolean hasCeiling();

	public boolean hasSkyLight();
}
