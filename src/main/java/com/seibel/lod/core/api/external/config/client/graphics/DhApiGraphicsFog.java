/*
 *    This file is part of the Distant Horizons mod (formerly the LOD Mod),
 *    licensed under the GNU LGPL v3 License.
 *
 *    Copyright (C) 2020-2022  James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.seibel.lod.core.api.external.config.client.graphics;

import com.seibel.lod.core.api.external.apiObjects.enums.*;
import com.seibel.lod.core.api.external.apiObjects.wrapperInterfaces.IDhApiConfig;
import com.seibel.lod.core.api.implementation.objects.GenericEnumConverter;
import com.seibel.lod.core.api.implementation.wrappers.DhApiConfig;
import com.seibel.lod.core.enums.rendering.*;
import com.seibel.lod.core.config.Config.Client.Graphics.FogQuality;

/**
 * Distant Horizons fog configuration. <br><br>
 *
 * Note: unless an option explicitly states that it modifies
 * Minecraft's vanilla rendering (like DisableVanillaFog)
 * these settings will only affect Distant horizons' fog.
 *
 * @author James Seibel
 * @version 2022-7-4
 */
public class DhApiGraphicsFog
{
	//====================//
	// basic fog settings //
	//====================//
	
	/** Returns the config related to when fog is rendered. */
	public static IDhApiConfig<EDhApiFogDistance> getFogDistanceConfig()
	{ return new DhApiConfig<>(FogQuality.fogDistance, new GenericEnumConverter<>(EFogDistance.class, EDhApiFogDistance.class)); }
	
	/** Returns the config related to when fog is rendered. */
	public static IDhApiConfig<EDhApiFogDrawMode> getFogRenderConfig()
	{ return new DhApiConfig<>(FogQuality.fogDrawMode, new GenericEnumConverter<>(EFogDrawMode.class, EDhApiFogDrawMode.class)); }
	
	/** Returns the config related to the fog draw type. */
	public static IDhApiConfig<EDhApiFogColorMode> getFogColorConfig()
	{ return new DhApiConfig<>(FogQuality.fogColorMode, new GenericEnumConverter<>(EFogColorMode.class, EDhApiFogColorMode.class)); }
	
	/** Returns the config related to disabling vanilla fog. */
	public static IDhApiConfig<Boolean> getDisableVanillaFogConfig()
	{ return new DhApiConfig<>(FogQuality.disableVanillaFog); }
	
	
	//=======================//
	// advanced fog settings //
	//=======================//
	
	/** Returns the config related to the fog starting distance. */
	public static IDhApiConfig<Double> getFogStartDistanceConfig()
	{ return new DhApiConfig<>(FogQuality.AdvancedFog.farFogStart); }
	
	/** Returns the config related to the fog ending distance. */
	public static IDhApiConfig<Double> getFogEndDistanceConfig()
	{ return new DhApiConfig<>(FogQuality.AdvancedFog.farFogEnd); }
	
	/**
	 * Returns the config related to the fog minimum thickness
	 * (aka how opaque the fog's is at its thinnest point).
	 */
	public static IDhApiConfig<Double> getFogMinThicknessConfig()
	{ return new DhApiConfig<>(FogQuality.AdvancedFog.farFogMin); }
	
	/**
	 * Returns the config related to the fog maximum thickness
	 * (aka how opaque the fog's is at its thickest point).
	 */
	public static IDhApiConfig<Double> getFogMaxThicknessConfig()
	{ return new DhApiConfig<>(FogQuality.AdvancedFog.farFogMax); }
	
	/**
	 * Returns the config related to how the fog increases/decreases
	 * in thickness over the given start and end distance.
	 */
	public static IDhApiConfig<EDhApiFogFalloff> getFogFalloffConfig()
	{ return new DhApiConfig<>(FogQuality.AdvancedFog.farFogType, new GenericEnumConverter<>(EFogFalloff.class, EDhApiFogFalloff.class)); }
	
	/** Returns the config related to the fog density. */
	public static IDhApiConfig<Double> getFogDensityFunctionConfig()
	{ return new DhApiConfig<>(FogQuality.AdvancedFog.farFogDensity); }
	
	
	//=====================//
	// height fog settings //
	//=====================//
	
	/** Returns the config related to how the height fog mixes. */
	public static IDhApiConfig<EDhApiHeightFogMixMode> getHeightFogMixModeConfig()
	{ return new DhApiConfig<>(FogQuality.AdvancedFog.HeightFog.heightFogMixMode, new GenericEnumConverter<>(EHeightFogMixMode.class, EDhApiHeightFogMixMode.class)); }
	
	/**
	 * Returns the config related to how the height fog
	 * is drawn relative to the camera or world.
	 */
	public static IDhApiConfig<EDhApiHeightFogMode> getHeightFogModeConfig()
	{ return new DhApiConfig<>(FogQuality.AdvancedFog.HeightFog.heightFogMode, new GenericEnumConverter<>(EHeightFogMode.class, EDhApiHeightFogMode.class)); }
	
	/**
	 * Returns the config related to the height fog's base height.
	 * (This defines the height used by EDhApiHeightFogMode if
	 * it is set up to use a specific height)
	 */
	public static IDhApiConfig<Double> getHeightFogBaseHeightConfig()
	{ return new DhApiConfig<>(FogQuality.AdvancedFog.HeightFog.heightFogHeight); }
	
	/**
	 * Returns the config related to the height fog's
	 * starting height as a percent of the world height.
	 */
	public static IDhApiConfig<Double> getHeightFogStartingHeightPercentConfig()
	{ return new DhApiConfig<>(FogQuality.AdvancedFog.HeightFog.heightFogStart); }
	
	/**
	 * Returns the config related to the height fog's
	 * ending height as a percent of the world height.
	 */
	public static IDhApiConfig<Double> getHeightFogEndingHeightPercentConfig()
	{ return new DhApiConfig<>(FogQuality.AdvancedFog.HeightFog.heightFogEnd); }
	
	/**
	 * Returns the config related to the height fog's minimum thickness
	 * (aka how opaque the height fog is at its thinnest point).
	 */
	public static IDhApiConfig<Double> getHeightFogMinThicknessConfig()
	{ return new DhApiConfig<>(FogQuality.AdvancedFog.HeightFog.heightFogMin); }
	
	/**
	 * Returns the config related to the height fog's maximum thickness
	 * (aka how opaque the height fog is at its thickest point).
	 */
	public static IDhApiConfig<Double> getHeightFogMaxThicknessConfig()
	{ return new DhApiConfig<>(FogQuality.AdvancedFog.HeightFog.heightFogMax); }
	
	/**
	 * Returns the config related to how the height fog increases/decreases
	 * in thickness over the given start and end height.
	 */
	public static IDhApiConfig<EDhApiFogFalloff> getHeightFogFalloffConfig()
	{ return new DhApiConfig<>(FogQuality.AdvancedFog.HeightFog.heightFogType, new GenericEnumConverter<>(EFogFalloff.class, EDhApiFogFalloff.class)); }
	
	/** Returns the config related to the height fog's density. */
	public static IDhApiConfig<Double> getHeightFogDensityConfig()
	{ return new DhApiConfig<>(FogQuality.AdvancedFog.HeightFog.heightFogDensity); }
	
	
}
