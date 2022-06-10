package com.seibel.lod.core.api.external.config.client.graphics;

import com.seibel.lod.core.api.external.apiObjects.objects.DhApiConfig_v1;
import com.seibel.lod.core.api.external.apiObjects.enums.DhApiRendererType;
import com.seibel.lod.core.config.Config.Client.Graphics.Quality;
import com.seibel.lod.core.config.Config.Client.Advanced.Debugging;

/**
 * General graphics settings.
 *
 * @author James Seibel
 * @version 2022-6-2
 */
public class DhApiGraphicsGeneral
{
	
	/**
	 * Returns the config related to Distant Horizons render distance. <br>
	 * The distance is a radius in measured in chunks.
	 */
	public static DhApiConfig_v1<Integer> getDhChunkRenderDistanceConfig_v1()
	{ return new DhApiConfig_v1<>(Quality.lodChunkRenderDistance); }
	
	/** Returns the config related to how Distant Horizons is set to render. */
	public static DhApiConfig_v1<DhApiRendererType> getRenderingTypeConfig_v1()
	{ return new DhApiConfig_v1<>(Debugging.rendererType); }
	
}
