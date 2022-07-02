package com.seibel.lod.core.api.external.config.client.graphics;

import com.seibel.lod.core.api.external.apiObjects.enums.*;
import com.seibel.lod.core.api.external.apiObjects.wrapperInterfaces.IDhApiConfig;
import com.seibel.lod.core.api.implementation.objects.GenericEnumConverter;
import com.seibel.lod.core.api.implementation.wrappers.DhApiConfig;
import com.seibel.lod.core.config.Config;
import com.seibel.lod.core.enums.config.*;
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
	
	
	
	//==================//
	// graphic settings //
	//==================//
	
	/**
	 * Returns the config related to what the maximum detail level
	 * Distant Horizons' fake chunks should be rendered at.
	 */
	public static IDhApiConfig<EDhApiHorizontalQuality> getMaxDetailLevelConfig()
	{ return new DhApiConfig<>(Quality.drawResolution, new GenericEnumConverter<>(EHorizontalResolution.class, EDhApiHorizontalQuality.class)); }
	
	/**
	 * Returns the config related to how detailed Distant Horizons fake chunks
	 * should be on the vertical axis.
	 */
	public static IDhApiConfig<EDhApiVerticalQuality> getVerticalQualityConfig()
	{ return new DhApiConfig<>(Quality.verticalQuality, new GenericEnumConverter<>(EVerticalQuality.class, EDhApiVerticalQuality.class)); }
	
	/**
	 * Returns the config related to how quickly Distant Horizons fake chunks
	 * drop in quality as they get farther away from the player.
	 */
	public static IDhApiConfig<EDhApiHorizontalQuality> getHorizontalQualityDropoffConfig()
	{ return new DhApiConfig<>(Quality.horizontalQuality, new GenericEnumConverter<>(EHorizontalQuality.class, EDhApiHorizontalQuality.class)); }
	
	/**
	 * Returns the config related to how quickly Distant Horizons fake chunks
	 * drop in quality as they get farther away from the player.
	 */
	public static IDhApiConfig<EDhApiDropoffQuality> getHorizontalQualityDropoffMethodConfig()
	{ return new DhApiConfig<>(Quality.dropoffQuality, new GenericEnumConverter<>(EDropoffQuality.class, EDhApiDropoffQuality.class)); }
	
	/**
	 * Returns the config related to how smooth Distant Horizons fake chunk
	 * biome blending should be generated.
	 */
	public static IDhApiConfig<Integer> getBiomeBlendingConfig()
	{ return new DhApiConfig<>(Quality.lodBiomeBlending); }
	
	
	
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
