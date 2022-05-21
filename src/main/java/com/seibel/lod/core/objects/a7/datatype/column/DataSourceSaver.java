package com.seibel.lod.core.objects.a7.datatype.column;

import com.seibel.lod.core.objects.a7.DHLevel;
import com.seibel.lod.core.objects.a7.data.DataFileHandler;
import com.seibel.lod.core.objects.a7.data.DataSourceLoader;
import com.seibel.lod.core.objects.a7.data.LodDataSource;
import com.seibel.lod.core.objects.a7.pos.DhSectionPos;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

public abstract class DataSourceSaver extends DataSourceLoader {
    public DataSourceSaver(Class<? extends LodDataSource> clazz, long datatypeId, byte loaderVersion) {
        super(clazz, datatypeId, loaderVersion);
    }
    public abstract void saveData(DHLevel level, LodDataSource loadedData, DataOutputStream out) throws IOException;
    // generate the default file path and file name based on various parameters.
    // Ensure the file extension is '.lod'!
    public File generateFilePathAndName(File levelFolderPath, DHLevel level, DhSectionPos sectionPos) {
        return new File(levelFolderPath, String.format("%s_v%d-%s%s", clazz.getSimpleName(), loaderVersion,
                sectionPos.serialize(), DataFileHandler.FILE_EXTENSION));
    }
}
