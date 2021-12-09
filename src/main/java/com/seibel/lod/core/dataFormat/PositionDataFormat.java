package com.seibel.lod.core.dataFormat;

public class PositionDataFormat
{
	public final static byte LOD_COUNT_SHIFT = 6;
	public final static byte CORRECT_LIGHT_SHIFT = 5;
	public final static byte GEN_TYPE_SHIFT = 2;
	public final static byte VOID_SHIFT = 1;
	public final static byte EXISTENCE_SHIFT = 0;
	
	//We are able to count up to 64 different lods in a column
	public final static short LOD_COUNT_MASK = 0b11_1111;
	public final static short CORRECT_LIGHT_MASK = 0b1;
	public final static short GEN_TYPE_MASK = 0b111;
	public final static short VOID_MASK = 0b1;
	public final static short EXISTENCE_MASK = 0b1;
	
	public final static int EMPTY_DATA = 0;
	public final static int VOID_DATA = VOID_MASK<<VOID_SHIFT + EXISTENCE_MASK<<EXISTENCE_SHIFT;
	
}
