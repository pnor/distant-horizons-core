/*
 *    This file is part of the Distant Horizon mod (formerly the LOD Mod),
 *    licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2020  James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.seibel.lod.core.render;

import com.seibel.lod.core.enums.rendering.FogDistance;
import com.seibel.lod.core.enums.rendering.FogDrawMode;
import com.seibel.lod.core.handlers.IReflectionHandler;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton;

/**
 * This object is just a replacement for an array
 * to make things easier to understand in the LodRenderer.
 * 
 * @author James Seibel
 * @version 11-26-2021
 */
public class LodFogConfig
{
	public FogDrawMode fogDrawMode;
	public FogDistance fogDistance;
	
	public float nearFogStart = 0;
	public float nearFogEnd = 0;
	
	public float farFogStart = 0;
	public float farFogEnd = 0;
	
	public LodFogConfig(ILodConfigWrapperSingleton config, IReflectionHandler reflectionHandler, int farPlaneBlockDistance, int vanillaBlockRenderedDistance) {
		
		fogDrawMode = config.client().graphics().fogQuality().getFogDrawMode();
		if (fogDrawMode == FogDrawMode.USE_OPTIFINE_SETTING)
			fogDrawMode = reflectionHandler.getFogDrawMode();
		
		// how different distances are drawn depends on the quality set
		fogDistance = config.client().graphics().fogQuality().getFogDistance();
		
		// far fog //
		
		if (config.client().graphics().fogQuality().getFogDistance() == FogDistance.NEAR_AND_FAR)
			farFogStart = farPlaneBlockDistance * 0.9f;
		else
			// for more realistic fog when using FAR
			farFogStart = Math.min(vanillaBlockRenderedDistance * 1.5f, farPlaneBlockDistance * 0.9f);
		
		farFogEnd = farPlaneBlockDistance;
	
		
		// near fog //
		
		// the reason that I wrote fogEnd then fogStart backwards
		// is because we are using fog backwards to how
		// it is normally used, hiding near objects
		// instead of far objects.
		nearFogEnd = vanillaBlockRenderedDistance * 1.41f;
		nearFogStart = vanillaBlockRenderedDistance * 1.6f;
	}
	
}
