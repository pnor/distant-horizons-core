package com.seibel.lod.core.objects.a7.io;

import com.seibel.lod.core.api.internal.InternalApiShared;
import com.seibel.lod.core.builders.lodBuilding.LodBuilder;
import com.seibel.lod.core.builders.lodBuilding.LodBuilderConfig;
import com.seibel.lod.core.config.Config;
import com.seibel.lod.core.enums.config.EDistanceGenerationMode;
import com.seibel.lod.core.enums.config.EVerticalQuality;
import com.seibel.lod.core.handlers.LodDimensionFileHandler;
import com.seibel.lod.core.handlers.LodDimensionFinder;
import com.seibel.lod.core.handlers.dependencyInjection.SingletonHandler;
import com.seibel.lod.core.handlers.dimensionFinder.PlayerData;
import com.seibel.lod.core.handlers.dimensionFinder.SubDimCompare;
import com.seibel.lod.core.logging.ConfigBasedLogger;
import com.seibel.lod.core.objects.DHChunkPos;
import com.seibel.lod.core.objects.DHRegionPos;
import com.seibel.lod.core.objects.a7.DHLevel;
import com.seibel.lod.core.objects.a7.DHWorld;
import com.seibel.lod.core.objects.lod.LodDimension;
import com.seibel.lod.core.objects.lod.LodRegion;
import com.seibel.lod.core.util.DataPointUtil;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.wrapperInterfaces.chunk.IChunkWrapper;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IWorldWrapper;
import org.apache.logging.log4j.LogManager;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class LevelToFileMatcher {
    private static final IMinecraftClientWrapper MC = SingletonHandler.get(IMinecraftClientWrapper.class);
    public static final ConfigBasedLogger LOGGER = new ConfigBasedLogger(LogManager.getLogger(LodDimensionFinder.class),
            () -> Config.Client.Advanced.Debugging.DebugSwitch.logFileSubDimEvent.get());

    /** Increasing this will increase accuracy but increase calculation time */
    private static final EVerticalQuality VERTICAL_QUALITY_TO_TEST_WITH = EVerticalQuality.LOW;

    public static final String THREAD_NAME = "Level-To-File-Matcher";

    private PlayerData playerData = new PlayerData(MC);
    private PlayerData firstSeenPlayerData = null;

    private volatile DHLevel foundLevel = null;

    /** If true the LodDimensionFileHelper is attempting to determine the folder for this dimension */
    private final AtomicBoolean determiningWorldFolder = new AtomicBoolean(false);

    private final IWorldWrapper currentWorld;
    private final File worldFolder;
    private final DHWorld dhWorld;

    public LevelToFileMatcher(DHWorld dhWorld, File worldFolder, IWorldWrapper targetWorld) {
        this.currentWorld = targetWorld;
        this.worldFolder = worldFolder;
        this.dhWorld = dhWorld;
    }

    // May return null, where at this moment the level is not yet known
    public DHLevel tryGetLevel() {
        tick();
        return foundLevel;
    }

    public IWorldWrapper getTargetWorld() {
        return currentWorld;
    }

    private void tick() {
        // prevent multiple threads running at the same time

        if (Config.Client.Multiplayer.multiDimensionRequiredSimilarity.get() == 0 || MC.hasSinglePlayerServer()) {
            File saveDir = getLevelFolderWithoutSimilarityMatching();
            foundLevel = new DHLevel(dhWorld, saveDir, currentWorld);
        } else {
            if (determiningWorldFolder.getAndSet(true)) return;
            //FIXME: Use a thread pool
            Thread thread = new Thread(() ->
            {
                try {
                    // attempt to get the file handler
                    File saveDir = attemptToDetermineSubDimensionFolder();
                    if (saveDir == null) return;
                    foundLevel = new DHLevel(dhWorld, saveDir, currentWorld);
                } catch (IOException e) {
                    LOGGER.error("Unable to set the dimension file handler for level [" + currentWorld + "]. Error: ", e);
                } finally {
                    // make sure we unlock this method
                    determiningWorldFolder.set(false);
                }
            });
            thread.setName(THREAD_NAME);
            thread.start();
        }
    }

    /**
     * Returns the default save folder if it exists
     * otherwise the first valid subDimension folder lexicographically.
     */
    private File getLevelFolderWithoutSimilarityMatching()
    {
        File[] subDirs = worldFolder.listFiles();
        File levelFolder = null;
        // check if a sub dimension folder exists
        if (subDirs != null)
        {
            // at least one folder exists
            LOGGER.info("Potential Sub Dimension folders: [" + subDirs.length + "]");

            Arrays.sort(subDirs); // listFiles isn't necessarily sorted
            for (File potentialFolder : subDirs)
            {
                if (isValidLevelFolder(potentialFolder))
                {
                    if (potentialFolder.getName().equals(currentWorld.getDimensionType().getDimensionName()))
                    {
                        // use the default save folder if possible
                        levelFolder = potentialFolder;
                        break;
                    }
                    else if (levelFolder == null)
                    {
                        // only get the first non-default sub folder
                        levelFolder = potentialFolder;
                    }
                }
            }
        }

        // if no valid sub dimension was found, create a new one
        if (levelFolder == null)
        {
            levelFolder = new File(worldFolder, currentWorld.getDimensionType().getDimensionName());
            levelFolder.mkdirs();
            LOGGER.info("Default Sub Dimension not found. Creating: [" +  currentWorld.getDimensionType().getDimensionName() + "]");
        }
        else
        {
            LOGGER.info("Default Sub Dimension set to: [" +  LodUtil.shortenString(levelFolder.getName(), 8) + "...]");
        }
        return levelFolder;
    }


    /**
     * Currently this method checks a single chunk (where the player is)
     * and compares it against the same chunk position in the other dimension worlds to
     * guess which world the player is in.
     *
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
        DHChunkPos playerChunkPos = new DHChunkPos(playerData.playerBlockPos);
        int startingBlockPosX = playerChunkPos.getMinBlockX();
        int startingBlockPosZ = playerChunkPos.getMinBlockZ();
        DHRegionPos playerRegionPos = new DHRegionPos(playerChunkPos);

        // chunk from the newly loaded level
        IChunkWrapper newlyLoadedChunk = MC.getWrappedClientWorld().tryGetChunk(playerChunkPos);
        // check if this chunk is valid to test
        if (!CanDetermineLevelFolder(newlyLoadedChunk))
            return null;

        // create a temporary dimension to store the test LOD
        LodDimension newlyLoadedDim = new LodDimension(MC.getCurrentDimension(), 1, null, false);
        newlyLoadedDim.move(playerRegionPos);
        newlyLoadedDim.regions.set(playerRegionPos.x, playerRegionPos.z, new LodRegion(LodUtil.BLOCK_DETAIL_LEVEL, playerRegionPos, VERTICAL_QUALITY_TO_TEST_WITH));

        // generate a LOD to test against
        boolean lodGenerated = InternalApiShared.lodBuilder.generateLodNodeFromChunk(newlyLoadedDim, newlyLoadedChunk, new LodBuilderConfig(EDistanceGenerationMode.FULL), true, true);
        if (!lodGenerated)
            return null;

        // log the start of this attempt
        LOGGER.info("Attempting to determine sub-dimension for [" + MC.getCurrentDimension().getDimensionName() + "]");
        LOGGER.info("Player block pos in dimension: [" + playerData.playerBlockPos.getX() + "," + playerData.playerBlockPos.getY() + "," + playerData.playerBlockPos.getZ() + "]");

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

        // compare each world with the newly loaded one
        SubDimCompare mostSimilarSubDim = null;

        File[] levelFolders = worldFolder.listFiles(File::isDirectory);
        LOGGER.info("Potential Sub Dimension folders: [" + levelFolders.length + "]");
        for (File testLevelFolder : levelFolders)
        {
            //FIXME: Err... what? The filter should have already filtered this out... Is this needed?
            if (!testLevelFolder.isDirectory()) continue;
            if (!isValidLevelFolder(testLevelFolder)) continue;
            LOGGER.info("Testing level folder: [" + LodUtil.shortenString(testLevelFolder.getName(), 8) + "]");
            try
            {
                // get a LOD from this dimension folder
                LodDimension tempLodDim = new LodDimension(null, 1, null, false);
                tempLodDim.move(playerRegionPos);
                LodDimensionFileHandler tempFileHandler = new LodDimensionFileHandler(testLevelFolder, tempLodDim);
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
                PlayerData testPlayerData = new PlayerData(testLevelFolder);
                LOGGER.info("Last known player pos: [" + testPlayerData.playerBlockPos.getX() + "," + testPlayerData.playerBlockPos.getY() + "," + testPlayerData.playerBlockPos.getZ() + "]");

                // check if the block positions are close
                int playerBlockDist = testPlayerData.playerBlockPos.getManhattanDistance(playerData.playerBlockPos);
                LOGGER.info("Player block position distance between saved sub dimension and first seen is [" + playerBlockDist + "]");

                // check if the chunk is actually empty
                if (isDataEmpty(testChunkData))
                {
                    String message = "The test chunk for dimension folder [" + LodUtil.shortenString(testLevelFolder.getName(), 8) + "] and chunk pos (" + playerChunkPos.getX() + "," + playerChunkPos.getZ() + ") is empty. This is expected if the position is outside the sub-dimension's generated area.";
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
                SubDimCompare subDimCompare = new SubDimCompare(equalDataPoints, totalDataPointCount, playerBlockDist, testLevelFolder);
                if (mostSimilarSubDim == null || subDimCompare.compareTo(mostSimilarSubDim) > 0)
                {
                    mostSimilarSubDim = subDimCompare;
                }

                LOGGER.info("Sub dimension [" + LodUtil.shortenString(testLevelFolder.getName(), 8) + "...] is current dimension probability: " + LodUtil.shortenString(subDimCompare.getPercentEqual() + "", 5) + " (" + equalDataPoints + "/" + totalDataPointCount + ")");
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

            LOGGER.info("Sub Dimension set to: [" +  LodUtil.shortenString(mostSimilarSubDim.folder.getName(), 8) + "...] with an equality of [" + mostSimilarSubDim.getPercentEqual() + "]");
            return mostSimilarSubDim.folder;
        }
        else
        {
            // no world folder was found, create a new one

            double highestEqualityPercent = mostSimilarSubDim != null ? mostSimilarSubDim.getPercentEqual() : 0;

            String newId = UUID.randomUUID().toString();
            String message = "No suitable sub dimension found. The highest equality was [" + LodUtil.shortenString(highestEqualityPercent + "", 5) + "]. Creating a new sub dimension with ID: " + LodUtil.shortenString(newId, 8) + "...";
            LOGGER.info(message);
            File folder = new File(worldFolder, newId);
            folder.mkdirs();
            return folder;
        }
    }

    /** Returns true if the given chunk is valid to test */
    public boolean CanDetermineLevelFolder(IChunkWrapper chunk)
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

    /** Returns true if the given folder holds valid Lod Dimension data */
    public static boolean isValidLevelFolder(File potentialFolder)
    {
        if (!potentialFolder.isDirectory())
            // it needs to be a folder
            return false;

        if (potentialFolder.listFiles() == null)
            // it needs to have folders in it
            return false;

        // check if there is at least one VerticalQuality folder in this directory
        for (File internalFolder : potentialFolder.listFiles())
        {
            if (EVerticalQuality.getByName(internalFolder.getName()) != null)
            {
                // one of the internal folders is a VerticalQuality folder
                return true;
            }
        }

        return false;
    }

}
