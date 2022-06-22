package com.seibel.lod.core.a7.world;

import com.seibel.lod.core.a7.WorldEnvironment;
import com.seibel.lod.core.a7.level.DhClientServerLevel;
import com.seibel.lod.core.a7.save.structure.LocalSaveStructure;
import com.seibel.lod.core.config.Config;
import com.seibel.lod.core.util.DetailDistanceUtil;
import com.seibel.lod.core.wrapperInterfaces.world.ILevelWrapper;

import java.util.HashMap;
import java.util.Iterator;

public class DhServerWorld extends DhWorld implements IServerWorld {
    private final HashMap<ILevelWrapper, DhClientServerLevel> levels;
    public final LocalSaveStructure saveStructure;

    public DhServerWorld() {
        super(WorldEnvironment.Server_Only);
        saveStructure = new LocalSaveStructure();
        levels = new HashMap<>();
    }

    @Override
    public DhClientServerLevel getOrLoadLevel(ILevelWrapper wrapper) {
        if (!levels.containsKey(wrapper)) {
            DhClientServerLevel level = saveStructure.tryGetLevel(wrapper);
            if (level != null) {
                levels.put(wrapper, level);
            }
            return level;
        } else return levels.get(wrapper);
    }

    @Override
    public DhClientServerLevel getLevel(ILevelWrapper wrapper) {
        return levels.get(wrapper);
    }

    @Override
    public void unloadLevel(ILevelWrapper wrapper) {
        if (levels.containsKey(wrapper)) {
            LOGGER.info("Unloading level for world " + wrapper.getDimensionType().getDimensionName());
            levels.get(wrapper).close();
            levels.remove(wrapper).close();
        }
    }

    public void tick() {
        int newViewDistance = Config.Client.Graphics.Quality.lodChunkRenderDistance.get() * 16;
        Iterator<DhClientServerLevel> iterator = levels.values().iterator();
        while (iterator.hasNext()) {
            DhClientServerLevel level = iterator.next();
            if (level.viewDistance != newViewDistance) {
                level.close();
                iterator.remove();
            }
        }
        DetailDistanceUtil.updateSettings();
    }

    public void doWorldGen() {

    }

    @Override
    public void saveAndFlush() {
        for (DhClientServerLevel level : levels.values()) {
            level.saveFlush();
        }
    }

    @Override
    public void close() {
        for (DhClientServerLevel level : levels.values()) {
            LOGGER.info("Unloading level for world " + level.level.getDimensionType().getDimensionName());
            level.close();
        }
        levels.clear();
    }



}
