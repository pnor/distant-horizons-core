package com.seibel.lod.core.a7.save.io.render;

import com.seibel.lod.core.a7.datatype.RenderSourceLoader;
import com.seibel.lod.core.a7.datatype.full.Data;
import com.seibel.lod.core.a7.save.io.file.IDataSourceProvider;
import com.seibel.lod.core.a7.pos.DhSectionPos;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.a7.datatype.LodRenderSource;
import com.seibel.lod.core.util.LodUtil;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class RenderFileHandler implements IRenderSourceProvider {
    final File renderCacheFolder;
    final IDataSourceProvider dataSourceProvider;
    ExecutorService renderCacheThread = LodUtil.makeSingleThreadPool("RenderCacheThread");
    Logger logger = DhLoggerBuilder.getLogger("RenderCache");

    public RenderFileHandler(IDataSourceProvider sourceProvider, File renderCacheFolder) {
        this.dataSourceProvider = sourceProvider;
        this.renderCacheFolder = renderCacheFolder;
    }

    @Override
    public CompletableFuture<LodRenderSource> createRenderData(RenderSourceLoader renderSourceLoader, DhSectionPos pos) {
        return null;
    }

    @Override
    public void addScannedFile(Collection<File> detectedFiles) {

    }

    @Override
    public void write(DhSectionPos sectionPos, Data chunkData) {

    }

    @Override
    public CompletableFuture<Void> flushAndSave() {
        return null;
    }

    @Override
    public void close() {

    }
}
