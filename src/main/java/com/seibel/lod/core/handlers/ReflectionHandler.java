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

package com.seibel.lod.core.handlers;

import java.lang.reflect.Field;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.seibel.lod.core.ModInfo;
import com.seibel.lod.core.enums.rendering.FogDrawMode;

/**
 * A singleton used to get variables from methods
 * where they are private or potentially absent. 
 * For example: the fog setting in Optifine or the
 * presence/absence of Vivecraft.
 * 
 * @author James Seibel
 * @version 12-14-2021
 */
public class ReflectionHandler implements IReflectionHandler
{
	private static final Logger LOGGER = LogManager.getLogger(ModInfo.NAME + "-" + ReflectionHandler.class.getSimpleName());
	
	public static ReflectionHandler instance;
	
	private Field ofFogField = null;
	private final Object mcOptionsObject;
	
	private Boolean sodiumPresent = null;
	private boolean optifinePresent = false;
	
	
	
	
	private ReflectionHandler(Field[] optionFields, Object newMcOptionsObject)
	{
		mcOptionsObject = newMcOptionsObject;
		
		setupFogField(optionFields);
	}
	
	/**
	 * @param optionFields the fields that should contain "ofFogType"
	 * @param newMcOptionsObject the object instance that contains "ofFogType"
	 * @return the ReflectionHandler just created
	 * @throws IllegalStateException if a ReflectionHandler already exists
	 */
	public static ReflectionHandler createSingleton(Field[] optionFields, Object newMcOptionsObject) throws IllegalStateException
	{
		if (instance != null)
		{
			throw new IllegalStateException();	
		}
		
		instance = new ReflectionHandler(optionFields, newMcOptionsObject);
		return instance;
	}
	
	
	
	
	
	
	/** finds the Optifine fog type field */
	private void setupFogField(Field[] optionFields)
	{
		// try and find the ofFogType variable in gameSettings
		for (Field field : optionFields)
		{
			if (field.getName().equals("ofFogType"))
			{
				optifinePresent = true;
				ofFogField = field;
				return;
			}
		}
		
		// we didn't find the field,
		// either optifine isn't installed, or
		// optifine changed the name of the variable
		LOGGER.info(ReflectionHandler.class.getSimpleName() + ": unable to find the Optifine fog field. If Optifine isn't installed this can be ignored.");
	}
	
	
	/**
	 * Get what type of fog optifine is currently set to render.
	 * @return the fog quality
	 */
	@Override
	public FogDrawMode getFogDrawMode()
	{
		if (ofFogField == null)
		{
			// either optifine isn't installed,
			// the variable name was changed, or
			// the setup method wasn't called yet.
			return FogDrawMode.FOG_ENABLED;
		}
		
		int returnNum = 0;
		
		try
		{
			returnNum = (int) ofFogField.get(mcOptionsObject);
		}
		catch (IllegalArgumentException | IllegalAccessException e)
		{
			e.printStackTrace();
		}
		
		switch (returnNum)
		{
		default:
		case 0:
			// optifine's "default" option,
			// it should never be called in this case
			
			// normal options
		case 1: // fast
		case 2: // fancy
			return FogDrawMode.FOG_ENABLED;
		case 3: // off
			return FogDrawMode.FOG_DISABLED;
		}
	}
	
	
	
	/** Detect if Vivecraft is present. Attempts to find the "VRRenderer" class. */
	@Override
	public boolean vivecraftPresent()
	{
		try
		{
			Class.forName("org.vivecraft.provider.VRRenderer");
			return true;
		}
		catch (ClassNotFoundException ignored)
		{
			LOGGER.info(ReflectionHandler.class.getSimpleName() + ": Vivecraft not detected.");
		}
		return false;
	}
	@Override
	public boolean optifinePresent()
	{
		return optifinePresent;
	}
	
	
	
	@Override
	public boolean sodiumPresent()
	{
		// we don't want to run a potentially expensive
		// reflection search operation every time this method is called
		if (sodiumPresent == null)
		{
			try
			{
				Class.forName("me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer");
				
				sodiumPresent = true;
			}
			catch (ClassNotFoundException e)
			{
				sodiumPresent = false;
			}
		}
		
		return sodiumPresent;
	}
	
}
