package com.seibel.lod.core.objects.a7.data;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.function.Function;

public abstract class LodDataSource {
    private static final String REGISTER_STRING_FILTER_REGEX = "^[a-zA-Z0-9_]*$";
    public static final HashMap<String, Function<ByteBuffer,? extends LodDataSource>>
            dataSourceLoaderRegistry = new HashMap<String, Function<ByteBuffer,? extends LodDataSource>>();

    public static void registerDataSourceLoader(String name, int version, Function<ByteBuffer,? extends LodDataSource> loader) {
        if (name == null || loader == null || name.isEmpty()) {
            throw new IllegalArgumentException("Name and loader must be non-null, and not empty");
        }
        if (!name.matches(REGISTER_STRING_FILTER_REGEX)) {
            throw new IllegalArgumentException("Name must pass the regex " + REGISTER_STRING_FILTER_REGEX);
        }
        if (dataSourceLoaderRegistry.containsKey(name)) {
            throw new IllegalArgumentException("Data source loader already registered for " + name);
        }
        dataSourceLoaderRegistry.put(name, loader);
    }

    public static LodDataSource loadData(String dataSourceTypeName, ByteBuffer data) {

        Function<ByteBuffer,? extends LodDataSource> loader = dataSourceLoaderRegistry.get(dataSourceTypeName);
        if (loader == null) {
            throw new IllegalArgumentException("No loader for data source type " + dataSourceTypeName);
        }
        return loader.apply(data);
    }
    public abstract Function<ByteBuffer,? extends LodDataSource> getLatestLoader();

    public abstract <T> T[] getData(); //TODO & FIXME: What is T?
}
