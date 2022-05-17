package com.seibel.lod.core.objects.a7;

import com.seibel.lod.core.objects.a7.pos.DhSectionPos;
import com.seibel.lod.core.objects.a7.render.RenderDataSource;
import com.seibel.lod.core.objects.a7.render.RenderDataSourceLoader;

import java.util.concurrent.CompletableFuture;

public interface RenderDataProvider {
    CompletableFuture<RenderDataSource> createRenderData(RenderDataSourceLoader renderSourceLoader, DhSectionPos pos);
}
