package com.seibel.lod.core.a7.save.io.render;

import com.seibel.lod.core.a7.RenderDataProvider;
import com.seibel.lod.core.a7.datatype.full.FullDatatype;
import com.seibel.lod.core.a7.pos.DhSectionPos;

import java.io.File;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public interface RenderSourceProvider extends RenderDataProvider, AutoCloseable {
    void addScannedFile(Collection<File> detectedFiles);
    void write(DhSectionPos sectionPos, FullDatatype chunkData);
    CompletableFuture<Void> flushAndSave();
}
