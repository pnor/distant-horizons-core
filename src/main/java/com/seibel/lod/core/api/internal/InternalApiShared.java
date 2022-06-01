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

package com.seibel.lod.core.api.internal;

import com.seibel.lod.core.builders.lodBuilding.LodBuilder;
import com.seibel.lod.core.enums.config.VerticalQuality;
import com.seibel.lod.core.objects.lod.LodWorld;

/**
 * This stores objects and variables that
 * are shared between the different Core internal api classes. <br> <br>
 *
 * The internal api package is designed to hold any code that
 * interfaces between Distant Horizons and the host mod loader
 * (IE Fabric or Forge).
 *
 * @author James Seibel
 * @version 2022-4-24
 */
@Deprecated
public class InternalApiShared
{
	public InternalApiShared INSTANCE = new InternalApiShared();
	
	public static final LodWorld lodWorld = new LodWorld();
	public static final LodBuilder lodBuilder = new LodBuilder();
	
	/** Used to determine if the LODs should be regenerated */
	public static int previousChunkRenderDistance = 0;
	/** Used to determine if the LODs should be regenerated */
	public static int previousLodRenderDistance = 0;
	public static VerticalQuality previousVertQual = null;
	
	/** Signal whether a world is shutting down */
	public static volatile boolean isShuttingDown = false;

	
	
	private InternalApiShared()
	{

	}
	
}
