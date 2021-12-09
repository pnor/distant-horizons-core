package com.seibel.lod.core.dataFormat;

public class PositionDataFormat
{
	public final static int LOD_COUNT_SHIFT = 6;
	public final static int CORRECT_LIGHT_SHIFT = 5;
	public final static int GEN_TYPE_SHIFT = 2;
	public final static int VOID_SHIFT = 1;
	public final static int EXISTENCE_SHIFT = 0;
	
	public final static short LOD_COUNT_MASK = 0b11_1111;
	public final static short CORRECT_LIGHT_MASK = 0b1;
	public final static short GEN_TYPE_MASK = 0b111;
	public final static short VOID_MASK = 1;
	public final static short EXISTENCE_MASK = 1;
}
