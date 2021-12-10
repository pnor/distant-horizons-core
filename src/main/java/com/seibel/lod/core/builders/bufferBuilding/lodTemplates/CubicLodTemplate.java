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

package com.seibel.lod.core.builders.bufferBuilding.lodTemplates;

import java.util.Map;

import com.seibel.lod.core.enums.LodDirection;
import com.seibel.lod.core.enums.rendering.DebugMode;
import com.seibel.lod.core.objects.VertexOptimizer;
import com.seibel.lod.core.objects.opengl.LodBufferBuilder;
import com.seibel.lod.core.util.DataPointUtil;
import com.seibel.lod.core.util.LodUtil;

/**
 * Builds LODs as rectangular prisms.
 * @author James Seibel
 * @version 12-8-2021
 */
public class CubicLodTemplate extends AbstractLodTemplate
{
	
	public CubicLodTemplate()
	{
	
	}
	
	@Override
	public void addLodToBuffer(LodBufferBuilder buffer, int playerX, int playerY, int playerZ, long data, Map<LodDirection, long[]> adjData,
			byte detailLevel, int posX, int posZ, VertexOptimizer vertexOptimizer, DebugMode debugging, boolean[] adjShadeDisabled)
	{
		if (vertexOptimizer == null)
			return;
		
		// equivalent to 2^detailLevel
		int blockWidth = 1 << detailLevel;
		
		int color;
		if (debugging != DebugMode.OFF)
			color = LodUtil.DEBUG_DETAIL_LEVEL_COLORS[detailLevel].getRGB();
		else
			color = DataPointUtil.getColor(data);
		
		
		generateBoundingBox(
				vertexOptimizer,
				DataPointUtil.getHeight(data),
				DataPointUtil.getDepth(data),
				blockWidth,
				posX * blockWidth, 0, posZ * blockWidth, // x, y, z offset
				playerX,
				playerY,
				playerZ,
				adjData,
				color,
				DataPointUtil.getLightSkyAlt(data),
				DataPointUtil.getLightBlock(data),
				adjShadeDisabled);
		
		addBoundingBoxToBuffer(buffer, vertexOptimizer);
	}
	
	private void generateBoundingBox(VertexOptimizer vertexOptimizer,
			int height, int depth, int width,
			double xOffset, double yOffset, double zOffset,
			int playerX, int playerY, int playerZ,
			Map<LodDirection, long[]> adjData,
			int color, byte skyLight, byte blockLight,
			boolean[] adjShadeDisabled)
	{
		// don't add an LOD if it is empty
		if (height == -1 && depth == -1)
			return;
		
		if (depth == height)
			// if the top and bottom points are at the same height
			// render this LOD as 1 block thick
			height++;
		
		// offset the AABB by its x/z position in the world since
		// it uses doubles to specify its location, unlike the model view matrix
		// which only uses floats
		double x = -playerX;
		double z = -playerZ;
		vertexOptimizer.reset();
		vertexOptimizer.setColor(color, adjShadeDisabled);
		vertexOptimizer.setLights(skyLight, blockLight);
		vertexOptimizer.setWidth(width, height - depth, width);
		vertexOptimizer.setOffset((int) (xOffset + x), (int) (depth + yOffset), (int) (zOffset + z));
		vertexOptimizer.setAdjData(adjData);
	}
	
	private void addBoundingBoxToBuffer(LodBufferBuilder buffer, VertexOptimizer vertexOptimizer)
	{
		int color;
		byte skyLight;
		byte blockLight;
		for (LodDirection lodDirection : VertexOptimizer.DIRECTIONS)
		{
			int verticalFaceIndex = 0;
			while (vertexOptimizer.shouldRenderFace(lodDirection, verticalFaceIndex))
			{
				for (int vertexIndex = 0; vertexIndex < 6; vertexIndex++)
				{
					skyLight = vertexOptimizer.getSkyLight(lodDirection, verticalFaceIndex);
					blockLight = (byte) vertexOptimizer.getBlockLight();
					color = vertexOptimizer.getColor(lodDirection);
					addPosAndColor(buffer,
							vertexOptimizer.getX(lodDirection, vertexIndex),
							vertexOptimizer.getY(lodDirection, vertexIndex, verticalFaceIndex) + DataPointUtil.VERTICAL_OFFSET,
							vertexOptimizer.getZ(lodDirection, vertexIndex),
							color, skyLight, blockLight );
				}
				verticalFaceIndex++;
			}
		}
	}
	
}
