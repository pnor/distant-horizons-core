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

package com.seibel.lod.core.builders.lodBuilding;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.seibel.lod.core.enums.config.DistanceGenerationMode;
import com.seibel.lod.core.enums.config.HorizontalResolution;
import com.seibel.lod.core.objects.lod.DataPoint;
import com.seibel.lod.core.objects.lod.LodDimension;
import com.seibel.lod.core.objects.lod.LodRegion;
import com.seibel.lod.core.objects.lod.LodWorld;
import com.seibel.lod.core.util.ColorUtil;
import com.seibel.lod.core.util.DataPointUtil;
import com.seibel.lod.core.util.DetailDistanceUtil;
import com.seibel.lod.core.util.LevelPosUtil;
import com.seibel.lod.core.util.LodThreadFactory;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.util.SingletonHandler;
import com.seibel.lod.core.util.ThreadMapUtil;
import com.seibel.lod.core.wrapperInterfaces.IWrapperFactory;
import com.seibel.lod.core.wrapperInterfaces.block.AbstractBlockPosWrapper;
import com.seibel.lod.core.wrapperInterfaces.block.IBlockColorSingletonWrapper;
import com.seibel.lod.core.wrapperInterfaces.block.IBlockColorWrapper;
import com.seibel.lod.core.wrapperInterfaces.block.IBlockShapeWrapper;
import com.seibel.lod.core.wrapperInterfaces.chunk.AbstractChunkPosWrapper;
import com.seibel.lod.core.wrapperInterfaces.chunk.IChunkWrapper;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IBiomeWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IDimensionTypeWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IWorldWrapper;

/**
 * This object is in charge of creating Lod related objects.
 *
 * @author Cola
 * @author Leonardo Amato
 * @author James Seibel
 * @version 10-22-2021
 */
@SuppressWarnings("GrazieInspection") public class LodBuilder
{
	private static final IMinecraftWrapper MC = SingletonHandler.get(IMinecraftWrapper.class);
	private static final IBlockColorSingletonWrapper BLOCK_COLOR = SingletonHandler.get(IBlockColorSingletonWrapper.class); 
	private static final IWrapperFactory FACTORY = SingletonHandler.get(IWrapperFactory.class);
	
	/** If no blocks are found in the area in determineBottomPointForArea return this */
	public static final short DEFAULT_DEPTH = 0;
	/** If no blocks are found in the area in determineHeightPointForArea return this */
	public static final short DEFAULT_HEIGHT = 0;
	/** Minecraft's max light value */
	public static final short DEFAULT_MAX_LIGHT = 15;
	
	
	private final ExecutorService lodGenThreadPool = Executors.newSingleThreadExecutor(new LodThreadFactory(this.getClass().getSimpleName()));
	private final ILodConfigWrapperSingleton config = SingletonHandler.get(ILodConfigWrapperSingleton.class);
	
	
	
	/**
	 * How wide LodDimensions should be in regions <br>
	 * Is automatically set before the first frame in ClientProxy.
	 */
	public int defaultDimensionWidthInRegions = 0;
	
	//public static final boolean useExperimentalLighting = true;
	
	
	
	
	public LodBuilder()
	{
	
	}
	
	public void generateLodNodeAsync(IChunkWrapper chunk, LodWorld lodWorld, IDimensionTypeWrapper dim)
	{
		generateLodNodeAsync(chunk, lodWorld, dim, DistanceGenerationMode.FULL);
	}
	
	public void generateLodNodeAsync(IChunkWrapper chunk, LodWorld lodWorld, IDimensionTypeWrapper dim, DistanceGenerationMode generationMode)
	{
		if (lodWorld == null || lodWorld.getIsWorldNotLoaded())
			return;
		
		// don't try to create an LOD object
		// if for some reason we aren't
		// given a valid chunk object
		if (chunk == null)
			return;
		
		Thread thread = new Thread(() ->
		{
			//noinspection GrazieInspection
			try
			{
				// we need a loaded client world in order to
				// get the textures for blocks
				if (MC.getWrappedClientWorld() == null)
					return;
				
				// don't try to generate LODs if the user isn't in the world anymore
				// (this happens a lot when the user leaves a world/server)
				if (!MC.hasSinglePlayerServer() && !MC.connectedToServer())
					return;
				
				// make sure the dimension exists
				LodDimension lodDim;
				if (lodWorld.getLodDimension(dim) == null)
				{
					lodDim = new LodDimension(dim, lodWorld, defaultDimensionWidthInRegions);
					lodWorld.addLodDimension(lodDim);
				}
				else
				{
					lodDim = lodWorld.getLodDimension(dim);
				}
				generateLodNodeFromChunk(lodDim, chunk, new LodBuilderConfig(generationMode));
			}
			catch (IllegalArgumentException | NullPointerException e)
			{
				e.printStackTrace();
				// if the world changes while LODs are being generated
				// they will throw errors as they try to access things that no longer
				// exist.
			}
		});
		lodGenThreadPool.execute(thread);
	}
	
	/**
	 * Creates a LodNode for a chunk in the given world.
	 * @throws IllegalArgumentException thrown if either the chunk or world is null.
	 */
	public void generateLodNodeFromChunk(LodDimension lodDim, IChunkWrapper chunk) throws IllegalArgumentException
	{
		generateLodNodeFromChunk(lodDim, chunk, new LodBuilderConfig());
	}
	
	/**
	 * Creates a LodNode for a chunk in the given world.
	 * @throws IllegalArgumentException thrown if either the chunk or world is null.
	 */
	public void generateLodNodeFromChunk(LodDimension lodDim, IChunkWrapper chunk, LodBuilderConfig config)
			throws IllegalArgumentException
	{
		if (chunk == null)
			throw new IllegalArgumentException("generateLodFromChunk given a null chunk");
		
		int startX;
		int startZ;
		
		
		LodRegion region = lodDim.getRegion(chunk.getPos().getRegionX(), chunk.getPos().getRegionZ());
		if (region == null)
			return;
		
		// this happens if a LOD is generated after the user leaves the world.
		if (MC.getWrappedClientWorld() == null)
			return;
		
		// determine how many LODs to generate horizontally
		byte minDetailLevel = region.getMinDetailLevel();
		HorizontalResolution detail = DetailDistanceUtil.getLodGenDetail(minDetailLevel);
		
		
		// determine how many LODs to generate vertically
		//VerticalQuality verticalQuality = LodConfig.CLIENT.graphics.qualityOption.verticalQuality.get();
		byte detailLevel = detail.detailLevel;
		
		
		// generate the LODs
		int posX;
		int posZ;
		for (int i = 0; i < detail.dataPointLengthCount * detail.dataPointLengthCount; i++)
		{
			startX = detail.startX[i];
			startZ = detail.startZ[i];
			
			DataPoint[] data;
			DataPoint[] dataToMergeVertical = createVerticalDataToMerge(detail, chunk, config, startX, startZ);
			data = DataPointUtil.mergeMultiData(dataToMergeVertical, DataPointUtil.WORLD_HEIGHT / 2 + 1, DetailDistanceUtil.getMaxVerticalData(detailLevel));
			
			
			//lodDim.clear(detailLevel, posX, posZ);
			if (data != null && data.length != 0)
			{
				posX = LevelPosUtil.convert((byte) 0, chunk.getPos().getX() * 16 + startX, detail.detailLevel);
				posZ = LevelPosUtil.convert((byte) 0, chunk.getPos().getZ() * 16 + startZ, detail.detailLevel);
				lodDim.addVerticalData(detailLevel, posX, posZ, data, false);
			}
		}
		lodDim.updateData(LodUtil.CHUNK_DETAIL_LEVEL, chunk.getPos().getX(), chunk.getPos().getZ());
	}
	
	/** creates a vertical DataPoint */
	private DataPoint[] createVerticalDataToMerge(HorizontalResolution detail, IChunkWrapper chunk, LodBuilderConfig config, int startX, int startZ)
	{
		// equivalent to 2^detailLevel
		int size = 1 << detail.detailLevel;
		
		DataPoint[] dataToMerge = ThreadMapUtil.getBuilderVerticalArray(detail.detailLevel);
		int verticalData = DataPointUtil.WORLD_HEIGHT / 2 + 1;
		
		AbstractChunkPosWrapper chunkPos = chunk.getPos();
		int height;
		int depth;
		int color;
		int light;
		int lightSky;
		int lightBlock;
		int generation = config.distanceGenerationMode.complexity;
		
		int xRel;
		int zRel;
		int xAbs;
		int yAbs;
		int zAbs;
		boolean hasCeiling = MC.getWrappedClientWorld().getDimensionType().hasCeiling();
		boolean hasSkyLight = MC.getWrappedClientWorld().getDimensionType().hasSkyLight();
		boolean isDefault;
		AbstractBlockPosWrapper blockPos = FACTORY.createBlockPos();
		int index;
		
		for (index = 0; index < size * size; index++)
		{
			xRel = startX + index % size;
			zRel = startZ + index / size;
			xAbs = chunkPos.getMinBlockX() + xRel;
			zAbs = chunkPos.getMinBlockZ() + zRel;
			
			//Calculate the height of the lod
			yAbs = DataPointUtil.WORLD_HEIGHT - DataPointUtil.VERTICAL_OFFSET + 1;
			int count = 0;
			boolean topBlock = true;
			while (yAbs > 0)
			{
				height = determineHeightPointFrom(chunk, config, xRel, yAbs, zRel, blockPos);
				
				// If the lod is at the default height, it must be void data
				if (height == DEFAULT_HEIGHT)
				{
					if (topBlock)
						dataToMerge[index * verticalData] = DataPointUtil.createVoidDataPoint(generation);
					break;
				}
				
				yAbs = height - 1;
				// We search light on above air block
				depth = determineBottomPointFrom(chunk, config, xRel, yAbs, zRel, blockPos);
				if (hasCeiling && topBlock)
				{
					yAbs = depth;
					blockPos.set(xAbs, yAbs, zAbs);
					light = getLightValue(chunk, blockPos, true, hasSkyLight, true);
					color = generateLodColor(chunk, config, xAbs, yAbs, zAbs, blockPos);
					blockPos.set(xAbs, yAbs - 1, zAbs);
				}
				else
				{
					blockPos.set(xAbs, yAbs, zAbs);
					light = getLightValue(chunk, blockPos, hasCeiling, hasSkyLight, topBlock);
					color = generateLodColor(chunk, config, xRel, yAbs, zRel, blockPos);
					blockPos.set(xAbs, yAbs + 1, zAbs);
				}
				lightBlock = light & 0b1111;
				lightSky = (light >> 4) & 0b1111;
				isDefault = ((light >> 8)) == 1;
				
				dataToMerge[index * verticalData + count] = DataPointUtil.createDataPoint(height - DataPointUtil.VERTICAL_OFFSET, depth - DataPointUtil.VERTICAL_OFFSET, color, lightSky, lightBlock, generation, isDefault);
				topBlock = false;
				yAbs = depth - 1;
				count++;
			}
		}
		return dataToMerge;
	}
	
	/**
	 * Find the lowest valid point from the bottom.
	 * Used when creating a vertical LOD.
	 */
	private short determineBottomPointFrom(IChunkWrapper chunk, LodBuilderConfig config, int xAbs, int yAbs, int zAbs, AbstractBlockPosWrapper blockPos)
	{
		short depth = DEFAULT_DEPTH;
		
		for (int y = yAbs; y >= 0; y--)
		{
			blockPos.set(xAbs, y, zAbs);
			if (!isLayerValidLodPoint(chunk, blockPos))
			{
				depth = (short) (y + 1);
				break;
			}
		}
		return depth;
	}
	
	/** Find the highest valid point from the Top */
	private short determineHeightPointFrom(IChunkWrapper chunk, LodBuilderConfig config, int xAbs, int yAbs, int zAbs, AbstractBlockPosWrapper blockPos)
	{
		short height = DEFAULT_HEIGHT;
		if (config.useHeightmap)
			height = (short) chunk.getHeightMapValue(xAbs, zAbs);
		else
		{
			for (int y = yAbs; y >= 0; y--)
			{
				blockPos.set(xAbs, y, zAbs);
				if (isLayerValidLodPoint(chunk, blockPos))
				{
					height = (short) (y + 1);
					break;
				}
			}
		}
		return height;
	}
	
	
	
	// =====================//
	// constructor helpers //
	// =====================//
	
	/**
	 * Generate the color for the given chunk using biome water color, foliage
	 * color, and grass color.
	 */
	private int generateLodColor(IChunkWrapper chunk, LodBuilderConfig builderConfig, int xRel, int yAbs, int zRel, AbstractBlockPosWrapper blockPos)
	{
		int colorInt;
		if (builderConfig.useBiomeColors)
		{
			// I have no idea why I need to bit shift to the right, but
			// if I don't the biomes don't show up correctly.
			colorInt = chunk.getBiome(xRel, yAbs, zRel).getColorForBiome(xRel, zRel);
		}
		else
		{
			blockPos.set(chunk.getPos().getMinBlockX() + xRel, yAbs, chunk.getPos().getMinBlockZ() + zRel);
			colorInt = getColorForBlock(chunk, blockPos);
			
			// if we are skipping non-full and non-solid blocks that means we ignore
			// snow, flowers, etc. Get the above block so we can still get the color
			// of the snow, flower, etc. that may be above this block
			int aboveColorInt = 0;
			if (config.client().worldGenerator().getBlocksToAvoid().nonFull || config.client().worldGenerator().getBlocksToAvoid().noCollision)
			{
				blockPos.set(chunk.getPos().getMinBlockX() + xRel, yAbs + 1, chunk.getPos().getMinBlockZ() + zRel);
				aboveColorInt = getColorForBlock(chunk, blockPos);
			}
			
			//if (colorInt == 0 && yAbs > 0)
			// if this block is invisible, check the block below it
			//	colorInt = generateLodColor(chunk, config, xRel, yAbs - 1, zRel, blockPos);
			
			// override this block's color if there was a block above this
			// and we were avoiding non-full/non-solid blocks
			if (aboveColorInt != 0)
				colorInt = aboveColorInt;
		}
		
		return colorInt;
	}
	
	/** Gets the light value for the given block position */
	private int getLightValue(IChunkWrapper chunk, AbstractBlockPosWrapper blockPos, boolean hasCeiling, boolean hasSkyLight, boolean topBlock)
	{
		int skyLight = 0;
		int blockLight;
		// 1 means the lighting is a guess
		int isDefault = 0;
		
		IWorldWrapper world = MC.getWrappedServerWorld();
		
		int blockBrightness = chunk.getEmittedBrightness(blockPos);
		// get the air block above or below this block
		if (hasCeiling && topBlock)
			blockPos.set(blockPos.getX(), blockPos.getY() - 1, blockPos.getZ());
		else
			blockPos.set(blockPos.getX(), blockPos.getY() + 1, blockPos.getZ());
		
		
		
		if (world != null)
		{
			// server world sky light (always accurate)
			blockLight = world.getBlockLight(blockPos);
			if (topBlock && !hasCeiling && hasSkyLight)
				skyLight = DEFAULT_MAX_LIGHT;
			else
			{
				if (hasSkyLight)
					skyLight = world.getSkyLight(blockPos);
				//else
				//	skyLight = 0;
			}
			if (!topBlock && skyLight == 15)
			{
				// we are on predicted terrain, and we don't know what the light here is,
				// lets just take a guess
				if (blockPos.getY() >= MC.getWrappedClientWorld().getSeaLevel() - 5)
				{
					skyLight = 12;
					isDefault = 1;
				}
				else
					skyLight = 0;
			}
		}
		else
		{
			world = MC.getWrappedClientWorld();
			if (world==null)
			{
				blockLight = 0;
				skyLight = 12;
				isDefault = 1;
			}
			else
			{
				// client world sky light (almost never accurate)
				blockLight = world.getBlockLight(blockPos);
				// estimate what the lighting should be
				if (hasSkyLight || !hasCeiling)
				{
					if (topBlock)
						skyLight = DEFAULT_MAX_LIGHT;
					else
					{
						if (hasSkyLight)
							skyLight = world.getSkyLight(blockPos);
						//else
						//	skyLight = 0;
						if (!chunk.isLightCorrect() && (skyLight == 0 || skyLight == 15))
						{
							// we don't know what the light here is,
							// lets just take a guess
							if (blockPos.getY() >= MC.getWrappedClientWorld().getSeaLevel() - 5)
							{
								skyLight = 12;
								isDefault = 1;
							}
							else
								skyLight = 0;
						}
					}
				}
			}
		}
		
		blockLight = LodUtil.clamp(0, Math.max(blockLight, blockBrightness), DEFAULT_MAX_LIGHT);
		
		return blockLight + (skyLight << 4) + (isDefault << 8);
	}
	
	/** Returns a color int for the given block. */
	private int getColorForBlock(IChunkWrapper chunk, AbstractBlockPosWrapper blockPos)
	{
		int colorOfBlock;
		int colorInt;
		
		int xRel = blockPos.getX() - chunk.getPos().getMinBlockX();
		int zRel = blockPos.getZ() - chunk.getPos().getMinBlockZ();
		//int x = blockPos.getX();
		int y = blockPos.getY();
		//int z = blockPos.getZ();
		
		IBlockColorWrapper blockColorWrapper;
		IBlockShapeWrapper blockShapeWrapper = chunk.getBlockShapeWrapper(blockPos);
		
		if (chunk.isWaterLogged(blockPos))
			blockColorWrapper = BLOCK_COLOR.getWaterColor();
		else
			blockColorWrapper = chunk.getBlockColorWrapper(blockPos);
		
		if (blockShapeWrapper.isToAvoid())
			return 0;
		
		colorOfBlock = blockColorWrapper.getColor();
		
		
		if (blockColorWrapper.hasTint())
		{
			IBiomeWrapper biome = chunk.getBiome(xRel, y, zRel);
			int tintValue;
			if (blockColorWrapper.hasGrassTint())
				// grass and green plants
				tintValue = biome.getGrassTint(0,0);
			else if (blockColorWrapper.hasFolliageTint())
				tintValue = biome.getFolliageTint();
			else
				//we can reintroduce this with the wrappers
				tintValue = biome.getWaterTint();
			
			colorInt = ColorUtil.multiplyRGBcolors(tintValue | 0xFF000000, colorOfBlock);
		}
		else
			colorInt = colorOfBlock;
		return colorInt;
	}
	
	
	/** Is the block at the given blockPos a valid LOD point? */
	private boolean isLayerValidLodPoint(IChunkWrapper chunk, AbstractBlockPosWrapper blockPos)
	{
		if (chunk.isWaterLogged(blockPos))
			return true;
		
		boolean nonFullAvoidance = config.client().worldGenerator().getBlocksToAvoid().nonFull;
		boolean noCollisionAvoidance = config.client().worldGenerator().getBlocksToAvoid().noCollision;
		
		IBlockShapeWrapper block = chunk.getBlockShapeWrapper(blockPos);
		return !block.isToAvoid()
					   && !(nonFullAvoidance && block.isNonFull())
					   && !(noCollisionAvoidance && block.hasNoCollision());
		
	}
}
