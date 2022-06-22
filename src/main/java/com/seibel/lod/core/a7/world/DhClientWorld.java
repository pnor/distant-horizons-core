package com.seibel.lod.core.a7.world;

import com.seibel.lod.core.a7.WorldEnvironment;
import com.seibel.lod.core.a7.level.DhClientServerLevel;
import com.seibel.lod.core.a7.save.structure.ClientOnlySaveStructure;
import com.seibel.lod.core.config.Config;
import com.seibel.lod.core.util.DetailDistanceUtil;
import com.seibel.lod.core.util.EventLoop;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.wrapperInterfaces.world.ILevelWrapper;

import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class DhClientWorld extends DhWorld implements IClientWorld {

    private final HashMap<ILevelWrapper, DhClientWorld> levels;
    public final ClientOnlySaveStructure saveStructure;

    public ExecutorService dhTickerThread = LodUtil.makeSingleThreadPool("DHTickerThread", 2);
    public EventLoop eventLoop = new EventLoop(dhTickerThread, this::tick);

    public DhClientWorld() {
        super(WorldEnvironment.Client_Only);
        saveStructure = new ClientOnlySaveStructure(this);
        levels = new HashMap<>();
    }

    @Override
    public DhClientServerLevel getOrLoadLevel(ILevelWrapper wrapper) {
        if (!levels.containsKey(wrapper)) {
            DhClientWorld level = saveStructure.tryGetLevel(wrapper);
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

    private void tick() {
        int newViewDistance = Config.Client.Graphics.Quality.lodChunkRenderDistance.get() * 16;
        Iterator<DhClientWorld> iterator = levels.values().iterator();
        while (iterator.hasNext()) {
            DhClientWorld level = iterator.next();
            if (level.viewDistance != newViewDistance) {
                level.close();
                iterator.remove();
            }
        }
        DetailDistanceUtil.updateSettings();
    }

    public void asyncTick() {
        eventLoop.tick();
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
