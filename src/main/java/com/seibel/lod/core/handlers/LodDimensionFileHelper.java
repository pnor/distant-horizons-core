package com.seibel.lod.core.handlers;

import com.seibel.lod.core.api.ApiShared;
import com.seibel.lod.core.builders.lodBuilding.LodBuilder;
import com.seibel.lod.core.builders.lodBuilding.LodBuilderConfig;
import com.seibel.lod.core.enums.config.DistanceGenerationMode;
import com.seibel.lod.core.enums.config.VerticalQuality;
import com.seibel.lod.core.handlers.dependencyInjection.SingletonHandler;
import com.seibel.lod.core.objects.lod.LodDimension;
import com.seibel.lod.core.objects.lod.LodRegion;
import com.seibel.lod.core.objects.lod.RegionPos;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.wrapperInterfaces.chunk.AbstractChunkPosWrapper;
import com.seibel.lod.core.wrapperInterfaces.chunk.IChunkWrapper;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IDimensionTypeWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IWorldWrapper;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * Used to guess the world folder for the player's current dimension.
 * @author James Seibel
 * @version 3-17-2022
 */
public class LodDimensionFileHelper
{
	private static final IMinecraftClientWrapper MC = SingletonHandler.get(IMinecraftClientWrapper.class);
	private static final ILodConfigWrapperSingleton CONFIG = SingletonHandler.get(ILodConfigWrapperSingleton.class);
	
	/** Increasing this will increase accuracy but increase calculation time */
	private static final VerticalQuality VERTICAL_QUALITY_TO_TEST_WITH = VerticalQuality.LOW;
	
	
	/**
	 * Currently this method checks a single chunk (where the player is)
	 * and compares it against the same chunk position in the other dimension worlds to
	 * guess which world the player is in.
	 * @return the new or existing folder for this dimension, null if there was a problem
	 * @throws IOException if the folder doesn't exist or can't be accessed
	 */
	public static File determineSaveFolder() throws IOException
	{
		// relevant positions
		AbstractChunkPosWrapper playerChunkPos = MC.getPlayerChunkPos();
		int startingBlockPosX = playerChunkPos.getMinBlockX();
		int startingBlockPosZ = playerChunkPos.getMinBlockZ();
		RegionPos playerRegionPos = new RegionPos(MC.getPlayerChunkPos());
		
		// chunk from the newly loaded dimension
		IChunkWrapper newlyLoadedChunk = MC.getWrappedClientWorld().tryGetChunk(playerChunkPos);
		// check if this chunk is valid to test
		if (!LodDimensionFileHelper.CanDetermineDimensionFolder(newlyLoadedChunk))
			return null;
		
		// create a temporary dimension to store the new LOD
		LodDimension newlyLoadedDim = new LodDimension(MC.getCurrentDimension(), null, 1);
		newlyLoadedDim.move(playerRegionPos);
		newlyLoadedDim.regions.set(playerRegionPos.x, playerRegionPos.z, new LodRegion(LodUtil.BLOCK_DETAIL_LEVEL, playerRegionPos, VERTICAL_QUALITY_TO_TEST_WITH));
		
		// generate a LOD to test against
		boolean lodGenerated = ApiShared.lodBuilder.generateLodNodeFromChunk(newlyLoadedDim, newlyLoadedChunk, new LodBuilderConfig(DistanceGenerationMode.FULL), true, true);
		if (!lodGenerated)
			return null;
		
		// new chunk data
		long[][][] newChunkData = new long[LodUtil.CHUNK_WIDTH][LodUtil.CHUNK_WIDTH][];
		for (int x = 0; x < LodUtil.CHUNK_WIDTH; x++)
		{
			for (int z = 0; z < LodUtil.CHUNK_WIDTH; z++)
			{
				long[] array = newlyLoadedDim.getRegion(playerRegionPos.x, playerRegionPos.z).getAllData(LodUtil.BLOCK_DETAIL_LEVEL, x + startingBlockPosX, z + startingBlockPosZ);
				newChunkData[x][z] = array;
			}
		}
		boolean newChunkHasData = isDataEmpty(newChunkData);
//		String message = "new chunk data " + (newChunkHasData ? newChunkData[0][0][0] : "[NULL]");
//		MC.sendChatMessage(message);
//		ApiShared.LOGGER.info(message);
		
		// check if the chunk is actually empty
		if (!newChunkHasData)
		{
			if (newlyLoadedChunk.getHeight() != 0)
			{
				// the chunk isn't empty but the LOD is...
				
//				String message = "Error: the chunk at (" + playerChunkPos.getX() + "," + playerChunkPos.getZ() + ") has a height of [" + newlyLoadedChunk.getHeight() + "] but the LOD generated is empty!";
//				MC.sendChatMessage(message);
//				ApiShared.LOGGER.info(message);
				return null;
			}
			else
			{
//				String message = "The chunk at (" + playerChunkPos.getX() + "," + playerChunkPos.getZ() + ") is empty.";
//				MC.sendChatMessage(message);
//				ApiShared.LOGGER.info(message);
			}
		}
		
		
		
		
		// get every folder (world) we have for this dimension
		File dimensionFolder = GetDimensionFolder(newlyLoadedDim.dimension, "");
		// check if the folder exists
		if (dimensionFolder.listFiles() == null)
		{
			if (!dimensionFolder.exists())
			{
				// create the directory since it doesn't exist
				dimensionFolder.mkdirs();
			}
			else
			{
				return null;
			}
		}
		
		
		// compare each world with the newly loaded one
		File mostSimilarWorldFolder = null;
		int mostEqualLines = 0;
		boolean oneDimensionIsValid = false;
		double minimumSimilarityRequired = CONFIG.client().multiplayer().getMultiDimensionRequiredSimilarity();
		
		for (File testDimFolder : dimensionFolder.listFiles())
		{
			// get a LOD from this dimension folder
			LodDimension tempLodDim = new LodDimension(null, null, 1);
			tempLodDim.move(playerRegionPos);
			LodDimensionFileHandler tempFileHandler = new LodDimensionFileHandler(testDimFolder, tempLodDim);
			LodRegion testRegion = tempFileHandler.loadRegionFromFile(LodUtil.BLOCK_DETAIL_LEVEL, playerRegionPos, VERTICAL_QUALITY_TO_TEST_WITH);
			// get data from this LOD
			long[][][] testChunkData = new long[LodUtil.CHUNK_WIDTH][LodUtil.CHUNK_WIDTH][];
			for (int x = 0; x < LodUtil.CHUNK_WIDTH; x++)
			{
				for (int z = 0; z < LodUtil.CHUNK_WIDTH; z++)
				{
					long[] array = testRegion.getAllData(LodUtil.BLOCK_DETAIL_LEVEL, x + startingBlockPosX, z + startingBlockPosZ);
					testChunkData[x][z] = array;
				}
			}
			
			// check if the chunk is actually empty
			if (!isDataEmpty(newChunkData))
			{
//				String message = "The test chunk for dimension folder [" + testDimFolder.getName() + "] and chunk pos (" + playerChunkPos.getX() + "," + playerChunkPos.getZ() + ") is empty. Is that correct?";
//				MC.sendChatMessage(message);
//				ApiShared.LOGGER.info(message);
				continue;
			}
			oneDimensionIsValid = true;
			
			
			// compare the two LODs
			int equalLines = 0;
			int totalLineCount = 0;
			for (int x = 0; x < LodUtil.CHUNK_WIDTH; x++)
			{
				for (int z = 0; z < LodUtil.CHUNK_WIDTH; z++)
				{
					for (int y = 0; y < newChunkData[x][z].length; y++)
					{
						if (newChunkData[x][z][y] == testChunkData[x][z][y])
						{
							equalLines++;
						}
						totalLineCount++;
					}
				}
			}
			
			
			// determine if this world is closer to the newly loaded world
			double percentEqual = (double) equalLines / (double) totalLineCount;
			if (equalLines > mostEqualLines && percentEqual >= minimumSimilarityRequired)
			{
				mostEqualLines = equalLines;
				mostSimilarWorldFolder = testDimFolder;
			}
//			String message = "test data [" + testDimFolder.getName().substring(0, 6) + "...] " + testChunkData[0][0][0] + " equal lines: " + equalLines + "/" + totalLineCount + " = " + percentEqual;
//			MC.sendChatMessage(message);
//			ApiShared.LOGGER.info(message);
		}
		
		
		if (!oneDimensionIsValid && dimensionFolder.listFiles().length != 0)
			// all the world folders were empty, and there was at least one world folder that we tested
			return null;
		
		
		if (mostSimilarWorldFolder != null)
		{
			// we found a world folder that is similar, use it
			
			String message = "Dimension folder set to: [" + mostSimilarWorldFolder.getName().substring(0, 8) + "...]";
//			MC.sendChatMessage(message);
			ApiShared.LOGGER.info(message);
			return mostSimilarWorldFolder;
		}
		else
		{
			// no world folder was found, create a new one
			
			String newId = UUID.randomUUID().toString();
			String message = "No dimension folder found. Creating a new one with ID: " + newId.substring(0, 8) + "...";
//			MC.sendChatMessage(message);
			ApiShared.LOGGER.info(message);
			return GetDimensionFolder(newlyLoadedDim.dimension, newId);
		}
	}
	
	/**
	 * Returns the dimension folder with the specific ID if specified. <br>
	 * If the worldId is empty or null this returns the dimension parent folder <br>
	 * Example folder names: "dim_overworld/worldId", "dim_the_nether/worldId"
	 */
	public static File GetDimensionFolder(IDimensionTypeWrapper newDimensionType, String worldId)
	{
		// prevent null pointers
		if (worldId == null)
			worldId = "";
		
		try
		{
			if (MC.hasSinglePlayerServer())
			{
				// local world
				IWorldWrapper serverWorld = LodUtil.getServerWorldFromDimension(newDimensionType);
				return new File(serverWorld.getSaveFolder().getCanonicalFile().getPath() + File.separatorChar + "lod" + File.separatorChar + worldId);
			}
			else
			{
				// multiplayer
				return new File(MC.getGameDirectory().getCanonicalFile().getPath() +
						File.separatorChar + "Distant_Horizons_server_data" + File.separatorChar + MC.getCurrentDimensionId() + File.separatorChar + worldId);
			}
		}
		catch (IOException e)
		{
			ApiShared.LOGGER.error("Unable to get dimension folder for dimension [" + newDimensionType.getDimensionName() + "]", e);
			return null;
		}
	}
	
	
	/** Returns true if the given chunk is valid to test */
	public static boolean CanDetermineDimensionFolder(IChunkWrapper chunk)
	{
		// we can only guess if the given chunk can be converted into a LOD
		return LodBuilder.canGenerateLodFromChunk(chunk);
	}
	
	
	/** Used for debugging, returns true if every data point is 0 */
	private static boolean isDataEmpty(long[][][] chunkData)
	{
		for (long[][] xArray : chunkData)
		{
			for (long[] zArray : xArray)
			{
				for (long dataPoint : zArray)
				{
					if (dataPoint != 0)
					{
						return true;
					}
				}
			}
		}
		
		return false;
	}
	
}
