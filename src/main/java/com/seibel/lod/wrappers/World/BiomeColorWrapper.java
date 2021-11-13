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

package com.seibel.lod.wrappers.World;

import com.seibel.lod.wrappers.Block.BlockPosWrapper;

import net.minecraft.world.biome.BiomeColors;


/**
 * 
 * @author Cola?
 * @version 11-12-2021
 */
public class BiomeColorWrapper
{
	
	public static int getGrassColor(WorldWrapper world, BlockPosWrapper blockPos)
	{
		return BiomeColors.getAverageGrassColor(world.getWorld(), blockPos.getBlockPos());
	}
	public static int getWaterColor(WorldWrapper world, BlockPosWrapper blockPos)
	{
		
		return BiomeColors.getAverageWaterColor(world.getWorld(), blockPos.getBlockPos());
	}
	public static int getFoliageColor(WorldWrapper world, BlockPosWrapper blockPos)
	{
		
		return BiomeColors.getAverageFoliageColor(world.getWorld(), blockPos.getBlockPos());
	}
}
