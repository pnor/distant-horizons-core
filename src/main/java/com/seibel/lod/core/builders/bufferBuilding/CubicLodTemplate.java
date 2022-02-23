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

package com.seibel.lod.core.builders.bufferBuilding;

import com.seibel.lod.core.enums.rendering.DebugMode;
import com.seibel.lod.core.objects.opengl.LodBox;
import com.seibel.lod.core.objects.opengl.LodQuadBuilder;
import com.seibel.lod.core.util.DataPointUtil;
import com.seibel.lod.core.util.LevelPosUtil;
import com.seibel.lod.core.util.LodUtil;

/**
 * Builds LODs as rectangular prisms.
 * 
 * @author James Seibel
 * @version 12-8-2021
 */
public class CubicLodTemplate {

	public static void addLodToBuffer(long data, long topData, long botData, long[][][] adjData, byte detailLevel,
			int offsetPosX, int offsetOosZ, LodQuadBuilder quadBuilder, DebugMode debugging) {
		short width = (short) (1 << detailLevel);
		short x = (short) LevelPosUtil.convert(detailLevel, offsetPosX, LodUtil.BLOCK_DETAIL_LEVEL);
		short y = DataPointUtil.getDepth(data);
		short z = (short) LevelPosUtil.convert(detailLevel, offsetOosZ, LodUtil.BLOCK_DETAIL_LEVEL);
		short dy = (short) (DataPointUtil.getHeight(data) - y);
		if (dy == 0)
			return;

		int color;
		if (debugging != DebugMode.OFF && debugging != DebugMode.SHOW_WIREFRAME) {
			if (debugging == DebugMode.SHOW_DETAIL || debugging == DebugMode.SHOW_DETAIL_WIREFRAME)
				color = LodUtil.DEBUG_DETAIL_LEVEL_COLORS[detailLevel].getRGB();
			else /// if (debugging == DebugMode.SHOW_GENMODE || debugging ==
					/// DebugMode.SHOW_GENMODE_WIREFRAME)
				color = LodUtil.DEBUG_DETAIL_LEVEL_COLORS[DataPointUtil.getGenerationMode(data)].getRGB();
		} else
			color = DataPointUtil.getColor(data);

		LodBox.addBoxQuadsToBuilder(quadBuilder, // buffer
				width, dy, width, // setWidth
				x, y, z, // setOffset
				color, // setColor
				DataPointUtil.getLightSky(data), DataPointUtil.getLightBlock(data), // setLights
				topData, botData, adjData); // setAdjData
	}
}
