package com.seibel.lod.wrappers.world;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.seibel.lod.core.enums.WorldType;
import com.seibel.lod.wrappers.block.BlockPosWrapper;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.IWorld;
import net.minecraft.world.server.ServerChunkProvider;
import net.minecraft.world.server.ServerWorld;

/**
 * 
 * @author James Seibel
 * @author ??
 * @version 11-12-2021
 */
public class WorldWrapper
{
	private static final ConcurrentMap<IWorld, WorldWrapper> worldWrapperMap = new ConcurrentHashMap<>();
	private final IWorld world;
	public final WorldType worldType;
	
	
	public WorldWrapper(IWorld newWorld)
	{
		world = newWorld;
		
		if (world.getClass() == ServerWorld.class)
			worldType = WorldType.ServerWorld;	
		else if (world.getClass() == ClientWorld.class)
			worldType = WorldType.ClientWorld;	
		else
			worldType = WorldType.Unknown;
	}
	
	
	
	public static WorldWrapper getWorldWrapper(IWorld world)
	{
		//first we check if the biome has already been wrapped
		if(worldWrapperMap.containsKey(world) && worldWrapperMap.get(world) != null)
			return worldWrapperMap.get(world);
		
		
		//if it hasn't been created yet, we create it and save it in the map
		WorldWrapper worldWrapper = new WorldWrapper(world);
		worldWrapperMap.put(world, worldWrapper);
		
		//we return the newly created wrapper
		return worldWrapper;
	}
	
	public static void clearMap()
	{
		worldWrapperMap.clear();
	}
	
	public DimensionTypeWrapper getDimensionType()
	{
		return DimensionTypeWrapper.getDimensionTypeWrapper(world.dimensionType());
	}
	
	public int getBlockLight(BlockPosWrapper blockPos)
	{
		return world.getLightEngine().blockEngine.getLightValue(blockPos.getBlockPos());
	}
	
	public int getSkyLight(BlockPosWrapper blockPos)
	{
		return world.getLightEngine().skyEngine.getLightValue(blockPos.getBlockPos());
	}
	
	public BiomeWrapper getBiome(BlockPosWrapper blockPos)
	{
		return BiomeWrapper.getBiomeWrapper(world.getBiome(blockPos.getBlockPos()));
	}
	
	public IWorld getWorld()
	{
		return world;
	}
	
	public boolean hasCeiling()
	{
		return world.dimensionType().hasCeiling();
	}
	
	public boolean hasSkyLight()
	{
		return world.dimensionType().hasSkyLight();
	}
	
	public boolean isEmpty()
	{
		return world == null;
	}
	
	public int getHeight()
	{
		return world.getHeight();
	}
	
	/** @throws UnsupportedOperationException if the WorldWrapper isn't for a ServerWorld */
	public File getSaveFolder() throws UnsupportedOperationException
	{
		if (worldType != WorldType.ServerWorld)
			throw new UnsupportedOperationException("getSaveFolder can only be called for ServerWorlds.");
		
		ServerChunkProvider chunkSource = ((ServerWorld) world).getChunkSource();
		return chunkSource.dataStorage.dataFolder;
	}
	
	
	/** @throws UnsupportedOperationException if the WorldWrapper isn't for a ServerWorld */
	public ServerWorld getServerWorld() throws UnsupportedOperationException
	{
		if (worldType != WorldType.ServerWorld)
			throw new UnsupportedOperationException("getSaveFolder can only be called for ServerWorlds.");
		
		return (ServerWorld) world;
	}
	
	
}
