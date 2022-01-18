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
import com.seibel.lod.core.objects.lod.LodDimension;
import com.seibel.lod.core.objects.lod.LodRegion;
import com.seibel.lod.core.objects.lod.LodWorld;
import com.seibel.lod.core.util.ColorUtil;
import com.seibel.lod.core.util.DataPointUtil;
import com.seibel.lod.core.util.DetailDistanceUtil;
import com.seibel.lod.core.util.LodThreadFactory;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.util.SingletonHandler;
import com.seibel.lod.core.util.ThreadMapUtil;
import com.seibel.lod.core.wrapperInterfaces.block.IBlockColorSingletonWrapper;
import com.seibel.lod.core.wrapperInterfaces.block.IBlockColorWrapper;
import com.seibel.lod.core.wrapperInterfaces.block.IBlockShapeWrapper;
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
 * @version 12-11-2021
 */
@SuppressWarnings("GrazieInspection")
public class LodBuilder
{
	private static final IMinecraftWrapper MC = SingletonHandler.get(IMinecraftWrapper.class);
	private static final IBlockColorSingletonWrapper BLOCK_COLOR = SingletonHandler.get(IBlockColorSingletonWrapper.class);
	
	
	/** This cannot be final! Different world have different height, and in menu, this causes Null Exceptions*/
	//public static final short MIN_WORLD_HEIGHT = MC.getWrappedClientWorld().getMinHeight();
	public static short MIN_WORLD_HEIGHT = 0; // Currently modified in EventApi.onWorldLoaded(...)
	/** Minecraft's max light value */
	public static final short DEFAULT_MAX_LIGHT = 15;
	
	//public static final ExecutorService lodGenThreadPool = Executors.newFixedThreadPool(8, new ThreadFactoryBuilder().setNameFormat("Lod-Builder-%d").build());

	private final ExecutorService lodGenThreadPool = Executors.newSingleThreadExecutor(new LodThreadFactory(this.getClass().getSimpleName()));
	private final ILodConfigWrapperSingleton config = SingletonHandler.get(ILodConfigWrapperSingleton.class);
	
	
	
	/**
	 * How wide LodDimensions should be in regions <br>
	 * Is automatically set before the first frame in ClientProxy.
	 */
	public int defaultDimensionWidthInRegions = 1;
	
	//public static final boolean useExperimentalLighting = true;
	
	
	
	
	public LodBuilder()
	{
	
	}
	
	public void generateLodNodeAsync(IChunkWrapper chunk, LodWorld lodWorld, IDimensionTypeWrapper dim)
	{
		// Block change event
		generateLodNodeAsync(chunk, lodWorld, dim, DistanceGenerationMode.FULL, true, ()->{},
				()->{generateLodNodeAsync(chunk,lodWorld,dim);});
	}
	
	public void generateLodNodeAsync(IChunkWrapper chunk, LodWorld lodWorld, IDimensionTypeWrapper dim,
			DistanceGenerationMode generationMode, boolean override, Runnable endCallback, Runnable retryCallback)
	{
		if (lodWorld == null || lodWorld.getIsWorldNotLoaded()) {
			endCallback.run();
			return;
		}
		// don't try to create an LOD object
		// if for some reason we aren't
		// given a valid chunk object
		if (chunk == null) {
			endCallback.run();
			return;
		}
		
		Thread thread = new Thread(() ->
		{
			boolean retryNeeded = false;
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
				// if not, it prob means that player left
				LodDimension lodDim = lodWorld.getLodDimension(dim);
				if (lodDim == null) return;
				
				retryNeeded = !generateLodNodeFromChunk(lodDim, chunk, new LodBuilderConfig(generationMode), override);
			}
			catch (RuntimeException e)
			{
				e.printStackTrace();
			//	// if the world changes while LODs are being generated
			//	// they will throw errors as they try to access things that no longer
			//	// exist.
			} finally {
				if (!retryNeeded)
					endCallback.run();
				else
					retryCallback.run();
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
		generateLodNodeFromChunk(lodDim, chunk, new LodBuilderConfig(), false);
	}
	
	/**
	 * Creates a LodNode for a chunk in the given world.
	 * @throws IllegalArgumentException thrown if either the chunk or world is null.
	 */
	public boolean generateLodNodeFromChunk(LodDimension lodDim, IChunkWrapper chunk, LodBuilderConfig config, boolean override)
			throws IllegalArgumentException
	{
		//long executeTime = System.currentTimeMillis();
		if (chunk == null)
			throw new IllegalArgumentException("generateLodFromChunk given a null chunk");
		
		int startX;
		int startZ;
		
		
		LodRegion region = lodDim.getRegion(chunk.getRegionPosX(), chunk.getRegionPosZ());
		if (region == null)
			return false;
		
		// this happens if a LOD is generated after the user leaves the world.
		if (MC.getWrappedClientWorld() == null)
			return false;
		
		// determine how many LODs to generate vertically
		//VerticalQuality verticalQuality = LodConfig.CLIENT.graphics.qualityOption.verticalQuality.get();
		region.isWriting++;
		try {
			LodRegion newRegion = lodDim.getRegionFromFile(region, (byte)0, region.getGenerationMode(), region.getVerticalQuality());
			assert(region==newRegion);
			
			// generate the LODs
			int posX;
			int posZ;
			for (int i = 0; i < 16*16; i++)
			{
				startX = i/16;
				startZ = i%16;
				
				long[] data;
				long[] dataToMergeVertical = createVerticalDataToMerge((byte)0, chunk, config, startX, startZ);
				data = DataPointUtil.mergeMultiData(dataToMergeVertical, DataPointUtil.WORLD_HEIGHT / 2 + 1, DetailDistanceUtil.getMaxVerticalData((byte)0));
				
				if (data != null && data.length != 0)
				{
					posX = chunk.getChunkPosX() * 16 + startX;
					posZ = chunk.getChunkPosZ() * 16 + startZ;
					if (region.addVerticalData((byte)0, posX, posZ, data, override))
						region.updateArea((byte)0, posX, posZ);
				}
			}
		} finally {
			region.isWriting--;
		}
		return true;
		//executeTime = System.currentTimeMillis() - executeTime;
		//if (executeTime > 0) ClientApi.LOGGER.info("generateLodNodeFromChunk level: " + detailLevel + " time ms: " + executeTime);
	}
	
	/** creates a vertical DataPoint */
	private long[] createVerticalDataToMerge(byte detail, IChunkWrapper chunk, LodBuilderConfig config, int startX, int startZ)
	{
		// equivalent to 2^detailLevel
		int size = 1 << detail;
		
		long[] dataToMerge = ThreadMapUtil.getBuilderVerticalArray(detail);
		int verticalData = DataPointUtil.WORLD_HEIGHT / 2 + 1;
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
		int index;
		
		for (index = 0; index < size * size; index++)
		{
			xRel = startX + index % size;
			zRel = startZ + index / size;
			xAbs = chunk.getMinX() + xRel;
			zAbs = chunk.getMinZ() + zRel;
			
			//Calculate the height of the lod
			yAbs = chunk.getMaxY(xRel,zRel) - MIN_WORLD_HEIGHT;
			int count = 0;
			boolean topBlock = true;
			if (yAbs <= 0)
				dataToMerge[index * verticalData] = DataPointUtil.createVoidDataPoint(generation);
			while (yAbs > 0)
			{
				height = determineHeightPointFrom(chunk, config, xAbs, yAbs, zAbs);
				
				// If the lod is at the default height, it must be void data
				if (height == 0)
					break;
				
				yAbs = height - 1;
				// We search light on above air block
				depth = determineBottomPointFrom(chunk, config, xAbs, yAbs, zAbs, count < this.config.client().graphics().quality().getVerticalQuality().maxConnectedLods && !hasCeiling);
				if (hasCeiling && topBlock)
					yAbs = depth;
				light = getLightValue(chunk, xAbs,yAbs + MIN_WORLD_HEIGHT, zAbs, hasCeiling, hasSkyLight, topBlock);
				color = generateLodColor(chunk, config, xAbs, yAbs, zAbs);
				lightBlock = light & 0b1111;
				lightSky = (light >> 4) & 0b1111;
				isDefault = ((light >> 8)) == 1;
				
				dataToMerge[index * verticalData + count] = DataPointUtil.createDataPoint(height, depth, color, lightSky, lightBlock, generation, isDefault);
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
	private short determineBottomPointFrom(IChunkWrapper chunk, LodBuilderConfig config, int xAbs, int yAbs, int zAbs, boolean strictEdge)
	{
		short depth = 0;
		
		int colorOfBlock = 0;
		if (strictEdge)
		{
			IBlockShapeWrapper block = chunk.getBlockShapeWrapper(xAbs, yAbs + 1, zAbs);
			if (block != null && !block.isToAvoid()
					&& ((this.config.client().worldGenerator().getBlocksToAvoid().nonFull && block.isNonFull())
					|| (this.config.client().worldGenerator().getBlocksToAvoid().noCollision && block.hasNoCollision())))
			{
				int aboveColorInt = chunk.getBlockColorWrapper(xAbs, yAbs + 1, zAbs).getColor();
				if (aboveColorInt != 0)
					colorOfBlock = aboveColorInt;
				else
					colorOfBlock = chunk.getBlockColorWrapper(xAbs, yAbs, zAbs).getColor();
			}
		}
		
		for (int y = yAbs - 1; y >= 0; y--)
		{
			
			if (!isLayerValidLodPoint(chunk, xAbs, y, zAbs)
				|| (strictEdge && colorOfBlock != chunk.getBlockColorWrapper(xAbs, y, zAbs).getColor()))
			{
				depth = (short) (y + 1);
				break;
			}
		}
		return depth;
	}
	
	/** Find the highest valid point from the Top */
	private short determineHeightPointFrom(IChunkWrapper chunk, LodBuilderConfig config, int xAbs, int yAbs, int zAbs)
	{
		//TODO find a way to skip bottom of the world
		short height = 0;
		if (config.useHeightmap)
			height = (short) chunk.getHeightMapValue(xAbs, zAbs);
		else
		{
			for (int y = yAbs; y >= 0; y--)
			{
				if (isLayerValidLodPoint(chunk, xAbs, y, zAbs))
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
	private int generateLodColor(IChunkWrapper chunk, LodBuilderConfig builderConfig, int x, int y, int z)
	{
		int colorInt;
		if (builderConfig.useBiomeColors)
		{
			// I have no idea why I need to bit shift to the right, but
			// if I don't the biomes don't show up correctly.
			colorInt = chunk.getBiome(x, y, z).getColorForBiome(x, z);
		}
		else
		{
			colorInt = getColorForBlock(chunk, x, y, z);
			
			// if we are skipping non-full and non-solid blocks that means we ignore
			// snow, flowers, etc. Get the above block so we can still get the color
			// of the snow, flower, etc. that may be above this block
			int aboveColorInt = 0;
			IBlockShapeWrapper block = chunk.getBlockShapeWrapper(x, y + 1, z);
			if (block != null && ((config.client().worldGenerator().getBlocksToAvoid().nonFull && block.isNonFull())
					|| (config.client().worldGenerator().getBlocksToAvoid().noCollision && block.hasNoCollision())))
				aboveColorInt = getColorForBlock(chunk, x, y + 1, z);
			
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
	private int getLightValue(IChunkWrapper chunk, int x, int y, int z, boolean hasCeiling, boolean hasSkyLight, boolean topBlock)
	{
		int skyLight;
		int blockLight;
		
		int blockBrightness = chunk.getEmittedBrightness(x, y, z);
		// get the air block above or below this block
		if (hasCeiling && topBlock)
			y--;
		else
			y++;
		
		blockLight = chunk.getBlockLight(x, y, z);
		skyLight = hasSkyLight ? chunk.getSkyLight(x, y, z) : 0;
		
		if (blockLight == -1 || skyLight == -1)
		{
			
			IWorldWrapper world = MC.getWrappedServerWorld();
			
			if (world != null)
			{
				// server world sky light (always accurate)
				blockLight = world.getBlockLight(x, y, z);
				
				if (topBlock && !hasCeiling && hasSkyLight)
					skyLight = DEFAULT_MAX_LIGHT;
				else
					skyLight = hasSkyLight ? world.getSkyLight(x, y, z) : 0;
				
				if (!topBlock && skyLight == 15)
				{
					// we are on predicted terrain, and we don't know what the light here is,
					// lets just take a guess
					skyLight = 12;
				}
			}
			else
			{
				world = MC.getWrappedClientWorld();
				if (world == null)
				{
					blockLight = 0;
					skyLight = 12;
				}
				else
				{
					// client world sky light (almost never accurate)
					blockLight = world.getBlockLight(x, y, z);
					// estimate what the lighting should be
					if (hasSkyLight || !hasCeiling)
					{
						if (topBlock)
							skyLight = DEFAULT_MAX_LIGHT;
						else
						{
							if (hasSkyLight)
								skyLight = world.getSkyLight(x, y, z);
							//else
							//	skyLight = 0;
							if (!chunk.isLightCorrect() && (skyLight == 0 || skyLight == 15))
							{
								// we don't know what the light here is,
								// lets just take a guess
								skyLight = 12;
							}
						}
					}
				}
			}
		}
		
		blockLight = LodUtil.clamp(0, Math.max(blockLight, blockBrightness), DEFAULT_MAX_LIGHT);
		return blockLight + (skyLight << 4);
	}
	
	/** Returns a color int for the given block. */
	private int getColorForBlock(IChunkWrapper chunk, int x, int y, int z)
	{
		int colorOfBlock;
		int colorInt;
		
		IBlockShapeWrapper blockShapeWrapper = chunk.getBlockShapeWrapper(x, y, z);
		
		if (blockShapeWrapper == null || blockShapeWrapper.isToAvoid())
			return 0;
		
		IBlockColorWrapper blockColorWrapper;
		
		if (chunk.isWaterLogged(x, y, z))
			blockColorWrapper = BLOCK_COLOR.getWaterColor();
		else
			blockColorWrapper = chunk.getBlockColorWrapper(x, y, z);
		
		
		
		colorOfBlock = blockColorWrapper.getColor();
		
		
		if (blockColorWrapper.hasTint())
		{
			IBiomeWrapper biome = chunk.getBiome(x, y, z);
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
	private boolean isLayerValidLodPoint(IChunkWrapper chunk, int x, int y, int z)
	{
		if (chunk.isWaterLogged(x, y, z))
			return true;
		
		boolean nonFullAvoidance = config.client().worldGenerator().getBlocksToAvoid().nonFull;
		boolean noCollisionAvoidance = config.client().worldGenerator().getBlocksToAvoid().noCollision;
		
		IBlockShapeWrapper block = chunk.getBlockShapeWrapper(x, y, z);
		
		return block != null
					&& !block.isToAvoid()
					&& !(nonFullAvoidance && block.isNonFull())
					&& !(noCollisionAvoidance && block.hasNoCollision());
		
	}
}
