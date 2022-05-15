package com.seibel.lod.core.objects.a7.data;

import com.seibel.lod.core.objects.a7.DHLevel;
import com.seibel.lod.core.objects.a7.pos.DhSectionPos;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Objects;
import java.util.function.Function;

public interface LodDataSource {
    class LoaderKey {
        public final long classId;
        public final byte loaderVersion;
        public LoaderKey(long classId, byte loaderVersion) {
            this.classId = classId;
            this.loaderVersion = loaderVersion;
        }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LoaderKey loaderKey = (LoaderKey) o;
            return classId == loaderKey.classId && loaderVersion == loaderKey.loaderVersion;
        }
        @Override
        public int hashCode() {
            return Objects.hash(classId, loaderVersion);
        }
    }

    HashMap<LoaderKey, DataSourceLoader>
            dataSourceLoaderRegistry = new HashMap<LoaderKey, DataSourceLoader>();

    HashMap<Long, Class<?>> dataSourceTypeRegistry = new HashMap<Long, Class<?>>();

    interface DataSourceLoader {
        // Can return null as meaning the requirement is not met
        LodDataSource loadData(DHLevel level, DhSectionPos sectionPos, InputStream data);
    }

    static void registerDataSourceLoader(Class<? extends LodDataSource> clazz, long typeId, byte version, DataSourceLoader loader) {
        if (loader == null) {
            throw new IllegalArgumentException("loader must be non-null");
        }
        if (dataSourceTypeRegistry.containsKey(typeId) && dataSourceTypeRegistry.get(typeId) != clazz) {
            throw new IllegalArgumentException("Loader for typeId " + typeId + " already registered with different class: "
                    + dataSourceTypeRegistry.get(typeId) + " != " + clazz);
        }
        LoaderKey key = new LoaderKey(typeId, version);
        if (dataSourceLoaderRegistry.containsKey(key)) {
            throw new IllegalArgumentException("Data source loader already registered for " + clazz + " with version " + version);
        }
        dataSourceLoaderRegistry.put(key, loader);
    }

    static DataSourceLoader getLoader(long dataTypeId, byte loaderVersion) {
        DataSourceLoader loader = dataSourceLoaderRegistry.get(new LoaderKey(dataTypeId, loaderVersion));
        return loader;
    }



    static LodDataSource loadData(String dataSourceTypeNameVersion, DHLevel level, DhSectionPos pos, InputStream data) {

        DataSourceLoader loader = dataSourceLoaderRegistry.get(dataSourceTypeNameVersion);
        if (loader == null) {
            throw new IllegalArgumentException("No loader for data source type " + dataSourceTypeNameVersion);
        }
        return loader.loadData(level, pos, data);
    }
    DataSourceLoader getLatestLoader();

    <T> T[] getData(); //TODO & FIXME: What is T?

    DhSectionPos getSectionPos();
    byte getDataDetail();
}
