package com.seibel.lod.core.a7.save.io.render;

import com.seibel.lod.core.a7.datatype.full.FullFormat;
import com.seibel.lod.core.a7.pos.DhSectionPos;

import java.io.File;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public interface IRenderSourceProvider extends AutoCloseable {
    void addScannedFile(Collection<File> detectedFiles);
    void write(DhSectionPos sectionPos, FullFormat chunkData);
    CompletableFuture<Void> flushAndSave();
}
