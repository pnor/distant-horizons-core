package com.seibel.lod.api;

import com.seibel.lod.LodMain;

import net.minecraft.util.math.vector.Matrix4f;

public class ClientApi
{
	
	public ClientApi()
	{
		
	}
	
	
	
	
	public void renderLods(Matrix4f mcModelViewMatrix, float partialTicks)
	{
		LodMain.client_proxy.renderLods(mcModelViewMatrix, partialTicks);
	}
	
}
