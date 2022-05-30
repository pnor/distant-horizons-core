package com.seibel.lod.core.objects.a7.datatype.column;

import com.seibel.lod.core.objects.a7.data.DataSourceLoader;
import com.seibel.lod.core.objects.a7.data.LodDataSource;

public abstract class OldDataSourceLoader extends DataSourceLoader {

    // Note: clazz can be null if the class no longer exists, as long as
    // the datatypeId have not been changed or overwritten.
    public OldDataSourceLoader(Class<? extends LodDataSource> clazz, long datatypeId, byte[] loaderVersions) {
        super(clazz, datatypeId, loaderVersions);
    }
    abstract public DataSourceSaver getNewSaver();
}
