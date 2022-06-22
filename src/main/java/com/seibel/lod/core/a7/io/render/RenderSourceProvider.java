package com.seibel.lod.core.a7.io.render;

import com.seibel.lod.core.a7.RenderDataProvider;
import com.seibel.lod.core.a7.datatype.full.FullDatatype;
import com.seibel.lod.core.a7.pos.DhSectionPos;

import java.util.concurrent.CompletableFuture;

public interface RenderSourceProvider extends RenderDataProvider, AutoCloseable {
    void write(DhSectionPos sectionPos, FullDatatype chunkData);
    CompletableFuture<Void> flushAndSave();
}
