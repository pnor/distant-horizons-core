package com.seibel.lod.core.objects.lod;

public class DataPoint
{
	public int color;
	//	|a	|a	|a	|a	|a	|a	|a	|a	|
	//	|r	|r	|r	|r	|r	|r	|r	|r	|
	//	|g	|g	|g	|g	|g	|g	|g	|g	|
	//	|b	|b	|b	|b	|b	|b	|b	|b	|
	public int data;
	//	|h	|h	|h	|h	|h	|h	|h	|h	|
	//	|h	|h	|h	|h	|d	|d	|d	|d	|
	//	|d	|d	|d	|d	|d	|d	|d	|d	|
	//	|bl	|bl	|bl	|bl	|sl	|sl	|sl	|sl	|
	public byte flags;
	//	|l	|l	|f	|g	|g	|g	|v	|e	|
	
	
	public DataPoint()
	{
		color = 0;
		data = 0;
		flags = 0;
	}
	
	public DataPoint(int newColor, int newData, byte newFlags)
	{
		color = newColor;
		data = newData;
		flags = newFlags;
	}
	
	public void clear()
	{
		color = 0;
		data = 0;
		flags = 0;
	}
}
