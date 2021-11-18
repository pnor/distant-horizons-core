package com.seibel.lod.core.wrapperAdapters.misc;

import net.minecraft.client.renderer.texture.NativeImage;

/**
 * 
 * @author Leonardo Amato
 * @version 11-13-2021
 */
public interface ILightMapWrapper
{
	public void setLightMap(NativeImage newlightMap);
	
	public int getLightValue(int skyLight, int blockLight);
}
