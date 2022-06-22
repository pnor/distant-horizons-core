package com.seibel.lod.core.a7.io.file;

import com.seibel.lod.core.a7.data.LodDataSource;
import com.seibel.lod.core.a7.datatype.full.FullDatatype;
import com.seibel.lod.core.a7.pos.DhSectionPos;

import java.util.concurrent.CompletableFuture;

public class RemoteDataFileHandler implements DataSourceProvider {
    @Override
    public CompletableFuture<LodDataSource> read(DhSectionPos pos) {
        return null;
    }

    @Override
    public void write(DhSectionPos sectionPos, FullDatatype chunkData) {

    }

    @Override
    public CompletableFuture<Void> flushAndSave() {
        return null;
    }

    @Override
    public void close() throws Exception {

    }
}
