package com.seibel.lod.core.dataFormat;

import com.seibel.lod.core.util.ColorUtil;

public class ColorFormat
{
	public final static int BLUE_SHIFT = 0;
	public final static int GREEN_SHIFT = BLUE_SHIFT + 8;
	public final static int RED_SHIFT = BLUE_SHIFT + 16;
	public final static int ALPHA_SHIFT = BLUE_SHIFT + 24;
	
	public final static long ALPHA_MASK = 0b1111;
	public final static long RED_MASK = 0b1111_1111;
	public final static long GREEN_MASK = 0b1111_1111;
	public final static long BLUE_MASK = 0b1111_1111;
	
	public static int createColorData(int color)
	{
		int colorData = 0;
		colorData += (ColorUtil.getAlpha(color) & ALPHA_MASK) << ALPHA_SHIFT;
		colorData += (ColorUtil.getRed(color) & RED_MASK) << RED_SHIFT;
		colorData += (ColorUtil.getGreen(color) & GREEN_MASK) << GREEN_SHIFT;
		colorData += (ColorUtil.getBlue(color) & BLUE_MASK) << BLUE_SHIFT;
		
		return colorData;
	}
	
	public static int createColorData(int alpha, int red, int green, int blue)
	{
		int colorData = 0;
		colorData += (alpha & ALPHA_MASK) << ALPHA_SHIFT;
		colorData += (red & RED_MASK) << RED_SHIFT;
		colorData += (green & GREEN_MASK) << GREEN_SHIFT;
		colorData += (blue & BLUE_MASK) << BLUE_SHIFT;
		
		return colorData;
	}
	
	public static int getColor(int colorData)
	{
		return colorData;
	}
	
	public static short getAlpha(long colorData)
	{
		return (short) ((colorData >>> ALPHA_SHIFT) & ALPHA_MASK);
	}
	
	public static short getRed(long colorData)
	{
		return (short) ((colorData >>> RED_SHIFT) & RED_MASK);
	}
	
	public static short getGreen(long colorData)
	{
		return (short) ((colorData >>> GREEN_SHIFT) & GREEN_MASK);
	}
	
	public static short getBlue(long colorData)
	{
		return (short) ((colorData >>> BLUE_SHIFT) & BLUE_MASK);
	}
}
