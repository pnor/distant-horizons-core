package com.seibel.lod.core.api;

import java.util.HashMap;
import java.util.Map;

import com.seibel.lod.core.wrapperInterfaces.modAccessor.IModAccessor;

/**
 * This class takes care of dependency injection
 * for mods (for mod compatibility support).
 * 
 * (Basically the same as SingletonHandler, except
 * it can return null which means that mod aren't
 * loaded in the game, or it haven't been implemented
 * for that build.)
 */
public class ModAccessorApi {
	private static final Map<Class<? extends IModAccessor>, IModAccessor> singletons = new HashMap<Class<? extends IModAccessor>, IModAccessor>();

	public static void bind(Class<? extends IModAccessor> interfaceClass, IModAccessor modAccessor) throws IllegalStateException
	{
		// make sure we haven't already bound this singleton
		if (singletons.containsKey(interfaceClass))
		{
			throw new IllegalStateException("The modAccessor [" + interfaceClass.getSimpleName() + "] has already been bound.");
		}
		
		// make sure the given singleton implements the interface
		boolean singletonImplementsInterface = false;
		for (Class<?> singletonInterface : modAccessor.getClass().getInterfaces())
		{
			if (singletonInterface.equals(interfaceClass))
			{
				singletonImplementsInterface = true;
				break;
			}
		}
		if (!singletonImplementsInterface)
		{
			throw new IllegalStateException("The singleton [" + interfaceClass.getSimpleName() + "] doesn't implement the interface [" + interfaceClass.getSimpleName() + "].");
		}
		ApiShared.LOGGER.info("DistantHorizon: Registored mod comatibility accessor for "+modAccessor.getModName());
		singletons.put(interfaceClass, modAccessor);
	}

	@SuppressWarnings("unchecked")
	public static <T extends IModAccessor> T get(Class<T> objectClass) throws ClassCastException
	{
		IModAccessor modAccessor = singletons.get(objectClass);
		return modAccessor==null ? null : (T) modAccessor;
	}
	
}
