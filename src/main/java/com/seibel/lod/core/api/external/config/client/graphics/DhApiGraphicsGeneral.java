package com.seibel.lod.core.api.external.config.client.graphics;

import com.seibel.lod.core.api.external.apiObjects.wrapperInterfaces.IDhApiConfig;
import com.seibel.lod.core.api.implementation.wrappers.DhApiConfig;
import com.seibel.lod.core.enums.rendering.ERendererType;
import com.seibel.lod.core.config.Config.Client.Graphics.Quality;
import com.seibel.lod.core.config.Config.Client.Advanced.Debugging;

/**
 * General graphics settings.
 *
 * @author James Seibel
 * @version 2022-6-13
 */
public class DhApiGraphicsGeneral
{
	
	/**
	 * Returns the config related to Distant Horizons render distance. <br>
	 * The distance is a radius in measured in chunks.
	 */
	public static IDhApiConfig<Integer> getDhChunkRenderDistanceConfig()
	{ return new DhApiConfig<>(Quality.lodChunkRenderDistance); }
	
	/** Returns the config related to how Distant Horizons is set to render. */
	public static IDhApiConfig<ERendererType> getRenderingTypeConfig()
	{ return new DhApiConfig<>(Debugging.rendererType); }
	
}
