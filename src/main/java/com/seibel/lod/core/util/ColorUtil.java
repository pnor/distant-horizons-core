/*
 *    This file is part of the Distant Horizons mod (formerly the LOD Mod),
 *    licensed under the GNU LGPL v3 License.
 *
 *    Copyright (C) 2020-2022  James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.seibel.lod.core.util;

import com.seibel.lod.core.handlers.dependencyInjection.SingletonHandler;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;

/**
 * 
 * @author Cola
 * @author Leonardo Amato
 * @version 11-13-2021
 */
public class ColorUtil
{
	//note: Minecraft color format is: 0xAA BB GG RR
	//________ DH mod color format is: 0xAA RR GG BB
	//OpenGL RGBA format native order: 0xRR GG BB AA
	//_ OpenGL RGBA format Java Order: 0xAA BB GG RR

	public static final int BLACK = rgbToInt(0,0,0);
	public static final int WHITE = rgbToInt(255,255,255);
	public static final int TRANSPARENT = rgbToInt(0, 0, 0, 0);

	public static final int RED = rgbToInt(255,0,0);
	
	public static int rgbToInt(int red, int green, int blue)
	{
		return (0xFF << 24) | (red << 16) | (green << 8) | blue;
	}
	
	public static int rgbToInt(int alpha, int red, int green, int blue)
	{
		return (alpha << 24) | (red << 16) | (green << 8) | blue;
	}
	public static int rgbToInt(float alpha, float red, float green, float blue)
	{
		return rgbToInt((int)(alpha*255f), (int)(red*255f), (int)(green*255f), (int)(blue*255f));
	}
	
	/** Returns a value between 0 and 255 */
	public static int getAlpha(int color)
	{
		return (color >>> 24) & 0xFF;
	}
	
	/** Returns a value between 0 and 255 */
	public static int getRed(int color)
	{
		return (color >> 16) & 0xFF;
	}
	
	/** Returns a value between 0 and 255 */
	public static int getGreen(int color)
	{
		return (color >> 8) & 0xFF;
	}
	
	/** Returns a value between 0 and 255 */
	public static int getBlue(int color)
	{
		return color & 0xFF;
	}
	
	public static int applyShade(int color, int shade)
	{
		if (shade < 0)
			return (getAlpha(color) << 24) | (Math.max(getRed(color) + shade, 0) << 16) | (Math.max(getGreen(color) + shade, 0) << 8) | Math.max(getBlue(color) + shade, 0);
		else
			return (getAlpha(color) << 24) | (Math.min(getRed(color) + shade, 255) << 16) | (Math.min(getGreen(color) + shade, 255) << 8) | Math.min(getBlue(color) + shade, 255);
	}
	
	public static int applyShade(int color, float shade)
	{
		if (shade < 1)
			return (getAlpha(color) << 24) | ((int) Math.max(getRed(color) * shade, 0) << 16) | ((int) Math.max(getGreen(color) * shade, 0) << 8) | (int) Math.max(getBlue(color) * shade, 0);
		else
			return (getAlpha(color) << 24) | ((int) Math.min(getRed(color) * shade, 255) << 16) | ((int) Math.min(getGreen(color) * shade, 255) << 8) | (int) Math.min(getBlue(color) * shade, 255);
	}
	
	/** Multiply ARGB with RGB colors */
	public static int multiplyARGBwithRGB(int argb, int rgb)
	{
		return ((getAlpha(argb) << 24) | ((getRed(argb) * getRed(rgb) / 255) << 16)
				| ((getGreen(argb) * getGreen(rgb) / 255) << 8) | (getBlue(argb) * getBlue(rgb) / 255));
	}
	
	/** Multiply 2 RGB colors */
	public static int multiplyARGBwithARGB(int color1, int color2)
	{
		return ((getAlpha(color1) * getAlpha(color2) / 255) << 24) | ((getRed(color1) * getRed(color2) / 255) << 16) | ((getGreen(color1) * getGreen(color2) / 255) << 8) | (getBlue(color1) * getBlue(color2) / 255);
	}

	// Below 2 functions are from: https://stackoverflow.com/questions/13806483/increase-or-decrease-color-saturation
	// Alpha in [0.0,1.0], hue in [0.0,360.0], Sat in [0.0,1.0], Value in [0.0,1.0]
	public static float[] argbToAhsv(int color) {
		float a = getAlpha(color) / 255f;
		float r = getRed(color) / 255f;
		float g = getGreen(color) / 255f;
		float b = getBlue(color) / 255f;
		float h, s, v;
		float min = Math.min(Math.min( r, g), b );
		float max = Math.max(Math.max( r, g), b );

		v = max;
		float delta = max - min;
		if( max != 0f )
			s = delta / max;     // s
		else {
			// r = g = b = 0     // s = 0, v is undefined
			return new float[]{a, 0f, 0f, 0f};
		}
		if (delta == 0f) {
			h = 0f;
		} else {
			if (r == max) h = (g - b) / delta; // between yellow & magenta
			else if (g == max) h = 2f + (b - r) / delta;  // between cyan & yellow
			else h = 4f + (r - g) / delta;  // between magenta & cyan
			h *= 60f; // degrees
			if (h < 0f)
				h += 360f;
		}
		return new float[]{a,h,s,v};
	}
	// Alpha in [0.0,1.0], hue in [0.0,360.0], Sat in [0.0,1.0], Value in [0.0,1.0]
	public static int ahsvToArgb(float a, float h, float s, float v) {
		if (a > 1.f) a = 1.f;
		if (h > 360.f) h -= 350.f;
		if (s > 1.f) s = 1.f;
		if (v > 1.f) v = 1.f;

		if(s == 0f) {
			// achromatic (grey)
			return ColorUtil.rgbToInt(a, v, v, v);
		}
		h /= 60f;
		int i = (int)(Math.floor(h));
		float f = h-i;          // factorial part of h
		float p = v * ( 1f - s );
		float q = v * ( 1f - s * f );
		float t = v * ( 1f - s * ( 1f - f ) );

		switch (i) {
			case 0: return ColorUtil.rgbToInt(a, v, t, p);
			case 1: return ColorUtil.rgbToInt(a, q, v, p);
			case 2: return ColorUtil.rgbToInt(a, p, v, t);
			case 3: return ColorUtil.rgbToInt(a, p, q, v);
			case 4: return ColorUtil.rgbToInt(a, t, p, v);
			default: return ColorUtil.rgbToInt(a, v, p, q);  // case 5
		}
}

	public static String toString(int color)
	{
		return "A:"+Integer.toHexString(getAlpha(color)) + ",R:" +
				Integer.toHexString(getRed(color)) + ",G:" +
				Integer.toHexString(getGreen(color)) + ",B:" +
				Integer.toHexString(getBlue(color));
	}
}
