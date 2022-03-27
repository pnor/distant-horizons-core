package com.seibel.lod.core.handlers;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;

import com.seibel.lod.core.api.ApiShared;
import com.seibel.lod.core.handlers.dimensionFinder.PlayerData;
import com.seibel.lod.core.handlers.dimensionFinder.SubDimCompare;
import com.seibel.lod.core.builders.lodBuilding.LodBuilder;
import com.seibel.lod.core.builders.lodBuilding.LodBuilderConfig;
import com.seibel.lod.core.enums.config.DistanceGenerationMode;
import com.seibel.lod.core.enums.config.VerticalQuality;
import com.seibel.lod.core.handlers.dependencyInjection.SingletonHandler;
import com.seibel.lod.core.logging.ConfigBasedLogger;
import com.seibel.lod.core.objects.lod.LodDimension;
import com.seibel.lod.core.objects.lod.LodRegion;
import com.seibel.lod.core.objects.lod.RegionPos;
import com.seibel.lod.core.util.DataPointUtil;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.wrapperInterfaces.IWrapperFactory;
import com.seibel.lod.core.wrapperInterfaces.chunk.AbstractChunkPosWrapper;
import com.seibel.lod.core.wrapperInterfaces.chunk.IChunkWrapper;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IDimensionTypeWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IWorldWrapper;
import net.minecraft.world.level.dimension.DimensionType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

/**
 * Used to guess the world folder for the player's current dimension.
 *
 * @author James Seibel
 * @version 2022-3-26
 */
public class LodDimensionFinder
{
	private static final IMinecraftClientWrapper MC = SingletonHandler.get(IMinecraftClientWrapper.class);
	private static final ILodConfigWrapperSingleton CONFIG = SingletonHandler.get(ILodConfigWrapperSingleton.class);
	private static final IWrapperFactory FACTORY = SingletonHandler.get(IWrapperFactory.class);
	public static final ConfigBasedLogger LOGGER = new ConfigBasedLogger(() -> CONFIG.client().advanced().debugging().debugSwitch().getLogFileSubDimEvent());
	
	/** Increasing this will increase accuracy but increase calculation time */
	private static final VerticalQuality VERTICAL_QUALITY_TO_TEST_WITH = VerticalQuality.LOW;
	
	public static final String THREAD_NAME = "Sub-Dimension-Finder";
	public static final String DEFAULT_SAVE_DIMENSION_FOLDER = "_Default-Sub-Dimension";
	
	private PlayerData playerData = new PlayerData(MC);
	private PlayerData firstSeenPlayerData = null;
	
	private volatile LodDimension foundLodDimension = null;
	
	/** If true the LodDimensionFileHelper is attempting to determine the folder for this dimension */
	private boolean determiningWorldFolder = false;
	
	
	
	public LodDimensionFinder()
	{
	
	}
	
	
	
	/** Returns true if a LodDimension has been found */
	public boolean isDone()
	{
		return foundLodDimension != null;
	}
	
	/** Returns the found LodDimension */
	public LodDimension getAndClearFoundLodDimension()
	{
		// clear the found dimension
		LodDimension returnDim = this.foundLodDimension;
		this.foundLodDimension = null;
		
		return returnDim;
	}
	
	
	
	public void AttemptToDetermineSubDimensionAsync(IDimensionTypeWrapper dimensionTypeWrapper)
	{
		// prevent multiple threads running at the same time
		if (determiningWorldFolder && !isDone())
			return;
		determiningWorldFolder = true;
		
		
		// run asynchronously since this could take a while
		Thread thread = new Thread(() ->
		{
			try
			{
				// attempt to get the file handler
				File saveDir;
				if (CONFIG.client().multiplayer().getMultiDimensionRequiredSimilarity() == 0)
				{
					// only allow 1 sub dimension per world
					
					// move any old data folders if they exist
					File dimensionFolder = GetDimensionFolder(MC.getCurrentDimension(), "");
					moveOldSaveFoldersIfNecessary(dimensionFolder, MC.getCurrentDimension(), DEFAULT_SAVE_DIMENSION_FOLDER);
					
					saveDir = GetDimensionFolder(dimensionTypeWrapper, DEFAULT_SAVE_DIMENSION_FOLDER);
				}
				else
				{
					saveDir = attemptToDetermineSubDimensionFolder();
				}
				
				if (saveDir == null)
					return;
				
				foundLodDimension = new LodDimension(dimensionTypeWrapper, ApiShared.lodBuilder.defaultDimensionWidthInRegions, saveDir);
			}
			catch (IOException e)
			{
				ApiShared.LOGGER.error("Unable to set the dimension file handler for dimension type [" + dimensionTypeWrapper.getDimensionName() + "]. Error: " + e.getMessage(), e);
			}
			finally
			{
				// make sure we unlock this method
				determiningWorldFolder = false;
			}
		});
		thread.setName(THREAD_NAME);
		thread.start();
	}
	
	
	
	/**
	 * Currently this method checks a single chunk (where the player is)
	 * and compares it against the same chunk position in the other dimension worlds to
	 * guess which world the player is in.
	 * @throws IOException if the folder doesn't exist or can't be accessed
	 */
	public File attemptToDetermineSubDimensionFolder() throws IOException
	{
		if (firstSeenPlayerData == null)
		{
			firstSeenPlayerData = playerData;
			playerData = new PlayerData(MC);
		}
		
		// relevant positions
		AbstractChunkPosWrapper playerChunkPos = FACTORY.createChunkPos(firstSeenPlayerData.playerBlockPos);
		int startingBlockPosX = playerChunkPos.getMinBlockX();
		int startingBlockPosZ = playerChunkPos.getMinBlockZ();
		RegionPos playerRegionPos = new RegionPos(playerChunkPos);
		
		
		// chunk from the newly loaded dimension
		IChunkWrapper newlyLoadedChunk = MC.getWrappedClientWorld().tryGetChunk(playerChunkPos);
		// check if this chunk is valid to test
		if (!CanDetermineDimensionFolder(newlyLoadedChunk))
			return null;
		
		// create a temporary dimension to store the test LOD
		LodDimension newlyLoadedDim = new LodDimension(MC.getCurrentDimension(), 1, null, false);
		newlyLoadedDim.move(playerRegionPos);
		newlyLoadedDim.regions.set(playerRegionPos.x, playerRegionPos.z, new LodRegion(LodUtil.BLOCK_DETAIL_LEVEL, playerRegionPos, VERTICAL_QUALITY_TO_TEST_WITH));
		
		// generate a LOD to test against
		LOGGER.debug("Generating LOD for testing...");
		boolean lodGenerated = ApiShared.lodBuilder.generateLodNodeFromChunk(newlyLoadedDim, newlyLoadedChunk, new LodBuilderConfig(DistanceGenerationMode.FULL), true, true);
		if (!lodGenerated)
			return null;
		
		
		// log the start of this attempt
		LOGGER.info("Attempting to determine sub-dimension for [" + MC.getCurrentDimension().getDimensionName() + "]");
		LOGGER.info("First seen player block pos in dimension: [" + firstSeenPlayerData.playerBlockPos.getX() + "," + firstSeenPlayerData.playerBlockPos.getY() + "," + firstSeenPlayerData.playerBlockPos.getZ() + "]");
		
		
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
		boolean newChunkHasData = !isDataEmpty(newChunkData);
		
		// check if the chunk is actually empty
		if (!newChunkHasData)
		{
			if (newlyLoadedChunk.getHeight() != 0)
			{
				// the chunk isn't empty but the LOD is...
				
				String message = "Error: the chunk at (" + playerChunkPos.getX() + "," + playerChunkPos.getZ() + ") has a height of [" + newlyLoadedChunk.getHeight() + "] but the LOD generated is empty!";
				LOGGER.error(message);
			}
			else
			{
				String message = "Warning: The chunk at (" + playerChunkPos.getX() + "," + playerChunkPos.getZ() + ") is empty.";
				LOGGER.warn(message);
			}
			return null;
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
		}
		
		
		// move any old data folders if they exist
		moveOldSaveFoldersIfNecessary(dimensionFolder, MC.getCurrentDimension(), UUID.randomUUID().toString());
		
		
		
		
		
		// compare each world with the newly loaded one
		SubDimCompare mostSimilarSubDim = null;

		LOGGER.info("Known Sub Dimension folders: [" + dimensionFolder.listFiles(File::isDirectory).length + "]");
		for (File testDimFolder : dimensionFolder.listFiles())
		{
			if (!testDimFolder.isDirectory())
				continue;
			
			LOGGER.info("Testing sub dimension: [" + LodUtil.shortenString(testDimFolder.getName(), 8) + "]");
			
			try
			{
				
				// get a LOD from this dimension folder
				LodDimension tempLodDim = new LodDimension(null, 1, null, false);
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
				
				
				// get the player data for this dimension folder
				PlayerData testPlayerData = new PlayerData(testDimFolder);
				LOGGER.info("Last known player pos: [" + testPlayerData.playerBlockPos.getX() + "," + testPlayerData.playerBlockPos.getY() + "," + testPlayerData.playerBlockPos.getZ() + "]");
				
				// check if the block positions are close
				int playerBlockDist = testPlayerData.playerBlockPos.getManhattanDistance(firstSeenPlayerData.playerBlockPos);
				ApiShared.LOGGER.info("Player block position distance between saved sub dimension and first seen is [" + playerBlockDist + "]");
				
				
				// check if the chunk is actually empty
				if (isDataEmpty(testChunkData))
				{
					String message = "The test chunk for dimension folder [" + LodUtil.shortenString(testDimFolder.getName(), 8) + "] and chunk pos (" + playerChunkPos.getX() + "," + playerChunkPos.getZ() + ") is empty. This is expected if the position is outside the sub-dimension's generated area.";
					LOGGER.info(message);
					continue;
				}
				
				
				// compare the two LODs
				int equalDataPoints = 0;
				int totalDataPointCount = 0;
				for (int x = 0; x < LodUtil.CHUNK_WIDTH; x++)
				{
					for (int z = 0; z < LodUtil.CHUNK_WIDTH; z++)
					{
						for (int y = 0; y < newChunkData[x][z].length; y++)
						{
							if (newChunkData[x][z][y] == testChunkData[x][z][y])
							{
								equalDataPoints++;
							}
							totalDataPointCount++;
							
							if (!DataPointUtil.doesItExist(newChunkData[x][z][y]) || !DataPointUtil.doesItExist(testChunkData[x][z][y]))
								break;
						}
					}
				}
				
				
				// determine if this world is closer to the newly loaded world
				SubDimCompare subDimCompare = new SubDimCompare(equalDataPoints, totalDataPointCount, playerBlockDist, testDimFolder);
				if (mostSimilarSubDim == null || subDimCompare.compareTo(mostSimilarSubDim) > 0)
				{
					mostSimilarSubDim = subDimCompare;
				}
				
				String message = "Sub dimension [" + LodUtil.shortenString(testDimFolder.getName(), 8) + "...] is current dimension probability: " + LodUtil.shortenString(subDimCompare.getPercentEqual() + "", 5) + " (" + equalDataPoints + "/" + totalDataPointCount + ")";
				LOGGER.info(message);
			}
			catch (Exception e)
			{
				// this sub dimension isn't formatted correctly
				// for now we are just assuming it is an unrelated file
			}
		}
		
		// TODO if two sub dimensions contain the same LODs merge them
		
		
		
		// the first seen player data is no longer needed, the sub dimension has been determined
		firstSeenPlayerData = null;
		
		
		if (mostSimilarSubDim != null && mostSimilarSubDim.isValidSubDim())
		{
			// we found a world folder that is similar, use it
			
			String message = "Sub Dimension set to: [" +  LodUtil.shortenString(mostSimilarSubDim.folder.getName(), 8) + "...] with an equality of [" + mostSimilarSubDim.getPercentEqual() + "]";
			LOGGER.info(message);
			return mostSimilarSubDim.folder;
		}
		else
		{
			// no world folder was found, create a new one
			
			double highestEqualityPercent = mostSimilarSubDim != null ? mostSimilarSubDim.getPercentEqual() : 0;
			
			String newId = UUID.randomUUID().toString();
			String message = "No suitable sub dimension found. The highest equality was [" + LodUtil.shortenString(highestEqualityPercent + "", 5) + "]. Creating a new sub dimension with ID: " + LodUtil.shortenString(newId, 8) + "...";
			LOGGER.info(message);
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
			LOGGER.error("Unable to get dimension folder for dimension [" + newDimensionType.getDimensionName() + "]", e);
			return null;
		}
	}
	
	
	/** Returns true if the given chunk is valid to test */
	public boolean CanDetermineDimensionFolder(IChunkWrapper chunk)
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
						return false;
					}
				}
			}
		}
		
		return true;
	}
	
	/**
	 * Moves any folders from the old save location
	 * (directly under the dimension type)
	 * to a sub-dimension folder with the given name.
	 */
	private void moveOldSaveFoldersIfNecessary(File dimensionFolder, IDimensionTypeWrapper dimensionType, String subDimensionName) throws IOException
	{
		for (File folder : dimensionFolder.listFiles())
		{
			if (VerticalQuality.getByName(folder.getName()) != null)
			{
				// this is a LOD save folder
				// create a new sub dimension and move the data into it
				
				File newDimension = GetDimensionFolder(dimensionType, subDimensionName);
				newDimension.mkdirs();
				
				File oldDataNewPath = new File(newDimension.getPath() + File.separatorChar + folder.getName());
				Files.move(folder.toPath(), oldDataNewPath.toPath());
			}
			else
			{
				// ignore this folder
			}
		}
	}
	
	
	
	
	
	
	
	public void updatePlayerData()
	{
		playerData.updateData(MC);
	}
	
	/** saves any necessary player data to the given world folder */
	public void saveDimensionPlayerData(File worldFolder)
	{
		// get and create the file and path if they don't exist
		File file = PlayerData.getFileForDimensionFolder(worldFolder);
		if (!file.exists())
		{
			try
			{
				file.getParentFile().mkdirs();
				file.createNewFile();
			}
			catch (IOException e)
			{
				LOGGER.error("Unable to save player dimension data for world folder [" + worldFolder.getPath() + "].", e);
				return;
			}
		}
		
		// determine the playerData
		IMinecraftClientWrapper mc = SingletonHandler.get(IMinecraftClientWrapper.class);
		PlayerData playerdata = new PlayerData(mc);
		
		// write the data to file
		CommentedFileConfig toml = CommentedFileConfig.builder(file).build();
		playerdata.toTomlFile(toml);
	}
	
	
}
