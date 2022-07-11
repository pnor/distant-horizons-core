/*
 *    This file is part of the Distant Horizons mod (formerly the LOD Mod),
 *    licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2020-2022  James Seibel
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

package com.seibel.lod.core.builders.lodBuilding.bufferBuilding;

import com.seibel.lod.core.api.ClientApi;
import com.seibel.lod.core.enums.rendering.DebugMode;
import com.seibel.lod.core.handlers.dependencyInjection.SingletonHandler;
import com.seibel.lod.core.objects.opengl.LodBox;
import com.seibel.lod.core.util.ColorUtil;
import com.seibel.lod.core.util.DataPointUtil;
import com.seibel.lod.core.util.LevelPosUtil;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton;

import org.apache.logging.log4j.Level;

/**
 * Builds LODs as rectangular prisms.
 *
 * @author James Seibel
 * @version 3-19-2022
 */
public class CubicLodTemplate {
	private static final ILodConfigWrapperSingleton CONFIG = SingletonHandler.get(ILodConfigWrapperSingleton.class);

	public static void addLodToBuffer(long data, long topData, long botData, long[][][] adjData, boolean[] adjFillBlack,
			byte detailLevel, int offsetPosX, int offsetOosZ, LodQuadBuilder quadBuilder, DebugMode debugging) {
		short width = (short) (1 << detailLevel);
		short x = (short) LevelPosUtil.convert(detailLevel, offsetPosX, LodUtil.BLOCK_DETAIL_LEVEL);
		short y = DataPointUtil.getDepth(data);
		short z = (short) LevelPosUtil.convert(detailLevel, offsetOosZ, LodUtil.BLOCK_DETAIL_LEVEL);
		short dy = (short) (DataPointUtil.getHeight(data) - y);
		if (dy == 0)
			return;
		if (dy < 0) {
			// TODO a hack; didn't fix at all
			// what is up with the height?
			return;
			// throw new IllegalArgumentException("Negative y size for the data! Data: " +
			// DataPointUtil.toString(data));
		}

		int color;
		boolean fullBright = false;
		switch (debugging) {
		case OFF:
		case SHOW_WIREFRAME: {
			float saturationMultiplier = (float) CONFIG.client().graphics().advancedGraphics()
					.getSaturationMultiplier();
			float brightnessMultiplier = (float) CONFIG.client().graphics().advancedGraphics()
					.getBrightnessMultiplier();
			if (saturationMultiplier == 1.0 && brightnessMultiplier == 1.0) {
				color = DataPointUtil.getColor(data);
			} else {
				float[] ahsv = ColorUtil.argbToAhsv(DataPointUtil.getColor(data));
				color = ColorUtil.ahsvToArgb(ahsv[0], ahsv[1], ahsv[2] * saturationMultiplier,
						ahsv[3] * brightnessMultiplier);
				// ApiShared.LOGGER.info("Raw color:[{}], AHSV:{}, Out color:[{}]",
				// ColorUtil.toString(DataPointUtil.getColor(data)),
				// ahsv, ColorUtil.toString(color));
			}
			break;
		}
		case SHOW_DETAIL:
		case SHOW_DETAIL_WIREFRAME: {
			color = LodUtil.DEBUG_DETAIL_LEVEL_COLORS[detailLevel];
			fullBright = true;
			break;
		}
		case SHOW_GENMODE:
		case SHOW_GENMODE_WIREFRAME: {
			color = LodUtil.DEBUG_DETAIL_LEVEL_COLORS[DataPointUtil.getGenerationMode(data)];
			fullBright = true;
			break;
		}
		case SHOW_OVERLAPPING_QUADS:
		case SHOW_OVERLAPPING_QUADS_WIREFRAME: {
			color = ColorUtil.WHITE;
			fullBright = true;
			break;
		}
		default:
			throw new IllegalArgumentException("Unknown debug mode: " + debugging);
		}
		LodBox.addBoxQuadsToBuilder(quadBuilder, // buffer
				width, dy, width, // setWidth
				x, y, z, // setOffset
				color, // setColor
				DataPointUtil.getLightSky(data), // setSkyLights
				fullBright ? 15 : DataPointUtil.getLightBlock(data), // setBlockLights
				topData, botData, adjData, adjFillBlack); // setAdjData
	}
}
