package com.seibel.lod.core.a7;

import com.seibel.lod.core.a7.io.DHFolderHandler;
import com.seibel.lod.core.a7.io.LevelToFileMatcher;
import com.seibel.lod.core.config.Config;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.util.DetailDistanceUtil;
import com.seibel.lod.core.util.EventLoop;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.wrapperInterfaces.world.IWorldWrapper;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;

public class DHWorld implements Closeable {
    private static final Logger LOGGER = DhLoggerBuilder.getLogger("DHWorld");

    private final File saveDir;
    private final HashMap<IWorldWrapper, DHLevel> levels;
    private LevelToFileMatcher levelToFileMatcher = null;

    public ExecutorService dhTickerThread = LodUtil.makeSingleThreadPool("DHTickerThread", 2);
    public EventLoop eventLoop = new EventLoop(dhTickerThread, this::tick);

    public DHWorld() {
        //Note: this changes the singleplayer lod save location.
        saveDir = DHFolderHandler.getCurrentWorldFolder();
        levels = new HashMap<>();
    }

    public DHLevel getOrLoadLevel(IWorldWrapper wrapper) {
        if (!levels.containsKey(wrapper)) {
            if (levelToFileMatcher == null || levelToFileMatcher.getTargetWorld() != wrapper) {
                LOGGER.info("Loading level for world " + wrapper.getDimensionType().getDimensionName());
                levelToFileMatcher = new LevelToFileMatcher(this, saveDir, wrapper);
            }
            DHLevel level = levelToFileMatcher.tryGetLevel();
            if (level != null) {
                levels.put(wrapper, level);
                levelToFileMatcher = null;
                return level;
            } else {
                return null;
            }
        } else return levels.get(wrapper);
    }

    public DHLevel getLevel(IWorldWrapper wrapper) {
        return levels.get(wrapper);
    }

    public void unloadLevel(IWorldWrapper wrapper) {
        if (levels.containsKey(wrapper)) {
            LOGGER.info("Unloading level for world " + wrapper.getDimensionType().getDimensionName());
            levels.get(wrapper).close();
            levels.remove(wrapper);
        }
    }

    public void tick() {
        int newViewDistance = Config.Client.Graphics.Quality.lodChunkRenderDistance.get() * 16;
        Iterator<DHLevel> iterator = levels.values().iterator();
        while (iterator.hasNext()) {
            DHLevel level = iterator.next();
            if (level.viewDistance != newViewDistance) {
                level.close();
                iterator.remove();
            }
        }
        DetailDistanceUtil.updateSettings();
    }
    public void doWorldGen() {
    }
    public void asyncTick() {
        eventLoop.tick();
    }

    public void save() {
        for (DHLevel level : levels.values()) {
            level.saveFlush();
        }
    }

    @Override
    public void close() {
        eventLoop.halt();
        for (DHLevel level : levels.values()) {
            LOGGER.info("Unloading level for world " + level.level.getDimensionType().getDimensionName());
            level.close();
        }
        levels.clear();
    }
}
