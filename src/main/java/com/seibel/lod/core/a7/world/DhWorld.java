package com.seibel.lod.core.a7.world;

import com.seibel.lod.core.a7.WorldEnvironment;
import com.seibel.lod.core.a7.io.LevelToFileMatcher;
import com.seibel.lod.core.a7.level.DHLevel;
import com.seibel.lod.core.a7.save.structure.SaveStructure;
import com.seibel.lod.core.config.Config;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.util.DetailDistanceUtil;
import com.seibel.lod.core.util.EventLoop;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.wrapperInterfaces.world.ILevelWrapper;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;

public abstract class DhWorld implements Closeable {
    protected static final Logger LOGGER = DhLoggerBuilder.getLogger();

    public final WorldEnvironment environment;

    protected DhWorld(WorldEnvironment environment) {
        this.environment = environment;
    }
    public abstract DHLevel getOrLoadLevel(ILevelWrapper wrapper);

    public abstract DHLevel getLevel(ILevelWrapper wrapper);

    public abstract void unloadLevel(ILevelWrapper wrapper);
    public abstract void saveAndFlush();
}
