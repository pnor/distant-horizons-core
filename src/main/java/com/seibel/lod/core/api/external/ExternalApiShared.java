package com.seibel.lod.core.api.external;

import com.seibel.lod.core.config.types.ConfigEntry;
import com.seibel.lod.core.handlers.dependencyInjection.SingletonHandler;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton;

/**
 * This stores objects and variables that
 * are shared between the different external api classes. <br> <br>
 *
 * The external api package is designed to hold any code that
 * interfaces between Distant Horizons and other mods or projects. <Br>
 * <strong>For example:</strong> if a weather mod wanted to disable LOD rendering during a blizzard
 * they would do that through a method in the external api.
 *
 *
 * @author James Seibel
 * @version 2022-5-28
 */
public class ExternalApiShared
{
	/**
	 * If the ConfigEntry doesn't allowApiOverride nothing happens.
	 *
	 * @param configEntry The ConfigEntry to set
	 * @param newValue the value to set the config too
	 * @param <T> the type the config accepts
	 *
	 * @return true if the value was set, false otherwise
	 */
	public static <T> boolean attemptToSetApiValue(ConfigEntry<T> configEntry, T newValue)
	{
		if (configEntry.allowApiOverride)
		{
			configEntry.set(newValue);
			return true;
		}
		else
		{
			return false;
		}
	}
}
