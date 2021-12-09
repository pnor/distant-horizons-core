package com.seibel.lod.core.dataFormat;

public class LightFormat
{
	public final static byte INT_BLOCK_LIGHT_SHIFT = 16;
	public final static byte INT_SKY_LIGHT_SHIFT = 0;
	
	public final static byte BYTE_BLOCK_LIGHT_SHIFT = 4;
	public final static byte BYTE_SKY_LIGHT_SHIFT = 0;
	
	public final static byte BLOCK_LIGHT_MASK = 0b1111;
	public final static byte SKY_LIGHT_MASK = 0b1111;
	
	
	public byte formatLightAsByte(byte skyLight, byte blockLight)
	{
		return (byte) (((skyLight & SKY_LIGHT_MASK) << BYTE_SKY_LIGHT_SHIFT) | ((blockLight & BLOCK_LIGHT_MASK) << BYTE_BLOCK_LIGHT_SHIFT));
	}
	
	public int formatLightAsInt(byte skyLight, byte blockLight)
	{
		return ((skyLight & SKY_LIGHT_MASK) << INT_SKY_LIGHT_SHIFT) | ((blockLight & BLOCK_LIGHT_MASK) << INT_BLOCK_LIGHT_SHIFT);
	}
	
	public int convertByteToIntFormat(byte lights)
	{
		return 0;
	}
	
	public byte getSkyLight(byte lights)
	{
		return (byte) ((lights >>> BYTE_SKY_LIGHT_SHIFT) & SKY_LIGHT_MASK);
	}
	
	public byte getBlockLight(byte lights)
	{
		return (byte) ((lights >>> BYTE_BLOCK_LIGHT_SHIFT) & BLOCK_LIGHT_MASK);
	}
}
