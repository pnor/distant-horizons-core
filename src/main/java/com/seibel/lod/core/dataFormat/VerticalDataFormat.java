package com.seibel.lod.core.dataFormat;

public class VerticalDataFormat
{
	public final static short MIN_WORLD_HEIGHT = -2048;
	public final static short MAX_WORLD_HEIGHT = 2047;
	public final static short WORLD_HEIGHT = MAX_WORLD_HEIGHT - MIN_WORLD_HEIGHT;
	
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
	
	public static int createVerticalData(int height, int depth, int level, boolean transparent, boolean bottom)
	{
		int verticalData = 0;
		verticalData |= ((height - MIN_WORLD_HEIGHT) & HEIGHT_MASK) << HEIGHT_SHIFT;
		verticalData |= ((depth - MIN_WORLD_HEIGHT) & DEPTH_MASK) << DEPTH_SHIFT;
		verticalData |= (level & LEVEL_MASK) << LEVEL_SHIFT;
		if (bottom)
			verticalData |= BOTTOM_TYPE_MASK << BOTTOM_TYPE_SHIFT;
		if (transparent)
			verticalData |= TRANSPARENCY_MASK << TRANSPARENCY_SHIFT;
		verticalData |= EMPTY_LOD_MASK << EMPTY_LOD_SHIFT;
		
		return verticalData;
	}
	
	public static String toString(int verticalData, short positionData)
	{
		return getHeight(verticalData) + " " +
					   getDepth(verticalData)  + " " +
					   getLevel(verticalData)  + " " +
					   isTransparent(verticalData)  + " " +
					   isBottom(verticalData)  + " " +
					   doesItExist(verticalData)  + " " + '\n';
	}
	
	public static short getHeight(int verticalData)
	{
		return (short) (((verticalData >>> HEIGHT_SHIFT) & HEIGHT_MASK) + MIN_WORLD_HEIGHT);
	}
	
	public static short getDepth(int verticalData)
	{
		return (short) (((verticalData >>> DEPTH_SHIFT) & DEPTH_MASK) + MIN_WORLD_HEIGHT);
	}
	
	public static byte getLevel(int verticalData)
	{
		return (byte) ((verticalData >>> LEVEL_SHIFT) & LEVEL_MASK);
	}
	
	public static boolean isTransparent(int verticalData)
	{
		return ((verticalData >>> TRANSPARENCY_SHIFT) & TRANSPARENCY_MASK) == 1;
	}
	
	public static boolean isBottom(int verticalData)
	{
		return ((verticalData >>> BOTTOM_TYPE_SHIFT) & BOTTOM_TYPE_MASK) == 1;
	}
	
	public static boolean doesItExist(int verticalData)
	{
		return (((verticalData >>> EMPTY_LOD_SHIFT) & EMPTY_LOD_MASK) == 1);
	}
	
}
