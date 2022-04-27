package com.seibel.lod.core.api.external.config.client.graphics;

import com.seibel.lod.core.api.external.ExternalApiShared;
import com.seibel.lod.core.enums.rendering.RendererType;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton;

/**
 * General graphics settings.
 *
 * @author James Seibel
 * @version 2022-4-26
 */
public class General
{
	private static final ILodConfigWrapperSingleton.IClient.IGraphics.IQuality qualitySettings = ExternalApiShared.CONFIG.client().graphics().quality();
	
	
	
	public static int getLodChunkRenderDistance_v1()
	{
		return qualitySettings.getLodChunkRenderDistance();
	}
	
	public static boolean getRenderingEnabled_v1()
	{
		return ExternalApiShared.CONFIG.client().advanced().debugging().getRendererType() == RendererType.DEFAULT;
	}
}
