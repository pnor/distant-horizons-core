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

package com.seibel.lod.core.enums.config;

/**
 * heightmap <br>
 * multi_lod <br>
 *
 * @author Leonardo Amato
 * @version 10-07-2021
 */
public enum VerticalQuality
{
	LOW(
			new int[] { 4, 2, 2, 2, 2, 1, 1, 1, 1, 1, 1 },
			2
	),
	
	MEDIUM(
			new int[] { 6, 4, 3, 2, 2, 1, 1, 1, 1, 1, 1 },
			4
	),
	
	HIGH(
			new int[] { 8, 6, 4, 2, 2, 2, 2, 1, 1, 1, 1 },
			6
	),
	
	ULTRA(
			new int[] { 16, 8, 4, 2, 2, 2, 2, 1, 1, 1, 1 },
			12
	);
	
	public final int[] maxVerticalData;
	
	@Deprecated // Will find other ways to optimize
	public final int maxConnectedLods;
	
	VerticalQuality(int[] maxVerticalData, int maxConnectedLods)
	{
		this.maxVerticalData = maxVerticalData;
		this.maxConnectedLods = maxConnectedLods;
	}
	
	// Note: return null if out of range
	public static VerticalQuality previous(VerticalQuality mode)
	{
		switch (mode)
		{
		case HIGH:
			return VerticalQuality.MEDIUM;
		case MEDIUM:
			return VerticalQuality.LOW;
		case LOW:
		default:
			return null;
		}
	}
	
	// Note: return null if out of range
	public static VerticalQuality next(VerticalQuality mode)
	{
		switch (mode)
		{
		case MEDIUM:
			return VerticalQuality.HIGH;
		case LOW:
			return VerticalQuality.MEDIUM;
		case HIGH:
		default:
			return null;
		}
	}
}