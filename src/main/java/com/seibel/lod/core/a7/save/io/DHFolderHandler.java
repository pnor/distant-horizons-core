package com.seibel.lod.core.a7.save.io;

import com.seibel.lod.core.config.Config;
import com.seibel.lod.core.enums.config.EServerFolderNameMode;
import com.seibel.lod.core.handlers.LodDimensionFinder;
import com.seibel.lod.core.handlers.dependencyInjection.SingletonHandler;
import com.seibel.lod.core.logging.ConfigBasedLogger;
import com.seibel.lod.core.objects.ParsedIp;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
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
    public static final ConfigBasedLogger LOGGER = new ConfigBasedLogger(LogManager.getLogger(LodDimensionFinder.class),
            () -> Config.Client.Advanced.Debugging.DebugSwitch.logFileSubDimEvent.get());

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

        return dimensionFolder;
    }
    private static String getServerFolderName()
    {
        // parse the current server's IP
        ParsedIp parsedIp = new ParsedIp(MC.getCurrentServerIp());
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
