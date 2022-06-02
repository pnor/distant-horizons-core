package com.seibel.lod.core.objects.a7.io;

import com.seibel.lod.core.enums.config.ServerFolderNameMode;
import com.seibel.lod.core.handlers.LodDimensionFinder;
import com.seibel.lod.core.handlers.dependencyInjection.SingletonHandler;
import com.seibel.lod.core.logging.ConfigBasedLogger;
import com.seibel.lod.core.objects.ParsedIp;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IWorldWrapper;
import org.apache.logging.log4j.LogManager;

import java.io.File;
import java.io.IOException;

public class DHFolderHandler {
    /**
     * This regex finds any characters that are invalid for use in a windows
     * (and by extension mac and linux) file path
     */
    public static final String INVALID_FILE_CHARACTERS_REGEX = "[\\\\/:*?\"<>|]";
    private static final IMinecraftClientWrapper MC = SingletonHandler.get(IMinecraftClientWrapper.class);
    private static final ILodConfigWrapperSingleton CONFIG = SingletonHandler.get(ILodConfigWrapperSingleton.class);
    public static final ConfigBasedLogger LOGGER = new ConfigBasedLogger(LogManager.getLogger(LodDimensionFinder.class),
            () -> CONFIG.client().advanced().debugging().debugSwitch().getLogFileSubDimEvent());

    public static File getCurrentWorldFolder() {
        File dimensionFolder;
        try
        {
            if (MC.hasSinglePlayerServer())
            {
                // local world
                dimensionFolder = new File(MC.getSinglePlayerServerFolder(), "lod");
            }
            else
            {
                // multiplayer world
                dimensionFolder = new File(MC.getGameDirectory().getCanonicalFile().getPath() +
                        File.separatorChar + "Distant_Horizons_server_data" + File.separatorChar + getServerFolderName());
            }
        }
        catch (IOException e)
        {
            LOGGER.error("Unable to get world folder directory: ", e);
            throw new RuntimeException("Critical error: Unable to get world folder directory", e);
        }

        // move any old data folders if they exist
        File[] subFolders = dimensionFolder.listFiles();
        if (subFolders != null) {
            for (File folder : subFolders)
            {
                //FIXME: Errr... What to do here?
                /*
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
                 */
            }
        }
        return dimensionFolder;
    }
    private static String getServerFolderName()
    {
        // parse the current server's IP
        ParsedIp parsedIp = new ParsedIp(MC.getCurrentServerIp());
        String serverIpCleaned = parsedIp.ip.replaceAll(INVALID_FILE_CHARACTERS_REGEX, "");
        String serverPortCleaned = parsedIp.port != null ? parsedIp.port.replaceAll(INVALID_FILE_CHARACTERS_REGEX, "") : "";


        // determine the format of the folder name
        ServerFolderNameMode folderNameMode = CONFIG.client().multiplayer().getServerFolderNameMode();
        if (folderNameMode == ServerFolderNameMode.AUTO)
        {
            if (parsedIp.isLan())
            {
                // LAN
                folderNameMode = ServerFolderNameMode.NAME_IP;
            }
            else
            {
                // normal multiplayer
                folderNameMode = ServerFolderNameMode.NAME_IP_PORT;
            }
        }
        String serverName = MC.getCurrentServerName().replaceAll(INVALID_FILE_CHARACTERS_REGEX, "");
        String serverMcVersion = MC.getCurrentServerVersion().replaceAll(INVALID_FILE_CHARACTERS_REGEX, "");
        // generate the folder name
        String folderName = "";
        switch (folderNameMode)
        {
            // default and auto shouldn't be used
            // and are just here to make the compiler happy
            default:
            case AUTO:
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






}
