package com.seibel.lod.core.config.file;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.seibel.lod.core.ModInfo;
import com.seibel.lod.core.config.ConfigBase;
import com.seibel.lod.core.config.types.AbstractConfigType;
import com.seibel.lod.core.config.types.ConfigEntry;
import com.seibel.lod.core.util.SingletonHandler;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftWrapper;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

/**
 * Handles all stuff to do with the files
 *
 * @author coolGi2007
 */
public class ConfigFileHandling {
    public static final Path ConfigPath = SingletonHandler.get(IMinecraftWrapper.class).getGameDirectory().toPath().resolve("config").resolve(ModInfo.NAME+".toml");

    /** Saves the config to the file */
    public static void saveToFile() {
        CommentedFileConfig config = CommentedFileConfig.builder(ConfigPath.toFile()).build();
        if (!Files.exists(ConfigPath)) // Try to check if the config exists
            try {
                Files.createFile(ConfigPath);
            } catch (IOException ex) {
                ex.printStackTrace();
            }

        loadConfig(config);

        for (AbstractConfigType<?, ?> entry : ConfigBase.entries) {
            if (ConfigEntry.class.isAssignableFrom(entry.getClass())) {
                createComment((ConfigEntry<?>) entry, config);
                saveEntry((ConfigEntry<?>) entry, config);
            }
        }

        config.save();
        config.close();
    }
    /** Loads the config from the file */
    public static void loadFromFile() {
        CommentedFileConfig config = CommentedFileConfig.builder(ConfigPath.toFile()).build();
        // Attempt to load the file and if it fails then save config to file
        try {
            if (Files.exists(ConfigPath))
                config.load();
            else {
                saveToFile();
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            saveToFile();
            return;
        }

        // Load all the entries
        for (AbstractConfigType<?, ?> entry : ConfigBase.entries) {
            if (ConfigEntry.class.isAssignableFrom(entry.getClass())) {
                createComment((ConfigEntry<?>) entry, config);
                loadEntry((ConfigEntry<?>) entry, config);
            }
        }

        config.save();
        config.close();
    }




    // Save an entry when only given the entry
    public static void saveEntry(ConfigEntry<?> entry) {
        CommentedFileConfig config = CommentedFileConfig.builder(ConfigPath.toFile()).build();
        loadConfig(config);
        saveEntry(entry, config);
        config.save();
        config.close();
    }
    // Save an entry
    @SuppressWarnings("unchecked")
    public static void saveEntry(ConfigEntry<?> entry, CommentedFileConfig workConfig) {
        if (!entry.getAppearance().showInFile)
            return;
        if (!entry.get().getClass().isAssignableFrom(HashMap.class)) {
            workConfig.set(entry.getNameWCategory(), entry.get());
        } else {
            workConfig.set(entry.getNameWCategory(), getStringFromHashMap((HashMap<String, ?>) entry.get()));
        }
    }

    // Loads an entry when only given the entry
    public static void loadEntry(ConfigEntry<?> entry) {
        CommentedFileConfig config = CommentedFileConfig.builder(ConfigPath.toFile()).autosave().build();
        loadConfig(config);
        loadEntry(entry, config);
        config.close();

    }
    // Loads an entry
    @SuppressWarnings("unchecked") // Suppress due to its always safe. (I think. See reasons below.)
	public static <T> void loadEntry(ConfigEntry<T> entry, CommentedFileConfig workConfig) {
        if (!entry.getAppearance().showInFile)
            return;
        if (workConfig.contains(entry.getNameWCategory())) {
            if (entry.get().getClass().isEnum()) {
                // Safe cast due to above checking that <T> is indeed a Enum
                // And the second cast back to <T> is safe due to the template
                entry.setWTSave((T) (
                        workConfig.getEnum(entry.getNameWCategory(), (Class<? extends Enum>) entry.get().getClass())
                ));
            } else if (entry.get().getClass().isAssignableFrom(HashMap.class)) {
                entry.setWTSave((T) getHashMapFromString(workConfig.get(entry.getNameWCategory())));
            } else {
                entry.setWTSave(workConfig.get(entry.getNameWCategory()));
            }
        } else {
            saveEntry(entry, workConfig);
        }
    }

    // Creates the comment for an entry when only given the entry
    public static void createComment(ConfigEntry<?> entry) {
        CommentedFileConfig config = CommentedFileConfig.builder(ConfigPath.toFile()).autosave().build();
        loadConfig(config);
        createComment(entry, config);
        config.close();
    }
    // Creates a comment for an entry
    public static void createComment(ConfigEntry<?> entry, CommentedFileConfig workConfig) {
        if (!entry.getAppearance().showInFile)
            return;
        workConfig.setComment(entry.getNameWCategory(), entry.getComment());
    }




    /** Does config.load(); but with error checking */
    public static void loadConfig(CommentedFileConfig config) {
        try {
            config.load();
        } catch (Exception e) {
            System.out.println("Loading file failed because of this expectation:\n"+e);
            try { // Now try remaking the file and loading it
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

    // Stuff for converting HashMap's and String's (uses json)
    public static String getStringFromHashMap(HashMap<String, ?> item) {
        JSONObject jsonObject = new JSONObject();

        for (int i=0; i< item.size(); i++) {
            jsonObject.put(item.keySet().toArray()[i], item.get(item.keySet().toArray()[i]));
        }

        return jsonObject.toJSONString();
    }
    public static <T> HashMap<String, ?> getHashMapFromString(String s) {
        HashMap<String, T> map = new HashMap<>();

        JSONObject jsonObject = null;
        try {
            jsonObject = (JSONObject) new JSONParser().parse(s);
        } catch (ParseException p) {
            p.printStackTrace();
        }

        for (int i = 0; i < jsonObject.keySet().toArray().length; i++) {
            map.put((String) jsonObject.keySet().toArray()[i], (T) jsonObject.get(jsonObject.keySet().toArray()[i]));
        }
        return map;
    }
}
