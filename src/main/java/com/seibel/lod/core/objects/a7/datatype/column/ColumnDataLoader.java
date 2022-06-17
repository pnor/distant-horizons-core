package com.seibel.lod.core.objects.a7.datatype.column;

import com.seibel.lod.core.config.Config;
import com.seibel.lod.core.enums.config.EVerticalQuality;
import com.seibel.lod.core.objects.a7.DHLevel;
import com.seibel.lod.core.objects.a7.data.DataFileHandler;
import com.seibel.lod.core.objects.a7.data.LodDataSource;
import com.seibel.lod.core.objects.a7.io.MetaFile;
import com.seibel.lod.core.objects.a7.io.file.DataMetaFile;
import com.seibel.lod.core.objects.a7.pos.DhSectionPos;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ColumnDataLoader extends DataSourceSaver {
    private static final byte COLUMN_DATA_LOADER_VERSION = 1;
    public static final ColumnDataLoader INSTANCE = new ColumnDataLoader();

    private ColumnDataLoader() {
        super(ColumnDatatype.class, ColumnDatatype.DATA_TYPE_ID, new byte[]{COLUMN_DATA_LOADER_VERSION});
    }

    @Override
    public LodDataSource loadData(DataMetaFile dataFile, InputStream data, DHLevel level) {
        try (
                //TODO: Add decompressor here
                DataInputStream dis = new DataInputStream(data);
             ) {
            return new ColumnDatatype(dataFile.pos, dis, dataFile.loaderVersion, level);
        } catch (IOException e) {
            //FIXME: Log error
            return null;
        }
    }

    @Override
    public void saveData(DHLevel level, LodDataSource loadedData, MetaFile file, OutputStream out) throws IOException {
        //TODO: Add compressor here
        try (DataOutputStream dos = new DataOutputStream(out)) {
            ((ColumnDatatype) loadedData).writeData(dos);
        }
    }

    @Override
    public File generateFilePathAndName(File levelFolderPath, DHLevel level, DhSectionPos sectionPos) {
        return generateFilePathAndName(levelFolderPath, sectionPos, Config.Client.Graphics.Quality.verticalQuality.get());
    }

    public File generateFilePathAndName(File levelFolderPath, DhSectionPos sectionPos, EVerticalQuality quality) {
        return new File(levelFolderPath, "cache" + File.separator + quality.toString() + File.separator +
                String.format("%s_v%d-%s%s", clazz.getSimpleName(), COLUMN_DATA_LOADER_VERSION,
                sectionPos.serialize(), DataFileHandler.FILE_EXTENSION));
    }

    @Override
    public List<File> foldersToScan(File levelFolderPath) {
        File cacheFolder = new File(levelFolderPath, "cache");
        List<File> foldersToScan = new ArrayList<>(EVerticalQuality.values().length);
        for (EVerticalQuality q : EVerticalQuality.values()) {
            foldersToScan.add(new File(cacheFolder, q.toString()));
        }
        return foldersToScan;
    }
}
