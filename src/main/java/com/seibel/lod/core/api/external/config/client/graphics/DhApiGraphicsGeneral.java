package com.seibel.lod.core.api.external.config.client.graphics;

import com.seibel.lod.core.api.external.apiObjects.wrapperInterfaces.IDhApiConfig_v1;
import com.seibel.lod.core.api.externalImplementation.apiObjects.wrappers.DhApiConfig_v1;
import com.seibel.lod.core.enums.rendering.ERendererType;
import com.seibel.lod.core.config.Config.Client.Graphics.Quality;
import com.seibel.lod.core.config.Config.Client.Advanced.Debugging;

/**
 * General graphics settings.
 *
 * @author James Seibel
 * @version 2022-6-9
 */
public class DhApiGraphicsGeneral
{
	
	/**
	 * Returns the config related to Distant Horizons render distance. <br>
	 * The distance is a radius in measured in chunks.
	 */
	public static IDhApiConfig_v1<Integer> getDhChunkRenderDistanceConfig_v1()
	{ return new DhApiConfig_v1<>(Quality.lodChunkRenderDistance); }
	
	/** Returns the config related to how Distant Horizons is set to render. */
	public static IDhApiConfig_v1<ERendererType> getRenderingTypeConfig_v1()
	{ return new DhApiConfig_v1<>(Debugging.rendererType); }
	
}
