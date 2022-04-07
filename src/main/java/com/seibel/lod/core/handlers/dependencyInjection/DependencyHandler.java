/*
 *    This file is part of the Distant Horizon mod (formerly the LOD Mod),
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

import java.util.HashMap;
import java.util.Map;

/**
 * This class takes care of tracking objects used in dependency injection.
 * 
 * @author James Seibel
 * @version 3-4-2022
 */
public class DependencyHandler
{
	private final Map<Class<?>, Object> dependencies = new HashMap<Class<?>, Object>();
	private boolean bindingFinished = false;
	

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
		// only allow binding before the finishBinding method is called
		if (bindingFinished)
		{
			throw new IllegalStateException("The dependency [" + depenencyInterface.getSimpleName() + "] cannot be bound, Binding is finished for [" + this.getClass().getSimpleName() + "]. Make sure your bindings are happening before the [bindingFinished] method is being called.");
		}
		
		
		// make sure we haven't already bound this dependency
		if (dependencies.containsKey(depenencyInterface))
		{
			throw new IllegalStateException("The dependency [" + depenencyInterface.getSimpleName() + "] has already been bound.");
		}
		
		
		// make sure the given dependency implements the necessary interfaces
		boolean implementsInterface = checkIfClassImplements(dependencyImplementation.getClass(), depenencyInterface);
		boolean implementsBindable = checkIfClassImplements(dependencyImplementation.getClass(), IBindable.class);
		
		// display any errors
		if (!implementsInterface)
		{
			throw new IllegalStateException("The dependency [" + dependencyImplementation.getClass().getSimpleName() + "] doesn't implement the interface [" + depenencyInterface.getSimpleName() + "].");
		}
		if (!implementsBindable)
		{
			throw new IllegalStateException("The dependency [" + dependencyImplementation.getClass().getSimpleName() + "] doesn't implement the interface [" + IBindable.class.getSimpleName() + "].");
		}
		
		
		dependencies.put(depenencyInterface, dependencyImplementation);
	}
	/**
	 * Checks if classToTest (or one of its ancestors)
	 * implements the given interface.
	 */
	private boolean checkIfClassImplements(Class<?> classToTest, Class<?> interfaceToLookFor)
	{
		// check the parent class (if applicable)
		if (classToTest.getSuperclass() != Object.class && classToTest.getSuperclass() != null)
		{
			if (checkIfClassImplements(classToTest.getSuperclass(), interfaceToLookFor))
			{
				return true;
			}
		}
		
		
		// check interfaces
		for (Class<?> implementationInterface : classToTest.getInterfaces())
		{
			// recurse to check interface parents if necessary
			if (implementationInterface.getInterfaces().length != 0)
			{
				if (checkIfClassImplements(implementationInterface, interfaceToLookFor))
				{
					return true;
				}
			}
			
			if (implementationInterface.equals(interfaceToLookFor))
			{
				return true;
			}
		}
		
		return false;
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
	public <T extends IBindable> T get(Class<?> interfaceClass) throws ClassCastException
	{
		// getting dependencies should only happen after everything has been bound
		if (!bindingFinished)
		{
			throw new IllegalStateException("Binding hasn't been finished for [" + this.getClass().getSimpleName() + "]. Make sure you are calling the [bindingFinished] method before calling [get].");
		}
		
		return (T) dependencies.get(interfaceClass);
	}
	
	
	/**
	 * Should only be called after all Binds have been done.
	 * Calls the delayedSetup method for each dependency. <br> <br>
	 * 
	 * This is done so we can have circular dependencies.
	 */
	public void finishBinding()
	{
		// (yes technically the binding isn't finished,
		// but this needs to be set to "true" so we can use "get")
		bindingFinished = true;
		
		for (Class<?> interfaceKey : dependencies.keySet())
		{
			IBindable concreteObject = get(interfaceKey);
			concreteObject.finishDelayedSetup();
		}
	}
	
	/** returns whether the finishBinding method has been called */
	public boolean getBindingFinished()
	{
		return bindingFinished;
	}
}
