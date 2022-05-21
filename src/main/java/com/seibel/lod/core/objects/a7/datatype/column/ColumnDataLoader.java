package com.seibel.lod.core.objects.a7.datatype.column;

import com.seibel.lod.core.Config;
import com.seibel.lod.core.enums.config.VerticalQuality;
import com.seibel.lod.core.objects.a7.DHLevel;
import com.seibel.lod.core.objects.a7.data.DataFileHandler;
import com.seibel.lod.core.objects.a7.data.DataSourceLoader;
import com.seibel.lod.core.objects.a7.data.LodDataSource;
import com.seibel.lod.core.objects.a7.pos.DhSectionPos;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Inherited;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ColumnDataLoader extends DataSourceSaver {
    private static final byte COLUMN_DATA_LOADER_VERSION = 10;
    public static final ColumnDataLoader INSTANCE = new ColumnDataLoader();

    private ColumnDataLoader() {
        super(ColumnDatatype.class, ColumnDatatype.DATA_TYPE_ID, COLUMN_DATA_LOADER_VERSION);
    }

    @Override
    public LodDataSource loadData(DHLevel level, DhSectionPos sectionPos, InputStream data) {
        //TODO: Add decompressor here
        return ColumnDatatype.loadFile(level, sectionPos, data, COLUMN_DATA_LOADER_VERSION);
    }

    @Override
    public void saveData(DHLevel level, LodDataSource loadedData, DataOutputStream out) throws IOException {
        //TODO: Add compressor here
        ((ColumnDatatype) loadedData).writeData(out);
    }

    @Override
    public File generateFilePathAndName(File levelFolderPath, DHLevel level, DhSectionPos sectionPos) {
        return generateFilePathAndName(levelFolderPath, sectionPos, Config.Client.Graphics.Quality.verticalQuality.get());
    }

    public File generateFilePathAndName(File levelFolderPath, DhSectionPos sectionPos, VerticalQuality quality) {
        return new File(levelFolderPath, "cache" + File.separator + quality.toString() + File.separator +
                String.format("%s_v%d-%s%s", clazz.getSimpleName(), loaderVersion,
                sectionPos.serialize(), DataFileHandler.FILE_EXTENSION));
    }

    @Override
    public List<File> foldersToScan(File levelFolderPath) {
        File cacheFolder = new File(levelFolderPath, "cache");
        List<File> foldersToScan = new ArrayList<>(VerticalQuality.values().length);
        for (VerticalQuality q : VerticalQuality.values()) {
            foldersToScan.add(new File(cacheFolder, q.toString()));
        }
        return foldersToScan;
    }
}
