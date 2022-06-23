package com.seibel.lod.core.a7.data;

import com.google.common.collect.HashMultimap;
import com.seibel.lod.core.a7.save.io.file.DataMetaFile;
import com.seibel.lod.core.a7.level.ILevel;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public abstract class DataSourceLoader {

    public static final HashMultimap<Long, DataSourceLoader> loaderRegistry = HashMultimap.create();
    public static final HashMap<Long, Class<? extends LodDataSource>> datatypeIdRegistry = new HashMap<>();

    public final long datatypeId;
    public final byte[] loaderSupportedVersions;
    public final Class<? extends LodDataSource> clazz;

    public DataSourceLoader(Class<? extends LodDataSource> clazz, long datatypeId, byte[] loaderSupportedVersions) {
        this.datatypeId = datatypeId;
        this.loaderSupportedVersions = loaderSupportedVersions;
        Arrays.sort(loaderSupportedVersions); // sort to allow fast access
        this.clazz = clazz;

        if (datatypeIdRegistry.containsKey(datatypeId) && datatypeIdRegistry.get(datatypeId) != clazz) {
            throw new IllegalArgumentException("Loader for datatypeId " + datatypeId + " already registered with different class: "
                    + datatypeIdRegistry.get(datatypeId) + " != " + clazz);
        }
        Set<DataSourceLoader> loaders = loaderRegistry.get(datatypeId);
        if (loaders.stream().anyMatch(other -> {
            // see if any loaderSupportsVersion conflicts with this one
            for (byte otherVer : other.loaderSupportedVersions) {
                if (Arrays.binarySearch(loaderSupportedVersions, otherVer) >= 0) return true;
            }
            return false;
        })) {
            throw new IllegalArgumentException("Loader for class " + clazz + " that supports one of the version in "
                    + Arrays.toString(loaderSupportedVersions) + " already registered!");
        }
        datatypeIdRegistry.put(datatypeId, clazz);
        loaderRegistry.put(datatypeId, this);
    }

    // Can return null as meaning the requirement is not met
    public abstract LodDataSource loadData(DataMetaFile dataFile, InputStream data, ILevel level) throws IOException;

    public List<File> foldersToScan(File levelFolderPath) {
        return Collections.emptyList();
    }

    public static DataSourceLoader getLoader(long dataTypeId, byte loaderVersion) {
        return loaderRegistry.get(dataTypeId).stream()
                .filter(l -> Arrays.binarySearch(l.loaderSupportedVersions, loaderVersion) >= 0)
                .findFirst().orElse(null);
    }

}
