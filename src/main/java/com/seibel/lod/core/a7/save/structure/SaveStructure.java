package com.seibel.lod.core.a7.save.structure;

import com.seibel.lod.core.a7.io.LevelToFileMatcher;
import com.seibel.lod.core.a7.level.DHLevel;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.wrapperInterfaces.world.ILevelWrapper;
import org.apache.logging.log4j.Logger;

import java.io.File;

public abstract class SaveStructure implements AutoCloseable {

    protected static final Logger LOGGER = DhLoggerBuilder.getLogger();

    public abstract DHLevel tryGetLevel(ILevelWrapper wrapper);

    protected abstract File getRenderCacheFolder(ILevelWrapper world);
    protected abstract File getDataFolder(ILevelWrapper world);
}

