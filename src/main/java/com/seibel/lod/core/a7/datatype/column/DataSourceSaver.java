package com.seibel.lod.core.a7.datatype.column;

import com.seibel.lod.core.a7.data.DataFileHandler;
import com.seibel.lod.core.a7.data.DataSourceLoader;
import com.seibel.lod.core.a7.data.LodDataSource;
import com.seibel.lod.core.a7.io.MetaFile;
import com.seibel.lod.core.a7.pos.DhSectionPos;
import com.seibel.lod.core.a7.level.DHLevel;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

public abstract class DataSourceSaver extends DataSourceLoader {
    public DataSourceSaver(Class<? extends LodDataSource> clazz, long datatypeId, byte[] loaderSupportedVersions) {
        super(clazz, datatypeId, loaderSupportedVersions);
    }

    public abstract void saveData(DHLevel level, LodDataSource loadedData, MetaFile file, OutputStream dataStream) throws IOException;
    // generate the default file path and file name based on various parameters.
    // Ensure the file extension is '.lod'!
    public File generateFilePathAndName(File levelFolderPath, DHLevel level, DhSectionPos sectionPos) {
        return new File(levelFolderPath, String.format("%s_v%d-%s%s", clazz.getSimpleName(), getSaverVersion(),
                sectionPos.serialize(), DataFileHandler.FILE_EXTENSION));
    }

    public byte getSaverVersion() {
        return loaderSupportedVersions[loaderSupportedVersions.length - 1];
    }
}
