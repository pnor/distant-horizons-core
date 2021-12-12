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

package com.seibel.lod.core.handlers;

import java.lang.reflect.Field;
import java.util.HashSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.seibel.lod.core.ModInfo;
import com.seibel.lod.core.enums.rendering.FogDrawMode;
import com.seibel.lod.core.util.SingletonHandler;
import com.seibel.lod.core.wrapperInterfaces.IWrapperFactory;
import com.seibel.lod.core.wrapperInterfaces.chunk.AbstractChunkPosWrapper;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;

/**
 * A singleton used to get variables from methods
 * where they are private or potentially absent. 
 * For example: the fog setting in Optifine or the
 * presence/absence of Vivecraft.
 * 
 * @author James Seibel
 * @version 12-12-2021
 */
public class ReflectionHandler implements IReflectionHandler
{
	private static final Logger LOGGER = LogManager.getLogger(ModInfo.NAME + "-" + ReflectionHandler.class.getSimpleName());
	
	private final IWrapperFactory wrapperFactory;
	
	public static ReflectionHandler instance;
	
	private Field ofFogField = null;
	private final Object mcOptionsObject;
	
	private Boolean sodiumPresent = null;
	private Field sodiumLoadedChunkPositionsField = null;
	private Object sodiumWorldRendererInstance = null;
	
	
	
	
	private ReflectionHandler(Field[] optionFields, Object newMcOptionsObject)
	{
		wrapperFactory = SingletonHandler.get(IWrapperFactory.class);
		
		mcOptionsObject = newMcOptionsObject;
		
		setupFogField(optionFields);
		
		if (sodiumPresent())
			setupSodiumInteraction();
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
	
	
	

	private void setupSodiumInteraction()
	{
		String errorMessagePrfix = ReflectionHandler.class.getSimpleName() + ": was unable to setup Sodium interaction. Error: ";
		
		try
		{
			// try getting the SodiumWorldRender class
			Class<?> sodiumWorldRendererClass = Class.forName("me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer");
			
			Field sodiumWorldRendererInstanceField = sodiumWorldRendererClass.getDeclaredField("instance");
			sodiumWorldRendererInstanceField.setAccessible(true); // the field is private by default
			
			try
			{
				// try getting the singleton from the static field
				sodiumWorldRendererInstance = sodiumWorldRendererInstanceField.get(null);
				
				try
				{
					// try getting the loadedChunkPosition field
					Field loadedPosField = sodiumWorldRendererInstance.getClass().getDeclaredField("loadedChunkPositions");
					loadedPosField.setAccessible(true);
					
					sodiumLoadedChunkPositionsField = loadedPosField;
				}
				catch (IllegalArgumentException e)
				{
					LOGGER.info(errorMessagePrfix + " no loadedChunkPositions field.", e);
				}
			}
			catch (IllegalArgumentException | IllegalAccessException e)
			{
				LOGGER.info(errorMessagePrfix + " no sodiumWorldRenderer instance.", e);
			}
		}
		catch (NoSuchFieldException | SecurityException | ClassNotFoundException e)
		{
			LOGGER.info(errorMessagePrfix + " no sodiumWorldRenderer class.", e);
		}
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
	
	/** 
	 * TODO: this returns chunks that aren't actually rendered. 
	 * Specifically as of 1.16.5 (12-12-2021) it also returns one layer
	 * of chunks further than what is currently rendered.
	 */
	@Override
	public HashSet<AbstractChunkPosWrapper> getSodiumRenderedChunks()
	{
		if (!sodiumPresent())
		{
			throw new IllegalStateException("[getSodiumRenderedChunks] can only be called if Sodium is installed.");
		}
		
		if (sodiumLoadedChunkPositionsField == null || sodiumWorldRendererInstance == null)
		{
			throw new IllegalStateException("[getSodiumRenderedChunks] was called either before the sodium setup was done, or the sodium setup failed.");
		}
		
		
		
		
		HashSet<AbstractChunkPosWrapper> loadedPos = new HashSet<>();
		
		try
		{
			LongSet loadedChunkPositions = (LongSet) sodiumLoadedChunkPositionsField.get(sodiumWorldRendererInstance);
			
			LongIterator iterator = loadedChunkPositions.iterator();
			while (iterator.hasNext())
			{
				loadedPos.add(wrapperFactory.createChunkPos(iterator.nextLong()));
			}
		}
		catch (IllegalArgumentException | IllegalAccessException e)
		{
			LOGGER.error("Unable to get sodium's rendered chunks" + e.getMessage(), e);
		}
		
		
//		// can be uncommented for debugging
//		StringBuilder builder = new StringBuilder(loadedPos.size() * 4);
//		for(AbstractChunkPosWrapper pos : loadedPos)
//		{
//			builder.append("(" + pos.getX() + "," + pos.getZ() + ") ");
//		}
//		ClientApi.LOGGER.info(builder.toString());
		
		return loadedPos;
	}
}
