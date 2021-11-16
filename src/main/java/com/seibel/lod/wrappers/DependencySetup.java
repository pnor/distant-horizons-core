package com.seibel.lod.wrappers;

import com.seibel.lod.core.wrapperAdapters.SingletonHandler;
import com.seibel.lod.core.wrapperAdapters.config.ILodConfigWrapperSingleton;
import com.seibel.lod.wrappers.config.LodConfigWrapperSingleton;

/**
 * Binds all necessary singletons so we
 * can access them in Core.
 * 
 * @author James Seibel
 * @version 11-15-2021
 */
public class DependencySetup
{
	static
	{
		SingletonHandler.bind(ILodConfigWrapperSingleton.class, LodConfigWrapperSingleton.INSTANCE);
	}
}
