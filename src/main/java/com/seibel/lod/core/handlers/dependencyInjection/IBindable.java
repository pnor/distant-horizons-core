package com.seibel.lod.core.handlers.dependencyInjection;

/**
 * Necessary for all singletons that can be dependency injected.
 * 
 * @author James Seibel
 * @version 3-4-2022
 */
public interface IBindable
{
	/**
	 * Finish initializing this object. <br> <br>
	 * 
	 * Generally this should just used for getting other objects through
	 * dependency injection and is specifically designed to allow 
	 * for circular references. <br><br>
	 * 
	 * If no circular dependencies are required this method 
	 * doesn't have to be implemented.
	 */
	public default void finishDelayedSetup()
	{
		
	}
}
