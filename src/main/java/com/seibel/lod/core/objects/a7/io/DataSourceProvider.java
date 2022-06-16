package com.seibel.lod.core.objects.a7.io;

import com.seibel.lod.core.objects.a7.data.LodDataSource;
import com.seibel.lod.core.objects.a7.datatype.full.FullDatatype;
import com.seibel.lod.core.objects.a7.pos.DhSectionPos;

import java.util.concurrent.CompletableFuture;

public interface DataSourceProvider {
    CompletableFuture<LodDataSource> read(DhSectionPos pos);
    void write(DhSectionPos sectionPos, FullDatatype chunkData);
    CompletableFuture<Void> flushAndSave();
}
