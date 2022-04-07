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
 
package com.seibel.lod.core.wrapperInterfaces;

import com.seibel.lod.core.enums.config.DistanceGenerationMode;
import com.seibel.lod.core.handlers.dependencyInjection.IBindable;

/**
 * A singleton that contains variables specific to each version of Minecraft
 * which can be used to change how DH-Core runs. For example: After MC 1.17
 * blocks can be negative, which changes how we generate LODs.
 * 
 * @author James Seibel
 * @version 3-5-2022
 */
public interface IVersionConstants extends IBindable {
	/** @returns the minimum height blocks can be generated */
	int getMinimumWorldHeight();

	/**
	 * @Returns the number of generations call per thread.
	 */
	default int getWorldGenerationCountPerThread() {
		return 8;
	}
	
	boolean isVanillaRenderedChunkSquare();

}
