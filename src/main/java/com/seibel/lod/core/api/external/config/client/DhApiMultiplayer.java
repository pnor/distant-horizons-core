package com.seibel.lod.core.api.external.config.client;

import com.seibel.lod.core.api.external.apiObjects.enums.EDhApiDistanceGenerationMode;
import com.seibel.lod.core.api.external.apiObjects.enums.EDhApiServerFolderNameMode;
import com.seibel.lod.core.api.external.apiObjects.wrapperInterfaces.IDhApiConfig;
import com.seibel.lod.core.api.implementation.objects.GenericEnumConverter;
import com.seibel.lod.core.api.implementation.wrappers.DhApiConfig;
import com.seibel.lod.core.config.Config.Client.Multiplayer;
import com.seibel.lod.core.enums.config.EServerFolderNameMode;

/**
 * Multiplayer settings.
 *
 * @author James Seibel
 * @version 2022-7-2
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
	public static IDhApiConfig<EDhApiDistanceGenerationMode> getDistantGeneratorModeConfig()
	{ return new DhApiConfig<>(Multiplayer.multiDimensionRequiredSimilarity); }
	
	
}
