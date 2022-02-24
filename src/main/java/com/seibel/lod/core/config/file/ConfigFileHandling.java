package com.seibel.lod.core.config.file;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.seibel.lod.core.ModInfo;
import com.seibel.lod.core.config.ConfigEntry;
import com.seibel.lod.core.util.SingletonHandler;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftWrapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Handles all stuff to do with the files
 *
 * @author coolGi2007
 */
public class ConfigFileHandling {
    public static final Path ConfigPath = SingletonHandler.get(IMinecraftWrapper.class).getGameDirectory().toPath().resolve("config").resolve(ModInfo.NAME+".toml");
    public static final CommentedFileConfig config = CommentedFileConfig.builder(ConfigPath.toFile()).autosave().build();

    public static void saveToFile() {
        if (!Files.exists(ConfigPath))
            try {
                Files.createFile(ConfigPath);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        loadConfig(config);

        config.close();
    }

    public static void loadFromFile() {
        try {
            config.load();
        } catch (Exception e) {
            saveToFile();
            return;
        }

        config.close();
    }

    public static void saveEntry(ConfigEntry<?> entry) {
        loadConfig(config);
        saveEntry(entry, config);
        config.close();
    }
    public static void saveEntry(ConfigEntry<?> entry, CommentedFileConfig workConfig) {

    }
    public static void loadEntry(ConfigEntry<?> entry) {
        loadConfig(config);
        loadEntry(entry, config);
        config.close();

    }
    public static void loadEntry(ConfigEntry<?> entry, CommentedFileConfig workConfig) {

    }

    public static void loadConfig(CommentedFileConfig config) {
        try {
            config.load();
        } catch (Exception e) {
            System.out.println("Loading file failed because of this expectation:\n"+e);
            try { // Now try remaking the file
                Files.deleteIfExists(ConfigPath);
                Files.createFile(ConfigPath);
                config.load();
            } catch (IOException ex) {
                System.out.println("Creating file failed");
                ex.printStackTrace();
                SingletonHandler.get(IMinecraftWrapper.class).crashMinecraft("Loading file and resetting config file failed at path ["+ConfigPath+"]. Please check the file is ok and you have the permissions", ex);
            }
        }
    }
}
