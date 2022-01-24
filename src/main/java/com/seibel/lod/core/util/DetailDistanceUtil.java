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
import com.seibel.lod.core.enums.config.HorizontalResolution;
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
	private static final int maxDetail = LodUtil.REGION_DETAIL_LEVEL + 1;
	private static final int minDistance = 0;
	private static int minDetailDistance = (int) (MC_RENDER.getRenderDistance()*16 * 1.42f);
	private static int maxDistance = CONFIG.client().graphics().quality().getLodChunkRenderDistance() * 16 * 2;
	
	
	public static void updateSettings()
	{
		minDetailDistance = (int) (MC_RENDER.getRenderDistance()*16 * 1.42f);
		minGenDetail = CONFIG.client().graphics().quality().getDrawResolution().detailLevel;
		minDrawDetail = (byte) Math.max(CONFIG.client().graphics().quality().getDrawResolution().detailLevel, CONFIG.client().graphics().quality().getDrawResolution().detailLevel);
		maxDistance = CONFIG.client().graphics().quality().getLodChunkRenderDistance() * 16 * 8;
	}
	
	public static int baseDistanceFunction(int detail)
	{
		if (detail <= minGenDetail)
			return minDistance;
		if (detail >= maxDetail)
			return maxDistance;
		
		if (CONFIG.client().graphics().advancedGraphics().getAlwaysDrawAtMaxQuality())
			return detail * 0x10000; //if you want more you are doing wrong
		
		int distanceUnit = CONFIG.client().graphics().quality().getHorizontalScale() * 16;
		if (CONFIG.client().graphics().quality().getHorizontalQuality() == HorizontalQuality.LOWEST)
			return (detail * distanceUnit);
		else
		{
			double base = CONFIG.client().graphics().quality().getHorizontalQuality().quadraticBase;
			return (int) (Math.pow(base, detail) * distanceUnit);
		}
	}
	
	public static int getDrawDistanceFromDetail(int detail)
	{
		return baseDistanceFunction(detail);
	}
	
	public static byte baseInverseFunction(int distance, byte minDetail)
	{
		byte detail;
		distance -= minDetailDistance;
		
		if (distance < 0 || CONFIG.client().graphics().advancedGraphics().getAlwaysDrawAtMaxQuality())
			distance = 0;
		int distanceUnit = CONFIG.client().graphics().quality().getHorizontalScale() * 16;
		double scaledDistance = distance;
		scaledDistance /= distanceUnit;
		if (CONFIG.client().graphics().quality().getHorizontalQuality() == HorizontalQuality.LOWEST)
			detail = (byte) (scaledDistance);
		else
		{
			double base = CONFIG.client().graphics().quality().getHorizontalQuality().quadraticBase;
			double logBase = Math.log(base);
			detail = (byte) (Math.log(scaledDistance) / logBase);
		}
		return (byte) LodUtil.clamp(minDetail, detail+minDetail, maxDetail - 1);
	}

	public static byte getDetailLevelFromDistance(int distance)
	{
		return baseInverseFunction(distance, minDrawDetail);
	}

	@Deprecated //Reason: All merged into `getDetailLevelFromDistance`
	public static byte getDrawDetailFromDistance(int distance)
	{
		return baseInverseFunction(distance, minDrawDetail);
	}

	@Deprecated //Reason: Same as 'getDrawDetailFromDistance'
	public static byte getGenerationDetailFromDistance(int distance)
	{
		return baseInverseFunction((int) (distance * genMultiplier), minGenDetail);
	}

	@Deprecated //Reason: Same as 'getDrawDetailFromDistance'
	public static byte getTreeCutDetailFromDistance(int distance)
	{
		return baseInverseFunction((int) (distance * treeCutMultiplier), minGenDetail);
	}

	@Deprecated //Reason: Same as 'getDrawDetailFromDistance'
	public static byte getTreeGenDetailFromDistance(int distance)
	{
		return baseInverseFunction((int) (distance * treeGenMultiplier), minGenDetail);
	}
	

	// NOTE: The recent LodWorldGenerator changes assumes that this value doesn't change with 'detail'.
	// If this is changed, LodWorldGenerator needs to be fixed!
	/*
	public static DistanceGenerationMode getDistanceGenerationMode(int detail)
	{
		return CONFIG.client().worldGenerator().getDistanceGenerationMode();
	}*/
	
	@Deprecated
	public static byte getLodDrawDetail(byte detail)
	{
		detail += minDrawDetail;
		if (detail > 10)
			detail = 10;
		return detail;
	}
	
	public static int getMaxVerticalData(int detail)
	{
		return CONFIG.client().graphics().quality().getVerticalQuality().maxVerticalData[LodUtil.clamp(minGenDetail, detail, LodUtil.REGION_DETAIL_LEVEL)];
	}
	
}
