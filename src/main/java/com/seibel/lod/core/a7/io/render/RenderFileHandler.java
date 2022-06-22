package com.seibel.lod.core.a7.io.render;

import com.seibel.lod.core.a7.RenderDataProvider;
import com.seibel.lod.core.a7.datatype.full.FullDatatype;
import com.seibel.lod.core.a7.io.file.DataSourceProvider;
import com.seibel.lod.core.a7.pos.DhSectionPos;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.a7.render.RenderDataSource;
import com.seibel.lod.core.a7.render.RenderDataSourceLoader;
import com.seibel.lod.core.util.LodUtil;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class RenderFileHandler implements RenderDataProvider {
    final File renderCacheFolder;
    final DataSourceProvider dataSourceProvider;
    ExecutorService renderCacheThread = LodUtil.makeSingleThreadPool("RenderCacheThread");
    Logger logger = DhLoggerBuilder.getLogger("RenderCache");

    public RenderFileHandler(DataSourceProvider sourceProvider, File renderCacheFolder) {
        this.dataSourceProvider = sourceProvider;
        this.renderCacheFolder = renderCacheFolder;
    }

    @Override
    public CompletableFuture<RenderDataSource> createRenderData(RenderDataSourceLoader renderSourceLoader, DhSectionPos pos) {
        return null;
    }

    public CompletableFuture<RenderDataSource> read(DhSectionPos pos) {
        return null;
    }

    public void write(DhSectionPos sectionPos, FullDatatype chunkData) {
        `
    }
}
