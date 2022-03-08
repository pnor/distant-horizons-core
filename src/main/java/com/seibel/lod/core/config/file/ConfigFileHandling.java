package com.seibel.lod.core.config.file;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.seibel.lod.core.ModInfo;
import com.seibel.lod.core.config.ConfigBase;
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

        for (ConfigEntry<?> entry : ConfigBase.entries) {
            createComment(entry, config);
            saveEntry(entry, config);
        }

        config.close();
    }

    public static void loadFromFile() {
        try {
            if (Files.exists(ConfigPath))
                config.load();
            else {
                saveToFile();
                return;
            }
        } catch (Exception e) {
            saveToFile();
            return;
        }

        for (ConfigEntry<?> entry : ConfigBase.entries) {
            createComment(entry, config);
            loadEntry(entry, config);
            System.out.println(entry.get());
        }

        config.close();
    }

    public static void saveEntry(ConfigEntry<?> entry) {
        loadConfig(config);
        saveEntry(entry, config);
        config.close();
    }
    public static void saveEntry(ConfigEntry<?> entry, CommentedFileConfig workConfig) {
        workConfig.set(entry.getNameWCategory(), entry.get());
    }
    public static void loadEntry(ConfigEntry<?> entry) {
        loadConfig(config);
        loadEntry(entry, config);
        config.close();

    }
    
    @SuppressWarnings("unchecked") // Suppress due to its always safe. (I think. See reasons below.)
	public static <T> void loadEntry(ConfigEntry<T> entry, CommentedFileConfig workConfig) {
        if (workConfig.contains(entry.getNameWCategory())) {
            if (entry.get().getClass().isEnum()) {
            	// Safe cast due to above checking that <T> is indeed a Enum
            	// And the second cast back to <T> is safe due to the template
                entry.setWTSave((T)(
                		workConfig.getEnum(entry.getNameWCategory(), (Class<? extends Enum>) entry.get().getClass())
                	));
            } else {
                entry.setWTSave(workConfig.get(entry.getNameWCategory()));
            }
            System.out.println(workConfig.get(entry.getNameWCategory()).getClass().toString());
//            entry.setWTSave(workConfig.get(entry.getNameWCategory()));
        } else {
            saveEntry(entry, workConfig);
        }
    }

    public static void createComment(ConfigEntry<?> entry) {
        loadConfig(config);
        createComment(entry, config);
        config.close();
    }
    public static void createComment(ConfigEntry<?> entry, CommentedFileConfig workConfig) {
        workConfig.setComment(entry.getNameWCategory(), entry.getComment());
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
