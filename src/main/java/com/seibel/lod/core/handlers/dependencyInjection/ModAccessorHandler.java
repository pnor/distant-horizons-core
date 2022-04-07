/*
 *    This file is part of the Distant Horizons mod (formerly the LOD Mod),
 *    licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2020-2022  James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.seibel.lod.core.handlers.dependencyInjection;

import com.seibel.lod.core.api.ApiShared;
import com.seibel.lod.core.wrapperInterfaces.modAccessor.IModAccessor;

/**
 * This class takes care of dependency injection for mods accessors. (for mod compatibility
 * support).
 * 
 * This is basically the same as the SingletonHandler, except it can return null. 
 * Getting null either means the mod isn't loaded in the game
 * or it hasn't been implemented for the given Minecraft version.
 * 
 * @author James Seibel
 * @author Leetom
 * @version 3-1-2022
 */
public class ModAccessorHandler
{
	private static final DependencyHandler dependencyHandler = new DependencyHandler();
	
	
	/**
	 * Links the given mod accessor to an interface so it can be referenced later.
	 * 
	 * @param interfaceClass The interface the mod accessor should implement.
	 * @param modAccessor An object that implements the interfaceClass interface.
	 * @throws IllegalStateException if the mod accessor doesn't implement 
	 *                               the interface or the interface has already been bound.
	 */
	public static void bind(Class<? extends IModAccessor> interfaceClass, IModAccessor modAccessor)
			throws IllegalStateException
	{
		dependencyHandler.bind(interfaceClass, modAccessor);
		ApiShared.LOGGER.info("Registored mod comatibility accessor for " + modAccessor.getModName());
	}
	
	/**
	 * Returns a mod accessor of type T if one has been bound.
	 * Returns null otherwise.
	 * 
	 * @param <T> class of the mod accessor
	 *            (inferred from the objectClass parameter)
	 * @param interfaceClass Interface of the mod accessor
	 * @return the dependency of type T
	 * @throws ClassCastException If the mod accessor isn't able to be cast to type T. 
	 *                            (this shouldn't normally happen, unless the bound object changed somehow)
	 */
	public static <T extends IModAccessor> T get(Class<T> objectClass) throws ClassCastException
	{
		return dependencyHandler.get(objectClass);
	}
	
	
	/**
	 * Should only be called after all Binds have been done.
	 * Calls the delayedSetup method for each dependency. <br> <br>
	 * 
	 * This is done so we can have circular dependencies.
	 */
	public static void finishBinding()
	{
		dependencyHandler.finishBinding();
	}
	
	/** returns whether the finishBinding method has been called */
	public static boolean bindingFinished() 
	{
		return dependencyHandler.getBindingFinished();
	}
	
}
