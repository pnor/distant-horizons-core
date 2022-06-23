package com.seibel.lod.core.a7.level;

import com.seibel.lod.core.a7.save.io.FileScanner;
import com.seibel.lod.core.a7.save.io.file.LocalDataFileHandler;
import com.seibel.lod.core.a7.save.structure.LocalSaveStructure;
import com.seibel.lod.core.wrapperInterfaces.world.ILevelWrapper;

import java.util.concurrent.CompletableFuture;

public class DhServerLevel implements IServerLevel {
    public final LocalSaveStructure save;
    public final LocalDataFileHandler dataFileHandler;
    public final ILevelWrapper level;

    public DhServerLevel(LocalSaveStructure save, ILevelWrapper level) {
        this.save = save;
        this.level = level;
        dataFileHandler = new LocalDataFileHandler(this, save.getDataFolder(level));
        FileScanner.scanFile(save, level, dataFileHandler, null);
    }

    public void tick() {
        //Nothing for now
    }

    @Override
    public int getMinY() {
        return level.getMinHeight();
    }

    @Override
    public void dumpRamUsage() {
        //TODO
    }
    @Override
    public void close() {
        dataFileHandler.close();
    }
    @Override
    public CompletableFuture<Void> save() {
        return dataFileHandler.flushAndSave();
    }

    @Override
    public void doWorldGen() {

    }
}
