package com.seibel.lod.core.api.external.config.client.graphics;

import com.seibel.lod.core.api.external.apiObjects.wrapperInterfaces.IDhApiConfig;
import com.seibel.lod.core.api.implementation.wrappers.DhApiConfig;
import com.seibel.lod.core.enums.rendering.EFogColorMode;
import com.seibel.lod.core.enums.rendering.EFogDistance;
import com.seibel.lod.core.config.Config.Client.Graphics.FogQuality;
import com.seibel.lod.core.enums.rendering.EFogDrawMode;

/**
 * Any graphics settings related to fog.
 *
 * @author James Seibel
 * @version 2022-6-13
 */
public class DhApiGraphicsFog
{
	
	/** Returns the config related to when fog is rendered. */
	public static IDhApiConfig<EFogDistance> getFogDistanceConfig()
	{ return new DhApiConfig<>(FogQuality.fogDistance); }
	
	/** Returns the config related to when fog is rendered. */
	public static IDhApiConfig<EFogDrawMode> getFogRenderConfig()
	{ return new DhApiConfig<>(FogQuality.fogDrawMode); }
	
	/** Returns the config related to the fog draw type. */
	public static IDhApiConfig<EFogColorMode> getFogColorConfig()
	{ return new DhApiConfig<>(FogQuality.fogColorMode); }
	
	/** Returns the config related to disabling vanilla fog. */
	public static IDhApiConfig<Boolean> getDisableVanillaFogConfig()
	{ return new DhApiConfig<>(FogQuality.disableVanillaFog); }
	
	
}
