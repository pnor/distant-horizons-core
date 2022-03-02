/*
 *    This file is part of the Distant Horizon mod (formerly the LOD Mod),
 *    licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2020  James Seibel
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

import java.util.HashMap;
import java.util.Map;

/**
 * This class takes care of dependency injection
 * for dependencies.
 * 
 * @author James Seibel
 * @version 3-1-2022
 */
public class DependencyHandler
{
	private static final Map<Class<?>, Object> dependencies = new HashMap<Class<?>, Object>();
	

	/**
	 * Links the given implementation object to an interface so it can be referenced later.
	 * 
	 * @param depenencyInterface The interface the implementation object should implement.
	 * @param dependencyImplementation A object that implements the depenencyInterface interface.
	 * @throws IllegalStateException if the implementation object doesn't implement 
	 *                               the interface or the interface has already been bound.
	 */
	public void bind(Class<?> depenencyInterface, Object dependencyImplementation) throws IllegalStateException
	{
		// make sure we haven't already bound this dependency
		if (dependencies.containsKey(depenencyInterface))
		{
			throw new IllegalStateException("The dependency [" + depenencyInterface.getSimpleName() + "] has already been bound.");
		}
		
		
		// make sure the given dependency implements the interface
		boolean dependencyImplementsInterface = false;
		for (Class<?> implementationInterface : dependencyImplementation.getClass().getInterfaces())
		{
			if (implementationInterface.equals(depenencyInterface))
			{
				dependencyImplementsInterface = true;
				break;
			}
		}
		if (!dependencyImplementsInterface)
		{
			throw new IllegalStateException("The dependency [" + dependencyImplementation.getClass().getSimpleName() + "] doesn't implement the interface [" + depenencyInterface.getSimpleName() + "].");
		}
		
		
		dependencies.put(depenencyInterface, dependencyImplementation);
	}

	/**
	 * Returns a dependency of type T if one has been bound.
	 * Returns null otherwise.
	 * 
	 * @param <T> class of the dependency
	 *            (inferred from the objectClass parameter)
	 * @param interfaceClass Interface of the dependency
	 * @return the dependency of type T
	 * @throws ClassCastException If the dependency isn't able to be cast to type T. 
	 *                            (this shouldn't normally happen, unless the bound object changed somehow)
	 */
	@SuppressWarnings("unchecked")
	public <T> T get(Class<T> interfaceClass) throws ClassCastException
	{
		return (T) dependencies.get(interfaceClass);
	}
	
}
