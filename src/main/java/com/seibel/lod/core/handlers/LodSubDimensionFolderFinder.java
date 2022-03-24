package com.seibel.lod.core.handlers;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.seibel.lod.core.api.ApiShared;
import com.seibel.lod.core.objects.opengl.builders.lodBuilding.LodBuilder;
import com.seibel.lod.core.objects.opengl.builders.lodBuilding.LodBuilderConfig;
import com.seibel.lod.core.enums.config.DistanceGenerationMode;
import com.seibel.lod.core.enums.config.VerticalQuality;
import com.seibel.lod.core.handlers.dependencyInjection.SingletonHandler;
import com.seibel.lod.core.objects.lod.LodDimension;
import com.seibel.lod.core.objects.lod.LodRegion;
import com.seibel.lod.core.objects.lod.RegionPos;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.wrapperInterfaces.IWrapperFactory;
import com.seibel.lod.core.wrapperInterfaces.block.AbstractBlockPosWrapper;
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
 * @version 3-23-2022
 */
public class LodSubDimensionFolderFinder
{
	private static final IMinecraftClientWrapper MC = SingletonHandler.get(IMinecraftClientWrapper.class);
	private static final ILodConfigWrapperSingleton CONFIG = SingletonHandler.get(ILodConfigWrapperSingleton.class);
	private static final IWrapperFactory FACTORY = SingletonHandler.get(IWrapperFactory.class);
	
	/** Increasing this will increase accuracy but increase calculation time */
	private static final VerticalQuality VERTICAL_QUALITY_TO_TEST_WITH = VerticalQuality.MEDIUM;
	
	
	private static LodSubDimensionFolderFinder.PlayerData PLAYER_DATA = new LodSubDimensionFolderFinder.PlayerData(MC);
	private static LodSubDimensionFolderFinder.PlayerData FIRST_SEEN_PLAYER_DATA = null;
	
	
	
	/**
	 * Currently this method checks a single chunk (where the player is)
	 * and compares it against the same chunk position in the other dimension worlds to
	 * guess which world the player is in.
	 * @return the new or existing folder for this dimension, null if there was a problem
	 * @throws IOException if the folder doesn't exist or can't be accessed
	 */
	public static File determineSubDimensionFolder() throws IOException
	{
		if (FIRST_SEEN_PLAYER_DATA == null)
		{
			FIRST_SEEN_PLAYER_DATA = PLAYER_DATA;
			PLAYER_DATA = new LodSubDimensionFolderFinder.PlayerData(MC);
		}
		
		
		// relevant positions
		AbstractChunkPosWrapper playerChunkPos = FACTORY.createChunkPos(FIRST_SEEN_PLAYER_DATA.playerBlockPos);
		int startingBlockPosX = playerChunkPos.getMinBlockX();
		int startingBlockPosZ = playerChunkPos.getMinBlockZ();
		RegionPos playerRegionPos = new RegionPos(playerChunkPos);
		
		
		// chunk from the newly loaded dimension
		IChunkWrapper newlyLoadedChunk = MC.getWrappedClientWorld().tryGetChunk(playerChunkPos);
		// check if this chunk is valid to test
		if (!LodSubDimensionFolderFinder.CanDetermineDimensionFolder(newlyLoadedChunk))
			return null;
		
		// create a temporary dimension to store the test LOD
		LodDimension newlyLoadedDim = new LodDimension(MC.getCurrentDimension(), null, 1);
		newlyLoadedDim.move(playerRegionPos);
		newlyLoadedDim.regions.set(playerRegionPos.x, playerRegionPos.z, new LodRegion(LodUtil.BLOCK_DETAIL_LEVEL, playerRegionPos, VERTICAL_QUALITY_TO_TEST_WITH));
		
		// generate a LOD to test against
		boolean lodGenerated = ApiShared.lodBuilder.generateLodNodeFromChunk(newlyLoadedDim, newlyLoadedChunk, new LodBuilderConfig(DistanceGenerationMode.FULL), true, true);
		if (!lodGenerated)
			return null;
		
		
		// log the start of this attempt
		ApiShared.LOGGER.info("Attempting to determine sub-dimension for [" + MC.getCurrentDimension().getDimensionName() + "]");
		ApiShared.LOGGER.info("First seen player block pos in dimension: [" + FIRST_SEEN_PLAYER_DATA.playerBlockPos.getX() + "," + FIRST_SEEN_PLAYER_DATA.playerBlockPos.getY() + "," + FIRST_SEEN_PLAYER_DATA.playerBlockPos.getZ() + "]");
		
		
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
		
		// check if the chunk is actually empty
		if (!newChunkHasData)
		{
			if (newlyLoadedChunk.getHeight() != 0)
			{
				// the chunk isn't empty but the LOD is...
				
				String message = "Error: the chunk at (" + playerChunkPos.getX() + "," + playerChunkPos.getZ() + ") has a height of [" + newlyLoadedChunk.getHeight() + "] but the LOD generated is empty!";
				MC.sendChatMessage(message);
				ApiShared.LOGGER.error(message);
				return null;
			}
			else
			{
				String message = "Warning: The chunk at (" + playerChunkPos.getX() + "," + playerChunkPos.getZ() + ") is empty.";
//				MC.sendChatMessage(message);
				ApiShared.LOGGER.warn(message);
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
		}
		
		// TODO move any old files if they exist
		
		
		
		// compare each world with the newly loaded one
		File mostSimilarWorldFolder = null;
		int mostEqualLines = 0;
		double highestEqualityPercent = 0.0;
		double minimumSimilarityRequired = CONFIG.client().multiplayer().getMultiDimensionRequiredSimilarity();
		
		ApiShared.LOGGER.info("Known Sub Dimensions: [" + dimensionFolder.listFiles().length + "]");
		for (File testDimFolder : dimensionFolder.listFiles())
		{
			ApiShared.LOGGER.info("Testing sub dimension: [" + LodUtil.shortenString(testDimFolder.getName(), 8) + "]");
			
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
			
			
			// get the player data for this dimension folder
			PlayerData testPlayerData = new PlayerData(testDimFolder);
			ApiShared.LOGGER.info("Last known player pos: [" + testPlayerData.playerBlockPos.getX() + "," + testPlayerData.playerBlockPos.getY() + "," + testPlayerData.playerBlockPos.getZ() + "]");
			
			// check if the block positions are close
			int distance = testPlayerData.playerBlockPos.getManhattanDistance(FIRST_SEEN_PLAYER_DATA.playerBlockPos);
			ApiShared.LOGGER.info("Player block position distance between saved sub dimension and first seen is [" + distance + "]");
//			if (distance <= 2) // TODO make this number a config
//			{
//				// TODO do something with this information
//			}
			
			
			// check if the chunk is actually empty
			if (!isDataEmpty(newChunkData))
			{
				String message = "The test chunk for dimension folder [" +  LodUtil.shortenString(testDimFolder.getName(), 8) + "] and chunk pos (" + playerChunkPos.getX() + "," + playerChunkPos.getZ() + ") is empty. Is that correct?";
//				MC.sendChatMessage(message);
				ApiShared.LOGGER.info(message);
				continue;
			}
			
			
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
			if (equalLines > mostEqualLines)
			{
				mostEqualLines = equalLines;
				highestEqualityPercent = percentEqual;
				
				if (percentEqual >= minimumSimilarityRequired)
				{
					mostSimilarWorldFolder = testDimFolder;
				}
			}
			String message = "Sub dimension [" +  LodUtil.shortenString(testDimFolder.getName(), 8) + "...] is current dimension probability: " +  LodUtil.shortenString(percentEqual + "", 5) + " (" + equalLines + "/" + totalLineCount + ")";
			MC.sendChatMessage(message);
			ApiShared.LOGGER.info(message);
		}
		
		// the first seen player data is no longer needed, the sub dimension has been determined
		FIRST_SEEN_PLAYER_DATA = null;
		
		
		if (mostSimilarWorldFolder != null)
		{
			// we found a world folder that is similar, use it
			
			String message = "Sub Dimension set to: [" +  LodUtil.shortenString(mostSimilarWorldFolder.getName(), 8) + "...] with an equality of [" + highestEqualityPercent + "]";
			MC.sendChatMessage(message);
			ApiShared.LOGGER.info(message);
			return mostSimilarWorldFolder;
		}
		else
		{
			// no world folder was found, create a new one
			
			String newId = UUID.randomUUID().toString();
			String message = "No suitable sub dimension found. The highest equality was [" + highestEqualityPercent + "]. Creating a new sub dimension with ID: " + LodUtil.shortenString(newId, 8) + "...";
			MC.sendChatMessage(message);
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
	
	
	
	
	
	
	
	private static final String playerDataFileName = "_playerData.toml";
	
	private static PlayerData getPlayerData(File worldFolder)
	{
		File file = PlayerData.getFileForDimensionFolder(worldFolder);
		if (!file.exists())
		{
			return null;
		}
		
		
		CommentedFileConfig toml = CommentedFileConfig.builder(file).build();
		toml.add("path", "test");
		toml.save();
		
		return null;
	}
	
	public static void updatePlayerData()
	{
		PLAYER_DATA.updateData(MC);
	}
	
	/** saves any necessary player data to the given world folder */
	public static void saveDimensionPlayerData(File worldFolder)
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
				ApiShared.LOGGER.error("Unable to save player dimension data for world folder [" + worldFolder.getPath() + "].", e);
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
	
	
	/** Data container for any player data we can use to differentiate one dimension from another. */
	private static class PlayerData
	{
		public static final  IWrapperFactory FACTORY = SingletonHandler.get(IWrapperFactory.class);
		
		public static final String PLAYER_BLOCK_POS_X_PATH = "playerBlockPosX";
		public static final String PLAYER_BLOCK_POS_Y_PATH = "playerBlockPosY";
		public static final String PLAYER_BLOCK_POS_Z_PATH = "playerBlockPosZ";
		AbstractBlockPosWrapper playerBlockPos;
		
		// not implemented yet
		public static final String WORLD_SPAWN_POS_X_PATH = "worldSpawnBlockPosX";
		public static final String WORLD_SPAWN_POS_Y_PATH = "worldSpawnBlockPosY";
		public static final String WORLD_SPAWN_POS_Z_PATH = "worldSpawnBlockPosZ";
		/**
		 * The client world has access to a spawn point, so this should be possible to fill in.
		 * I'm not sure what this will look like for worlds that don't have a spawn point.
		 */
		AbstractBlockPosWrapper worldSpawnPointBlockPos;
		
		
		
		public PlayerData(IMinecraftClientWrapper mc)
		{
			updateData(mc);
		}
		
		public PlayerData(File dimensionFolder)
		{
			File file = getFileForDimensionFolder(dimensionFolder);
			CommentedFileConfig toml = CommentedFileConfig.builder(file).build();
			
			// player block pos
			try
			{
				toml.load();
				
				// TODO this is crashing...
				int x = toml.getInt(PLAYER_BLOCK_POS_X_PATH);
				int y = toml.getInt(PLAYER_BLOCK_POS_Y_PATH);
				int z = toml.getInt(PLAYER_BLOCK_POS_Z_PATH);
				this.playerBlockPos = FACTORY.createBlockPos(x, y, z);
			}
			catch(Exception e)
			{
				ApiShared.LOGGER.error(e.getMessage(), e);
			}
		}
		
		
		
		public static File getFileForDimensionFolder(File file)
		{
			return new File(file.getPath() + File.separatorChar + playerDataFileName);
		}
		
		
		/**  */
		public void updateData(IMinecraftClientWrapper mc)
		{
			this.playerBlockPos = mc.getPlayerBlockPos();
		}
		
		/** Writes everything from this object to the file given. */
		public void toTomlFile(CommentedFileConfig toml)
		{
			// player block pos
			toml.add(PLAYER_BLOCK_POS_X_PATH, playerBlockPos.getX());
			toml.add(PLAYER_BLOCK_POS_Y_PATH, playerBlockPos.getY());
			toml.add(PLAYER_BLOCK_POS_Z_PATH, playerBlockPos.getZ());
			
			
			toml.save();
		}
		
		
		@Override
		public String toString()
		{
			return "PlayerBlockPos: [" + playerBlockPos.getX() + "," + playerBlockPos.getY() + "," + playerBlockPos.getZ() + "]";
		}
	}
}
