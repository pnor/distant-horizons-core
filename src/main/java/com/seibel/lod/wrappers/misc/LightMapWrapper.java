package com.seibel.lod.wrappers.misc;

import com.seibel.lod.core.wrapperAdapters.misc.ILightMapWrapper;

import net.minecraft.client.renderer.texture.NativeImage;

/**
 * 
 * @author Leonardo Amato
 * @version 11-13-2021
 */
public class LightMapWrapper implements ILightMapWrapper
{
	private NativeImage lightMap = null;
	
	public LightMapWrapper(NativeImage newlightMap)
	{
		lightMap = newlightMap;
	}
	
	@Override
	public void setLightMap(NativeImage newlightMap)
	{
		lightMap = newlightMap;
	}
	
	@Override
	public int getLightValue(int skyLight, int blockLight)
	{
		return lightMap.getPixelRGBA(skyLight, blockLight);
	}
}
