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
 
package com.seibel.lod.core.wrapperInterfaces.modAccessor;

import com.seibel.lod.core.handlers.dependencyInjection.IBindable;

/**
 * Checks if a mod is loaded
 *
 * @author coolGi
 * @version 3-5-2022
 */
public interface IModChecker extends IBindable {
    /** Checks if a mod is loaded */
    boolean isModLoaded(String modid);
}
