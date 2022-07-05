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

import com.seibel.lod.core.api.external.apiObjects.enums.EDhApiServerFolderNameMode;
import com.seibel.lod.core.api.external.apiObjects.wrapperInterfaces.IDhApiConfig;
import com.seibel.lod.core.api.implementation.objects.GenericEnumConverter;
import com.seibel.lod.core.api.implementation.wrappers.DhApiConfig;
import com.seibel.lod.core.config.Config.Client.Multiplayer;
import com.seibel.lod.core.enums.config.EServerFolderNameMode;

/**
 * Distant Horizons client-side multiplayer configuration.
 *
 * @author James Seibel
 * @version 2022-7-4
 */
public class DhApiMultiplayer
{
	
	/**
	 * Returns the config related to how Distant Horizons
	 * names multiplayer server folders.
	 */
	public static IDhApiConfig<EDhApiServerFolderNameMode> getFolderSavingModeConfig()
	{ return new DhApiConfig<>(Multiplayer.serverFolderNameMode, new GenericEnumConverter<>(EServerFolderNameMode.class, EDhApiServerFolderNameMode.class)); }
	
	/**
	 * Returns the config related to how Distant Horizons' determines
	 * what level a specific dimension belongs too. <br>
	 * This is specifically to support serverside mods like Multiverse.
	 */
	public static IDhApiConfig<Double> getDistantGeneratorModeConfig()
	{ return new DhApiConfig<>(Multiplayer.multiDimensionRequiredSimilarity); }
	
	
}
