package com.seibel.lod.core.dataFormat;

public class VerticalDataFormat
{
	public final static short MIN_WORLD_HEIGHT = -2048;
	public final static short MAX_WORLD_HEIGHT = 2047;
	
	public final static byte HEIGHT_SHIFT = 20;
	public final static byte DEPTH_SHIFT = 8;
	public final static byte LEVEL_SHIFT = 3;
	public final static byte BOTTOM_TYPE_SHIFT = 2;
	public final static byte TRANSPARENCY_SHIFT = 1;
	public final static byte EMPTY_LOD_SHIFT = 0;
	
	
	public final static int HEIGHT_MASK = 0b1111_1111_1111;
	public final static int DEPTH_MASK = 0b1111_1111_1111;
	public final static int LEVEL_MASK = 0b111;
	public final static int TRANSPARENCY_MASK = 0b1;
	public final static int BOTTOM_TYPE_MASK = 0b1;
	public final static int EMPTY_LOD_MASK = 0b1;
	
	public final static int EMPTY_LOD = 0;
}
