package com.seibel.lod.core.dataFormat;

public class LightFormat
{
	public final static int BLOCK_LIGHT_SHIFT = 4;
	public final static int SKY_LIGHT_SHIFT = 0;
	
	public final static long BLOCK_LIGHT_MASK = 0b1111;
	public final static long SKY_LIGHT_MASK = 0b1111;
}
