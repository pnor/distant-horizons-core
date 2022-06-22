package com.seibel.lod.core.a7.io.render;

import com.seibel.lod.core.a7.data.LodDataSource;
import com.seibel.lod.core.a7.datatype.full.FullDatatype;
import com.seibel.lod.core.a7.pos.DhSectionPos;
import com.seibel.lod.core.a7.render.RenderDataSource;

import java.util.concurrent.CompletableFuture;

public interface RenderSourceProvider {
    CompletableFuture<RenderDataSource> get(DhSectionPos pos);
    void write(DhSectionPos sectionPos, FullDatatype chunkData);
    CompletableFuture<Void> flushAndSave();
}
