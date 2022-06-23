package com.seibel.lod.core.a7.save.io.render;

import com.seibel.lod.core.a7.datatype.full.FullDatatype;
import com.seibel.lod.core.a7.save.io.file.DataSourceProvider;
import com.seibel.lod.core.a7.pos.DhSectionPos;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.a7.render.RenderDataSource;
import com.seibel.lod.core.a7.render.RenderDataSourceLoader;
import com.seibel.lod.core.util.LodUtil;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class RenderFileHandler implements RenderSourceProvider {
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

    @Override
    public void addScannedFile(Collection<File> detectedFiles) {

    }

    @Override
    public void write(DhSectionPos sectionPos, FullDatatype chunkData) {

    }

    @Override
    public CompletableFuture<Void> flushAndSave() {
        return null;
    }

    @Override
    public void close() {

    }
}
