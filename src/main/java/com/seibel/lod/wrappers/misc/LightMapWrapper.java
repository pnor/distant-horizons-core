package com.seibel.lod.wrappers.misc;

import net.minecraft.client.renderer.texture.NativeImage;

/**
 * 
 * @author Leonardo Amato
 * @version 11-13-2021
 */
public class LightMapWrapper
{
	static NativeImage lightMap = null;
	
	public static void setLightMap(NativeImage newlightMap)
	{
		lightMap = newlightMap;
	}
	
	public static int getLightValue(int skyLight, int blockLight)
	{
		return lightMap.getPixelRGBA(skyLight, blockLight);
	}
}
