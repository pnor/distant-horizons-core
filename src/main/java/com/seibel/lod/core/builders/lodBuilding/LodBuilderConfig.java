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

package com.seibel.lod.core.builders.lodBuilding;

import com.seibel.lod.core.enums.config.DistanceGenerationMode;

/**
 * This is used to easily configure how LodChunks are generated.
 * Generally this will only be used if we want to generate a
 * LodChunk using an incomplete Chunk, otherwise the defaults
 * work best for a fully generated chunk (IE has correct surface blocks).
 * @author James Seibel
 * @version 8-14-2021
 */
public class LodBuilderConfig
{
	/** default: false */
	public boolean useHeightmap;
	/** default: false */
	public boolean useBiomeColors;
	/** default: true */
	public boolean useSolidBlocksInColorGen;
	/** default: server */
	public DistanceGenerationMode distanceGenerationMode;
	public boolean quickFillWithVoid;
	
	/**
	 * default settings for a normal chunk <br>
	 * useHeightmap = false <br>
	 * useBiomeColors = false <br>
	 * useSolidBlocksInColorGen = true <br>
	 * generationMode = Server <br>
	 */
	public LodBuilderConfig(DistanceGenerationMode newDistanceGenerationMode)
	{
		useHeightmap = false;
		useBiomeColors = false;
		useSolidBlocksInColorGen = true;
		quickFillWithVoid = false;
		distanceGenerationMode = newDistanceGenerationMode;
	}
	
	public static LodBuilderConfig getFillVoidConfig() {
		LodBuilderConfig config = new LodBuilderConfig(DistanceGenerationMode.NONE);
		config.quickFillWithVoid = true;
		return config;
	}
}