package com.seibel.lod.core.dataFormat;

public class VerticalDataFormat
{
	public final static int HEIGHT_SHIFT = 20;
	public final static int DEPTH_SHIFT = 8;
	public final static int BOTTOM_TYPE_SHIFT = 1;
	public final static int TRANSPARENCY_SHIFT = 0;
	
	public final static int HEIGHT_MASK = 0b1111_1111_1111;
	public final static int DEPTH_MASK = 0b1111_1111_1111;
	public final static int TRANSPARENCY_MASK = 0b1;
	public final static int TRANSPARENCY_MASK = 0b1;
	public final static int BOTTOM_TYPE_MASK = 0b1;
	public final static int TRANSPARENCY_MASK = 0b1;
}
