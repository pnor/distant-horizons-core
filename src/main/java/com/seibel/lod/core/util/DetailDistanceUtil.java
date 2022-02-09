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

package com.seibel.lod.core.util;

import com.seibel.lod.core.enums.config.HorizontalQuality;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftRenderWrapper;

/**
 * 
 * @author Leonardo Amato
 * @version ??
 */
public class DetailDistanceUtil
{
	private static final ILodConfigWrapperSingleton CONFIG = SingletonHandler.get(ILodConfigWrapperSingleton.class);
	private static final IMinecraftRenderWrapper MC_RENDER = SingletonHandler.get(IMinecraftRenderWrapper.class);
	
	@Deprecated
	private static final double genMultiplier = 1.0;
	@Deprecated
	private static final double treeGenMultiplier = 1.0;
	@Deprecated
	private static final double treeCutMultiplier = 1.0;
	private static byte minGenDetail = CONFIG.client().graphics().quality().getDrawResolution().detailLevel;
	private static byte minDrawDetail = CONFIG.client().graphics().quality().getDrawResolution().detailLevel;
	private static final byte maxDetail = LodUtil.DETAIL_OPTIONS;
	private static final double minDistance = 0;
	private static double distanceUnit = 16 * CONFIG.client().graphics().quality().getHorizontalScale();
	private static double minGenDetailDistance = (int) (MC_RENDER.getRenderDistance()*16 * 1.42f);
	private static double maxDistance = CONFIG.client().graphics().quality().getLodChunkRenderDistance() * 16 * 2;
	private static double logBase = Math.log(CONFIG.client().graphics().quality().getHorizontalQuality().quadraticBase);
	
	
	public static void updateSettings()
	{
		distanceUnit = 16 * CONFIG.client().graphics().quality().getHorizontalScale();
		minGenDetailDistance = (int) (MC_RENDER.getRenderDistance()*16 * 1.42f);
		minGenDetail = CONFIG.client().graphics().quality().getDrawResolution().detailLevel;
		minDrawDetail = (byte) Math.max(CONFIG.client().graphics().quality().getDrawResolution().detailLevel, CONFIG.client().graphics().quality().getDrawResolution().detailLevel);
		maxDistance = CONFIG.client().graphics().quality().getLodChunkRenderDistance() * 16 * 8;
		logBase = Math.log(CONFIG.client().graphics().quality().getHorizontalQuality().quadraticBase);
	}
	
	public static double baseDistanceFunction(int detail)
	{
		if (detail <= minGenDetail)
			return minDistance;
		if (detail >= maxDetail)
			return maxDistance;
		
		if (CONFIG.client().graphics().advancedGraphics().getAlwaysDrawAtMaxQuality())
			return detail * 0x10000; //if you want more you are doing wrong
		
		if (CONFIG.client().graphics().quality().getHorizontalQuality() == HorizontalQuality.LOWEST)
			return ((double)detail * distanceUnit);
		else
		{
			double base = CONFIG.client().graphics().quality().getHorizontalQuality().quadraticBase;
			return Math.pow(base, detail) * distanceUnit;
		}
	}
	
	public static double getDrawDistanceFromDetail(int detail)
	{
		return baseDistanceFunction(detail);
	}
	
	public static byte baseInverseFunction(double distance)
	{
		double maxDetailDistance = getDrawDistanceFromDetail(maxDetail-1);
		if (distance > maxDetailDistance) {
			//ClientApi.LOGGER.info("DEBUG: Scale as max: {}", distance);
			return maxDetail-1;
		}
		
		int detail;
		
		if (CONFIG.client().graphics().quality().getHorizontalQuality() == HorizontalQuality.LOWEST)
			detail = (int) (distance/distanceUnit);
		else
			detail = (int) (Math.log(distance/distanceUnit) / logBase);
		
		return (byte) LodUtil.clamp(minDrawDetail, detail+minDrawDetail, maxDetail - 1);
	}
	
	public static byte getDetailLevelFromDistance(double distance)
	{
		return baseInverseFunction(distance);
	}
	
	public static byte getGenDetailLevelFromDistance(double distance)
	{
		if(distance < minGenDetailDistance)
			return minGenDetail;
		return baseInverseFunction(distance);
	}
	
	
	
	
	// NOTE: The recent LodWorldGenerator changes assumes that this value doesn't change with 'detail'.
	// If this is changed, LodWorldGenerator needs to be fixed!
	/*
	public static DistanceGenerationMode getDistanceGenerationMode(int detail)
	{
		return CONFIG.client().worldGenerator().getDistanceGenerationMode();
	}*/
	
	public static int getMaxVerticalData(int detail)
	{
		return CONFIG.client().graphics().quality().getVerticalQuality().maxVerticalData[LodUtil.clamp(minGenDetail, detail, LodUtil.REGION_DETAIL_LEVEL)];
	}
	
}
