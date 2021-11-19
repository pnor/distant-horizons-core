package com.seibel.lod.wrappers;

import com.seibel.lod.core.wrapperAdapters.IWrapperFactory;
import com.seibel.lod.core.wrapperAdapters.SingletonHandler;
import com.seibel.lod.core.wrapperAdapters.block.IBlockColorSingletonWrapper;
import com.seibel.lod.core.wrapperAdapters.config.ILodConfigWrapperSingleton;
import com.seibel.lod.core.wrapperAdapters.handlers.IReflectionHandler;
import com.seibel.lod.core.wrapperAdapters.minecraft.IMinecraftRenderWrapper;
import com.seibel.lod.core.wrapperAdapters.minecraft.IMinecraftWrapper;
import com.seibel.lod.wrappers.block.BlockColorSingletonWrapper;
import com.seibel.lod.wrappers.config.LodConfigWrapperSingleton;
import com.seibel.lod.wrappers.handlers.ReflectionHandler;
import com.seibel.lod.wrappers.minecraft.MinecraftRenderWrapper;
import com.seibel.lod.wrappers.minecraft.MinecraftWrapper;

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
		SingletonHandler.bind(IBlockColorSingletonWrapper.class, BlockColorSingletonWrapper.INSTANCE);
		SingletonHandler.bind(IMinecraftWrapper.class, MinecraftWrapper.INSTANCE);
		SingletonHandler.bind(IMinecraftRenderWrapper.class, MinecraftRenderWrapper.INSTANCE);
		SingletonHandler.bind(IWrapperFactory.class, WrapperFactory.INSTANCE);
		SingletonHandler.bind(IReflectionHandler.class, ReflectionHandler.INSTANCE);
	}
}
