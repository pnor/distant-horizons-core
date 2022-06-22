package com.seibel.lod.core.a7.save.structure;

import com.seibel.lod.core.a7.io.LevelToFileMatcher;
import com.seibel.lod.core.a7.level.DHLevel;
import com.seibel.lod.core.a7.world.DhClientWorld;
import com.seibel.lod.core.a7.world.DhWorld;
import com.seibel.lod.core.config.Config;
import com.seibel.lod.core.enums.config.EServerFolderNameMode;
import com.seibel.lod.core.enums.config.EVerticalQuality;
import com.seibel.lod.core.handlers.dependencyInjection.SingletonHandler;
import com.seibel.lod.core.objects.ParsedIp;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.ILevelWrapper;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ClientOnlySaveStructure extends SaveStructure {
    final File folder;
    private static final IMinecraftClientWrapper MC_CLIENT = SingletonHandler.get(IMinecraftClientWrapper.class);
    public static final String INVALID_FILE_CHARACTERS_REGEX = "[\\\\/:*?\"<>|]";
    private static String getServerFolderName()
    {
        // parse the current server's IP
        ParsedIp parsedIp = new ParsedIp(MC_CLIENT.getCurrentServerIp());
        String serverIpCleaned = parsedIp.ip.replaceAll(INVALID_FILE_CHARACTERS_REGEX, "");
        String serverPortCleaned = parsedIp.port != null ? parsedIp.port.replaceAll(INVALID_FILE_CHARACTERS_REGEX, "") : "";

        // determine the format of the folder name
        EServerFolderNameMode folderNameMode = Config.Client.Multiplayer.serverFolderNameMode.get();
        if (folderNameMode == EServerFolderNameMode.AUTO)
        {
            if (parsedIp.isLan())
            {
                // LAN
                folderNameMode = EServerFolderNameMode.NAME_IP;
            }
            else
            {
                // normal multiplayer
                folderNameMode = EServerFolderNameMode.NAME_IP_PORT;
            }
        }
        String serverName = MC_CLIENT.getCurrentServerName().replaceAll(INVALID_FILE_CHARACTERS_REGEX, "");
        String serverMcVersion = MC_CLIENT.getCurrentServerVersion().replaceAll(INVALID_FILE_CHARACTERS_REGEX, "");
        // generate the folder name
        String folderName = "";
        switch (folderNameMode)
        {
            // default and auto shouldn't be used
            // and are just here to make the compiler happy
            default:
            case NAME_ONLY:
                folderName = serverName;
                break;

            case NAME_IP:
                folderName = serverName + ", IP " + serverIpCleaned;
                break;
            case NAME_IP_PORT:
                folderName = serverName + ", IP " + serverIpCleaned + (serverPortCleaned.length() != 0 ? ("-" + serverPortCleaned) : "");
                break;
            case NAME_IP_PORT_MC_VERSION:
                folderName = serverName + ", IP " + serverIpCleaned + (serverPortCleaned.length() != 0 ? ("-" + serverPortCleaned) : "") + ", GameVersion " + serverMcVersion;
                break;
        }
        return folderName;
    }

    LevelToFileMatcher fileMatcher = null;
    final DhClientWorld world;

    // Fit for Client_Only environment
    public ClientOnlySaveStructure(DhClientWorld world) {
        folder = new File(MC_CLIENT.getGameDirectory().getPath() +
                File.separatorChar + "Distant_Horizons_server_data" + File.separatorChar + getServerFolderName());
        if (!folder.exists()) folder.mkdirs(); //TODO: Deal with errors
        this.world = world;
    }

    @Override
    public DHLevel tryGetLevel(ILevelWrapper wrapper) {
        if (Config.Client.Multiplayer.multiDimensionRequiredSimilarity.get() == 0) {
            if (fileMatcher != null) {
                fileMatcher.close();
                fileMatcher = null;
            }
            return new DHLevel(world, getLevelFolderWithoutSimilarityMatching(wrapper), wrapper);
        }

        if (fileMatcher == null || !fileMatcher.isFindingLevel(wrapper)) {
            LOGGER.info("Loading level for world " + wrapper.getDimensionType().getDimensionName());
            fileMatcher = new LevelToFileMatcher(world, wrapper, folder,
                    (File[]) getMatchingLevelFolders(wrapper).toArray());
        }

        DHLevel level = fileMatcher.tryGetLevel();
        if (level != null) {
            fileMatcher.close();
            fileMatcher = null;
        }
        return level;
    }

    private File getLevelFolderWithoutSimilarityMatching(ILevelWrapper level)
    {
        Stream<File> folders = getMatchingLevelFolders(level);
        Optional<File> first = folders.findFirst();
        if (first.isPresent())
        {
            LOGGER.info("Default Sub Dimension set to: [" +  LodUtil.shortenString(first.get().getName(), 8) + "...]");
            return first.get();
        } else { // if no valid sub dimension was found, create a new one
            LOGGER.info("Default Sub Dimension not found. Creating: [" +  level.getDimensionType().getDimensionName() + "]");
            return new File(folder, level.getDimensionType().getDimensionName());
        }
    }

    public Stream<File> getMatchingLevelFolders(@Nullable ILevelWrapper level) {
        File[] folders = folder.listFiles();
        if (folders==null) return Stream.empty();
        return Arrays.stream(folders).filter(
                (f) -> {
                    if (!isValidLevelFolder(f)) return false;
                    return level==null || f.getName().equalsIgnoreCase(level.getDimensionType().getDimensionName());
                }
        ).sorted();
    }

    /** Returns true if the given folder holds valid Lod Dimension data */
    private static boolean isValidLevelFolder(File potentialFolder)
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


    @Override
    public File getRenderCacheFolder(ILevelWrapper world) {
        return null;
    }

    @Override
    public File getDataFolder(ILevelWrapper world) {
        return null;
    }

    @Override
    public void close() {
        fileMatcher.close();
    }
}
