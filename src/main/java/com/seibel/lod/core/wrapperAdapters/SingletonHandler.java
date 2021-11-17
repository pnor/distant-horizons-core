package com.seibel.lod.core.wrapperAdapters;

import java.util.HashMap;
import java.util.Map;

/**
 * This class takes care of dependency injection
 * for singletons.
 * 
 * @author James Seibel
 * @version 11-15-2021
 */
public class SingletonHandler
{
	private static final Map<Class<?>, Object> singletons = new HashMap<Class<?>, Object>();
	
	
	
	

	/**
	 * Adds the given singleton so it can be referenced later.
	 * 
	 * @param objectClass
	 * @param singletonReference
	 * @throws IllegalStateException
	 */
	public static void bind(Class<?> interfaceClass, Object singletonReference) throws IllegalStateException
	{
		// make sure we haven't already bound this singleton
		if (singletons.containsKey(interfaceClass))
		{
			throw new IllegalStateException("The singleton [" + interfaceClass.getSimpleName() + "] has already been bound.");
		}
		
		
		// make sure the given singleton implements the interface
		boolean singletonImplementsInterface = false;
		for (Class<?> singletonInterface : singletonReference.getClass().getInterfaces())
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
		
		
		singletons.put(interfaceClass, singletonReference);
	}

	/**
	 * Returns a singleton of type T
	 * if one has been bound.
	 * 
	 * @param <T> class of the singleton
	 * @param objectClass class of the singleton, but as a parameter!
	 * @return the singleton of type T
	 * @throws NullPointerException if no singleton of type T has been bound.
	 * @throws ClassCastException if the singleton isn't able to be cast to type T. (this shouldn't normally happen, unless the bound object changed somehow)
	 */
	@SuppressWarnings("unchecked")
	public static <T> T get(Class<T> objectClass) throws NullPointerException, ClassCastException
	{
		if (!singletons.containsKey(objectClass))
		{
			throw new NullPointerException("The singleton [" + objectClass.getSimpleName() + "] was never bound.");
		}
		
		return (T) singletons.get(objectClass);
	}
	
}
