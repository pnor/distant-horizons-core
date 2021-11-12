package com.seibel.lod.api;

import com.seibel.lod.LodMain;
import com.seibel.lod.objects.rending.Mat4f;

/**
 * TODO
 * 
 * @author James Seibel
 * @version 11-11-2021
 */
public class ClientApi
{
	
	public ClientApi()
	{
		
	}
	
	
	
	
	public static void renderLods(Mat4f mcModelViewMatrix, float partialTicks)
	{
		LodMain.client_proxy.renderLods(mcModelViewMatrix, partialTicks);
	}
	
	
	
	
}
