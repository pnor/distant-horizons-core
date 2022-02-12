package com.seibel.lod.core.config;

import com.seibel.lod.core.ModInfo;
import com.seibel.lod.core.util.SingletonHandler;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftWrapper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Config class should extend this
 *
 * @author coolGi2007
 */
public abstract class ConfigBase {
    public static final File ConfigPath = SingletonHandler.get(IMinecraftWrapper.class).getGameDirectory().toPath().resolve("config").resolve(ModInfo.NAME+".toml").toFile();
    public static final List<ConfigEntry> entries = new ArrayList<>();

    public static void init(Class<?> config) {
        initNestedClass(config);

        // File handling (load from file)
    }

    private static void initNestedClass(Class<?> config) {
        // Put all the entries in entries
    }
}
