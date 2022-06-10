package com.seibel.lod.core.api.external.config.client.graphics;

import com.seibel.lod.core.enums.rendering.FogColorMode;
import com.seibel.lod.core.api.external.apiObjects.enums.DhApiFogDistance;
import com.seibel.lod.core.api.external.apiObjects.objects.DhApiConfig_v1;
import com.seibel.lod.core.config.Config.Client.Graphics.FogQuality;
import com.seibel.lod.core.api.external.apiObjects.enums.DhApiFogDrawMode;

/**
 * Any graphics settings related to fog.
 *
 * @author James Seibel
 * @version 2022-6-2
 */
public class DhApiGraphicsFog
{
	
	/** Returns the config related to when fog is rendered. */
	public static DhApiConfig_v1<DhApiFogDistance> getFogDistanceConfig_v1()
	{ return new DhApiConfig_v1<>(FogQuality.fogDistance); }
	
	/** Returns the config related to when fog is rendered. */
	public static DhApiConfig_v1<DhApiFogDrawMode> getFogRenderConfig_v1()
	{ return new DhApiConfig_v1<>(FogQuality.fogDrawMode); }
	
	/** Returns the config related to the fog draw type. */
	public static DhApiConfig_v1<FogColorMode> getFogColorConfig_v1()
	{ return new DhApiConfig_v1<>(FogQuality.fogColorMode); }
	
	/** Returns the config related to disabling vanilla fog. */
	public static DhApiConfig_v1<Boolean> getDisableVanillaFogConfig_v1()
	{ return new DhApiConfig_v1<>(FogQuality.disableVanillaFog); }
	
	
}
