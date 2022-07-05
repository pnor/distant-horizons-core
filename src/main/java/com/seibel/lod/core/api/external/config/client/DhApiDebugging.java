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

import com.seibel.lod.core.api.external.apiObjects.enums.EDhApiDebugMode;
import com.seibel.lod.core.api.external.apiObjects.wrapperInterfaces.IDhApiConfig;
import com.seibel.lod.core.api.implementation.objects.GenericEnumConverter;
import com.seibel.lod.core.api.implementation.wrappers.DhApiConfig;
import com.seibel.lod.core.config.Config.Client.Advanced.Debugging;
import com.seibel.lod.core.enums.rendering.EDebugMode;

/**
 * Distant Horizons debug configuration.
 *
 * @author James Seibel
 * @version 2022-7-4
 */
public class DhApiDebugging
{
	
	/**
	 * Returns the config related to if/how Distant Horizons
	 * uses debug rendering.
	 */
	public static IDhApiConfig<EDhApiDebugMode> getDebugRenderModeConfig()
	{ return new DhApiConfig<>(Debugging.debugMode, new GenericEnumConverter<>(EDebugMode.class, EDhApiDebugMode.class)); }
	
	/**
	 * Returns the config related to if Distant Horizons
	 * debug keybindings are active or not.
	 */
	public static IDhApiConfig<Double> getEnableDebugKeybindingsConfig()
	{ return new DhApiConfig<>(Debugging.enableDebugKeybindings); }
	
	
}
