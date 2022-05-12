package com.seibel.lod.core.objects.a7.data;

import com.seibel.lod.core.objects.a7.RenderDataProvider;
import com.seibel.lod.core.objects.a7.pos.DhSectionPos;
import com.seibel.lod.core.objects.a7.render.EmptyRenderContainer;
import com.seibel.lod.core.objects.a7.render.RenderDataSource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;

public class DataFileHandler implements RenderDataProvider {
    public final File folder;
    private final HashMap<DhSectionPos, LodDataSource> dataSourceCache;

    public DataFileHandler(File folderPath) {
        this.folder = folderPath;
        dataSourceCache = new HashMap<>();
    }

    @Override
    public RenderDataSource createRenderData(DhSectionPos pos) {
        LodDataSource dataSource = getDataSource(pos);
        RenderDataSource renderDataSource = RenderDataSource.tryConstruct(dataSource, pos);
        if (renderDataSource == null) renderDataSource = EmptyRenderContainer.INSTANCE;
        return renderDataSource;
    }

    private LodDataSource getDataSource(DhSectionPos pos) {
        return dataSourceCache.computeIfAbsent(pos, this::loadOrCreateDataSource);
    }

    private LodDataSource loadOrCreateDataSource(DhSectionPos pos) {
        File dataFile = getDataFile(pos);
        if (dataFile.exists()) {
            String format = getFormat(dataFile);
            try {
                LodDataSource data = LodDataSource.loadData(format, new FileInputStream(dataFile));
                return data;
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        return new CompleteDataContainer();
    }

    private String getFormat(File targetFile) {
        return null; //TODO
    }

    private File getDataFile(DhSectionPos pos) {
        return null; //TODO
    }

}
