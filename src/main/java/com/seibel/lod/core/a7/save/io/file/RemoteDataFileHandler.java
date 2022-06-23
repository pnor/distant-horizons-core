package com.seibel.lod.core.a7.save.io.file;

import com.seibel.lod.core.a7.datatype.LodDataSource;
import com.seibel.lod.core.a7.datatype.full.FullFormat;
import com.seibel.lod.core.a7.pos.DhSectionPos;

import java.io.File;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class RemoteDataFileHandler implements IDataSourceProvider {
    @Override
    public void addScannedFile(Collection<File> detectedFiles) {

    }

    @Override
    public CompletableFuture<LodDataSource> read(DhSectionPos pos) {
        return null;
    }

    @Override
    public void write(DhSectionPos sectionPos, FullFormat chunkData) {

    }

    @Override
    public CompletableFuture<Void> flushAndSave() {
        return null;
    }

    @Override
    public void close() throws Exception {

    }
}
