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

import com.seibel.lod.core.api.ClientApi;
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
	public boolean generateLodNodeFromChunk(LodDimension lodDim, IChunkWrapper chunk, LodBuilderConfig config, boolean override)
			throws IllegalArgumentException
	{
		//config.distanceGenerationMode = DistanceGenerationMode.FULL;
		
		//long executeTime = System.currentTimeMillis();
		if (chunk == null)
			throw new IllegalArgumentException("generateLodFromChunk given a null chunk");
		
		LodRegion region = lodDim.getRegion(chunk.getRegionPosX(), chunk.getRegionPosZ());
		if (region == null)
			return false;
		
		// this happens if a LOD is generated after the user leaves the world.
		if (MC.getWrappedClientWorld() == null)
			return false;
		
		if (!chunk.isLightCorrect()) return false;
		
		// determine how many LODs to generate vertically
		//VerticalQuality verticalQuality = LodConfig.CLIENT.graphics.qualityOption.verticalQuality.get();
		
		// generate the LODs
		int maxVerticalData = DetailDistanceUtil.getMaxVerticalData((byte)0);
		long[] data = new long[maxVerticalData*16*16];
		for (int i = 0; i < 16*16; i++)
		{
			int subX = i/16;
			int subZ = i%16;
			writeVerticalData(data, i*maxVerticalData, maxVerticalData, chunk, config, subX, subZ);
			//if (DataPointUtil.isVoid(data[i*maxVerticalData]))
			//	ClientApi.LOGGER.debug("Datapoint is Void: {}, {}", chunk.getMinX()+subX, chunk.getMinZ()+subZ);
			if (!DataPointUtil.doesItExist(data[i*maxVerticalData]))
				throw new RuntimeException("Datapoint does not exist at "+ chunk.getMinX()+subX +", "+ chunk.getMinZ()+subZ);
			if (DataPointUtil.getGenerationMode(data[i*maxVerticalData]) != config.distanceGenerationMode.complexity)
				throw new RuntimeException("Datapoint invalid at "+ chunk.getMinX()+subX +", "+ chunk.getMinZ()+subZ);
		}
		if (!chunk.isLightCorrect()) return false;
		
		region.isWriting++;
		try {
			if (region.getMinDetailLevel()!= 0) {
				LodRegion newRegion = lodDim.getRegionFromFile(region, (byte)0, region.getVerticalQuality());
				if (region!=newRegion)
					throw new RuntimeException();
			}
			//ClientApi.LOGGER.info("Generate chunk: {}, {} ({}, {}) at genMode {}",
			//		chunk.getChunkPosX(), chunk.getChunkPosZ(), chunk.getMinX(), chunk.getMinZ(), config.distanceGenerationMode);
			region.addChunkOfData((byte)0, chunk.getMinX(), chunk.getMinZ(), 16, 16, data, maxVerticalData, override);
			region.regenerateLodFromArea((byte)0, chunk.getMinX(), chunk.getMinZ(), 16, 16);
			lodDim.regenDimensionBuffers = true;
			
			if (!region.doesDataExist((byte)0, chunk.getMinX(), chunk.getMinZ(), config.distanceGenerationMode))
				throw new RuntimeException();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			region.isWriting--;
		}
		
		return true;
		//executeTime = System.currentTimeMillis() - executeTime;
		//if (executeTime > 0) ClientApi.LOGGER.info("generateLodNodeFromChunk level: " + detailLevel + " time ms: " + executeTime);
	}

	/** creates a vertical DataPoint */
	private void writeVerticalData(long[] data, int dataOffset, int maxVerticalData,
			IChunkWrapper chunk, LodBuilderConfig config, int chunkSubPosX, int chunkSubPosZ)
	{

		int totalVerticalData = (chunk.getHeight());
		long[] dataToMerge = new long[totalVerticalData];
		
		boolean hasCeiling = MC.getWrappedClientWorld().getDimensionType().hasCeiling();
		boolean hasSkyLight = MC.getWrappedClientWorld().getDimensionType().hasSkyLight();
		int generation = config.distanceGenerationMode.complexity;
		int count = 0;
		// FIXME: This yAbs is just messy!
		int x = chunk.getMinX() + chunkSubPosX;
		int z = chunk.getMinZ() + chunkSubPosZ;
		int y = chunk.getMaxY(x, z);
		
		boolean topBlock = true;
		if (y <= chunk.getMinBuildHeight())
			data[dataOffset] = DataPointUtil.createVoidDataPoint(generation); 
		while (y > chunk.getMinBuildHeight()) {
			int height = determineHeightPointFrom(chunk, config, x, y, z);
			// If the lod is at the default height, it must be void data
			if (height <= chunk.getMinBuildHeight())
				break;
			y = height - 1;
			// We search light on above air block
			int depth = determineBottomPointFrom(chunk, config, x, y, z,
					count < this.config.client().graphics().quality().getVerticalQuality().maxConnectedLods
					&& !hasCeiling);
			if (hasCeiling && topBlock)
				y = depth;
			int light = getLightValue(chunk, x, y, z, hasCeiling, hasSkyLight, topBlock);
			int color = generateLodColor(chunk, config, x, y, z);
			int lightBlock = light & 0b1111;
			int lightSky = (light >> 4) & 0b1111;
			boolean isDefault = ((light >> 8)) == 1;
			dataToMerge[count] = DataPointUtil.createDataPoint(height-chunk.getMinBuildHeight(), depth-chunk.getMinBuildHeight(),
					color, lightSky, lightBlock, generation, isDefault);
			topBlock = false;
			y = depth - 1;
			count++;
		}
		long[] result = DataPointUtil.mergeMultiData(dataToMerge, totalVerticalData, maxVerticalData);
		if (result.length != maxVerticalData) throw new ArrayIndexOutOfBoundsException();
		System.arraycopy(result, 0, data, dataOffset, maxVerticalData);
	}
	
	/**
	 * Find the lowest valid point from the bottom.
	 * Used when creating a vertical LOD.
	 */
	private int determineBottomPointFrom(IChunkWrapper chunk, LodBuilderConfig config, int xAbs, int yAbs, int zAbs, boolean strictEdge)
	{
		int depth = chunk.getMinBuildHeight();
		
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
		
		for (int y = yAbs - 1; y >= chunk.getMinBuildHeight(); y--)
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
	private int determineHeightPointFrom(IChunkWrapper chunk, LodBuilderConfig config, int xAbs, int yAbs, int zAbs)
	{
		//TODO find a way to skip bottom of the world
		int height = chunk.getMinBuildHeight();
		for (int y = yAbs; y >= chunk.getMinBuildHeight(); y--)
		{
			if (isLayerValidLodPoint(chunk, xAbs, y, zAbs))
			{
				height = (y + 1);
				break;
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
