package com.seibel.lod.wrappers;

import com.seibel.lod.core.wrapperAdapters.SingletonHandler;
import com.seibel.lod.core.wrapperAdapters.config.ILodConfigWrapperSingleton;
import com.seibel.lod.wrappers.config.LodConfigWrapperSingleton;

/**
 * Binds all necessary dependencies so we
 * can access them in Core. <br>
 * This needs to be called before any Core classes
 * are loaded.
 * 
 * @author James Seibel
 * @version 11-16-2021
 */
public class DependencySetup
{
	public static void createInitialBindings()
	{
		SingletonHandler.bind(ILodConfigWrapperSingleton.class, LodConfigWrapperSingleton.INSTANCE);
	}
}
