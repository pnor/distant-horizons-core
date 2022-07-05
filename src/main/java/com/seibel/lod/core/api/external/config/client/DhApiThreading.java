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

package com.seibel.lod.core.api.external.config.client;

import com.seibel.lod.core.api.external.apiObjects.wrapperInterfaces.IDhApiConfig;
import com.seibel.lod.core.api.implementation.wrappers.DhApiConfig;
import com.seibel.lod.core.config.Config.Client.Advanced.Threading;

/**
 * Distant Horizons threading configuration.
 *
 * @author James Seibel
 * @version 2022-7-4
 */
public class DhApiThreading
{
	
	/**
	 * Returns the config related to the world generator threads. <br>
	 * <br>
	 * If the number of threads is less than 1 it will be treated as a percentage
	 * representing how often a single thread will be actively generating terrain. <br> <br>
	 *
	 * 0.0 = 1 thread active 0% of the time <br>
	 * 0.5 = 1 thread active 50% of the time <br>
	 * 1.0 = 1 thread active 100% of the time <br>
	 * 1.5 = 2 threads active 100% of the time (partial values are rounded up) <br>
	 * 2.0 = 2 threads active 100% of the time <br>
	 *
	 * @deprecated this (and the related config) should be replaced with an int
	 * 				count of threads and then a double percent active config.
	 */
	@Deprecated
	public static IDhApiConfig<Double> getWorldGeneratorThreadConfig()
	{ return new DhApiConfig<>(Threading.numberOfWorldGenerationThreads); }
	
	// TODO the above should be replaced with these
//	public static IDhApiConfig<Integer> getWorldGeneratorThreadConfig()
//	{ return new DhApiConfig<>(Threading.numberOfWorldGenerationThreads); }
	
//	public static IDhApiConfig<Double> getWorldGeneratorThreadActivePercentConfig()
//	{ return new DhApiConfig<>(Threading.ToBeDetermined); }
	
	
	/** Returns the config related to the buffer (GPU Terrain data) builder threads. */
	public static IDhApiConfig<Integer> getBufferBuilderThreadConfig()
	{ return new DhApiConfig<>(Threading.numberOfBufferBuilderThreads); }
	
}
