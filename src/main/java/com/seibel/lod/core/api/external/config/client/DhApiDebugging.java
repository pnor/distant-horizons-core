package com.seibel.lod.core.api.external.config.client;

import com.seibel.lod.core.api.external.apiObjects.enums.EDhApiDebugMode;
import com.seibel.lod.core.api.external.apiObjects.wrapperInterfaces.IDhApiConfig;
import com.seibel.lod.core.api.implementation.objects.GenericEnumConverter;
import com.seibel.lod.core.api.implementation.wrappers.DhApiConfig;
import com.seibel.lod.core.config.Config.Client.Advanced.Debugging;
import com.seibel.lod.core.enums.rendering.EDebugMode;

/**
 * Multiplayer settings.
 *
 * @author James Seibel
 * @version 2022-7-2
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
