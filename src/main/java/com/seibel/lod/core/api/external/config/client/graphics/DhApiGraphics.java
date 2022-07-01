package com.seibel.lod.core.api.external.config.client.graphics;

import com.seibel.lod.core.api.external.apiObjects.enums.EDhApiFogDistance;
import com.seibel.lod.core.api.external.apiObjects.enums.EDhApiRendererType;
import com.seibel.lod.core.api.external.apiObjects.enums.EDhApiVanillaOverdraw;
import com.seibel.lod.core.api.external.apiObjects.wrapperInterfaces.IDhApiConfig;
import com.seibel.lod.core.api.implementation.objects.GenericEnumConverter;
import com.seibel.lod.core.api.implementation.wrappers.DhApiConfig;
import com.seibel.lod.core.config.Config;
import com.seibel.lod.core.enums.config.EVanillaOverdraw;
import com.seibel.lod.core.enums.rendering.EFogDistance;
import com.seibel.lod.core.enums.rendering.ERendererType;
import com.seibel.lod.core.config.Config.Client.Graphics.Quality;
import com.seibel.lod.core.config.Config.Client.Advanced.Debugging;
import com.seibel.lod.core.config.Config.Client.Graphics.AdvancedGraphics;

/**
 * General graphics settings.
 *
 * @author James Seibel
 * @version 2022-6-13
 */
public class DhApiGraphics
{
	
	//========================//
	// basic graphic settings //
	//========================//
	
	/**
	 * Returns the config related to Distant Horizons render distance. <br>
	 * The distance is a radius in measured in chunks.
	 */
	public static IDhApiConfig<Integer> getChunkRenderDistanceConfig()
	{ return new DhApiConfig<>(Quality.lodChunkRenderDistance); }
	
	/** Returns the config related to how Distant Horizons is set to render. */
	public static IDhApiConfig<EDhApiRendererType> getRenderingTypeConfig()
	{ return new DhApiConfig<>(Debugging.rendererType, new GenericEnumConverter<>(ERendererType.class, EDhApiRendererType.class)); }
	
	
	//===========================//
	// advanced graphic settings //
	//===========================//
	
	/**
	 * Returns the config related to whether Distant Horizons
	 * should disable its directional culling.
	 */
	public static IDhApiConfig<Boolean> getDisableDirectionalCullingConfig()
	{ return new DhApiConfig<>(AdvancedGraphics.disableDirectionalCulling); }
	
	/**
	 * Returns the config related to how Distant Horizons will
	 * overdraw underneath vanilla Minecraft chunks.
	 */
	public static IDhApiConfig<EDhApiVanillaOverdraw> getVanillaOverdrawConfig()
	{ return new DhApiConfig<>(AdvancedGraphics.vanillaOverdraw, new GenericEnumConverter<>(EVanillaOverdraw.class, EDhApiVanillaOverdraw.class)); }
	
	/**
	 * Returns the config related to how near (or far) Distant Horizons
	 * will overdraw if it can't determine which Minecraft chunks are being rendered.
	 */
	public static IDhApiConfig<Integer> getVanillaOverdrawOffsetConfig()
	{ return new DhApiConfig<>(AdvancedGraphics.overdrawOffset); }
	
	/**
	 * Returns the config related to whether Distant Horizons
	 * should use an extended near clip plane.
	 */
	public static IDhApiConfig<Boolean> getUseExtendedNearClipPlaneConfig()
	{ return new DhApiConfig<>(AdvancedGraphics.useExtendedNearClipPlane); }
	
	/**
	 * Returns the config related to Distant Horizons'
	 * fake chunk brightness multiplier.
	 */
	public static IDhApiConfig<Double> getBrightnessMultiplierConfig()
	{ return new DhApiConfig<>(AdvancedGraphics.brightnessMultiplier); }
	
	/**
	 * Returns the config related to Distant Horizons'
	 * fake chunk saturation multiplier.
	 */
	public static IDhApiConfig<Double> getSaturationMultiplierConfig()
	{ return new DhApiConfig<>(AdvancedGraphics.saturationMultiplier); }
	
	/**
	 * Returns the config related to whether Distant Horizons
	 * should attempt to cull caves.
	 */
	public static IDhApiConfig<Boolean> getCaveCullingEnabledConfig()
	{ return new DhApiConfig<>(AdvancedGraphics.enableCaveCulling); }
	
	/**
	 * Returns the config related to what height Distant Horizons
	 * will attempt to cull caves.
	 */
	public static IDhApiConfig<Boolean> getCaveCullingHeightConfig()
	{ return new DhApiConfig<>(AdvancedGraphics.caveCullingHeight); }
	
	/**
	 * Returns the config related to Distant Horizons'
	 * earth curvature distance ratio. <br>
	 * (The ratio is relative to Earth's real world curvature)
	 */
	public static IDhApiConfig<Boolean> getEarthCurvatureRatioConfig()
	{ return new DhApiConfig<>(AdvancedGraphics.earthCurveRatio); }
	
	
	
}
