package com.seibel.lod.core.a7.world;

import com.seibel.lod.core.a7.WorldEnvironment;
import com.seibel.lod.core.a7.level.DhClientServerLevel;
import com.seibel.lod.core.a7.save.structure.LocalSaveStructure;
import com.seibel.lod.core.config.Config;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.wrapperInterfaces.world.ILevelWrapper;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;

public class DhClientServerWorld extends DhWorld implements IClientWorld, IServerWorld {
    private final HashMap<ILevelWrapper, DhClientServerLevel> levels;
    public final LocalSaveStructure saveStructure;

    public DhClientServerWorld() {
        super(WorldEnvironment.Client_Server);
        saveStructure = new LocalSaveStructure();
        levels = new HashMap<>();
    }

    @Override
    public DhClientServerLevel getOrLoadLevel(ILevelWrapper wrapper) {
        return levels.computeIfAbsent(wrapper, (w) -> {
            File levelFile = saveStructure.tryGetLevelFolder(w);
            LodUtil.assertTrue(levelFile != null);
            return new DhClientServerLevel(saveStructure, w);
        });
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
            if (level.tree.viewDistance != newViewDistance) {
                level.close();
                iterator.remove();
            }
        }
        //DetailDistanceUtil.updateSettings();
        levels.values().forEach(DhClientServerLevel::tick);
    }

    public void doWorldGen() {
        levels.values().forEach(DhClientServerLevel::doWorldGen);
    }

    @Override
    public CompletableFuture<Void> saveAndFlush() {
        return CompletableFuture.allOf(levels.values().stream().map(DhClientServerLevel::save).toArray(CompletableFuture[]::new));
    }

    @Override
    public void close() {
        saveAndFlush().join();
        for (DhClientServerLevel level : levels.values()) {
            LOGGER.info("Unloading level for world " + level.level.getDimensionType().getDimensionName());
            level.close();
        }
        levels.clear();
    }
}