package com.seibel.lod.core.objects.a7;

import com.seibel.lod.core.objects.a7.pos.DhSectionPos;
import com.seibel.lod.core.objects.a7.render.RenderDataSource;

import java.util.concurrent.CompletableFuture;

public interface RenderDataProvider {
    CompletableFuture<RenderDataSource> createRenderData(RenderDataSource.RenderDataSourceLoader renderSourceLoader, DhSectionPos pos);
}
