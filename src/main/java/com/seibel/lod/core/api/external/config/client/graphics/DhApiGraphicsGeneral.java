package com.seibel.lod.core.api.external.config.client.graphics;

import com.seibel.lod.core.api.external.ExternalApiShared;
import com.seibel.lod.core.config.Config;
import com.seibel.lod.core.enums.rendering.RendererType;
import com.seibel.lod.core.config.Config.Client.Graphics.Quality;
import com.seibel.lod.core.config.Config.Client.Advanced.Debugging;

/**
 * General graphics settings.
 *
 * @author James Seibel
 * @version 2022-5-28
 */
public class DhApiGraphicsGeneral
{
	
	/** Returns the current Disant Horizons render distance radius in chunks. */
	public static int getLodChunkRenderDistance_v1()
	{
		return Quality.lodChunkRenderDistance.get();
	}
	/** @return true if the value was set, false otherwise. */
	public static boolean setLodChunkRenderDistance_v1(int newValue)
	{
		return ExternalApiShared.attemptToSetApiValue(Quality.lodChunkRenderDistance, newValue);
	}
	
	/**
	 * Returns true if rendering is currently enabled. <br>
	 * Returns false when rendering is disabled or debug rendering is enabled.
	 */
	public static boolean getRenderingEnabled_v1()
	{
		return Config.Client.Advanced.Debugging.rendererType.get() == RendererType.DEFAULT;
	}
	/** @return true if the value was set, false otherwise. */
	public static boolean setRenderingEnabled_v1(boolean enableRendering)
	{
		RendererType newValue = enableRendering ? RendererType.DEFAULT : RendererType.DISABLED;
		return ExternalApiShared.attemptToSetApiValue(Debugging.rendererType, newValue);
	}
	
}
