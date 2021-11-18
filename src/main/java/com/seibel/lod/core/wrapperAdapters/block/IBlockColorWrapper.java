package com.seibel.lod.core.wrapperAdapters.block;

/**
 * This class wraps the minecraft Block class
 * 
 * @author James Seibel
 * @version 11-17-2021
 */
public interface IBlockColorWrapper
{
	//--------------//
	//Colors getters//
	//--------------//
	
	public boolean hasColor();
	
	public int getColor();
	
	
	//------------//
	//Tint getters//
	//------------//
	
	public boolean hasTint();
	
	public boolean hasGrassTint();
	
	public boolean hasFolliageTint();
	
	public boolean hasWaterTint();
	
}

