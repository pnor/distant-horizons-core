package com.seibel.lod.core.objects.a7.data;

import com.google.common.collect.HashMultimap;
import com.seibel.lod.core.objects.a7.DHLevel;
import com.seibel.lod.core.objects.a7.pos.DhSectionPos;

import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.*;

public abstract class DataSourceLoader {

    public static final HashMultimap<Long, DataSourceLoader> loaderRegistry = HashMultimap.create();
    public static final HashMap<Long, Class<? extends LodDataSource>> datatypeIdRegistry = new HashMap<>();

    public final long datatypeId;
    public final byte loaderVersion;
    public final Class<? extends LodDataSource> clazz;

    public DataSourceLoader(Class<? extends LodDataSource> clazz, long datatypeId, byte loaderVersion) {
        this.datatypeId = datatypeId;
        this.loaderVersion = loaderVersion;
        this.clazz = clazz;

        if (datatypeIdRegistry.containsKey(datatypeId) && datatypeIdRegistry.get(datatypeId) != clazz) {
            throw new IllegalArgumentException("Loader for datatypeId " + datatypeId + " already registered with different class: "
                    + datatypeIdRegistry.get(datatypeId) + " != " + clazz);
        }
        Set<DataSourceLoader> loaders = loaderRegistry.get(datatypeId);
        if (loaders.stream().anyMatch(l -> l.loaderVersion == loaderVersion)) {
            throw new IllegalArgumentException("Loader for class " + clazz + " with version " + loaderVersion " already registered!");
        }
        datatypeIdRegistry.put(datatypeId, clazz);
        loaderRegistry.put(datatypeId, this);
    }

    // Can return null as meaning the requirement is not met
    public abstract LodDataSource loadData(DHLevel level, DhSectionPos sectionPos, InputStream data);

    public List<File> foldersToScan(File levelFolderPath) {
        return Collections.emptyList();
    }

    public static DataSourceLoader getLoader(long dataTypeId, byte loaderVersion) {
        return loaderRegistry.get(dataTypeId).stream()
                .filter(l -> l.loaderVersion == loaderVersion)
                .findFirst().orElse(null);
    }

}
