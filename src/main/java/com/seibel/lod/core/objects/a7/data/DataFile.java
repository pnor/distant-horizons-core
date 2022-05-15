package com.seibel.lod.core.objects.a7.data;

import com.seibel.lod.core.objects.a7.DHLevel;
import com.seibel.lod.core.objects.a7.pos.DhSectionPos;
import com.seibel.lod.core.util.LodUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class DataFile {
    //Metadata format:
    //
    //    4 bytes: magic bytes: "DHv0" (in ascii: 0x44 48 76 30) (this also signal the metadata format)
    //    4 bytes: section X position
    //    4 bytes: section Y position (Unused, for future proofing)
    //    4 bytes: section Z position
    //
    //    4 bytes: data checksum //TODO: Implement checksum
    //    1 byte: section detail level
    //    1 byte: data detail level // Note: not sure if this is needed
    //    1 byte: loader version
    //    1 byte: unused
    //
    //    8 bytes: datatype identifier
    //
    //    8 bytes: unused

    // Total size: 32 bytes

    public static final int METADATA_SIZE = 32;
    public static final int METADATA_MAGIC_BYTES = 0x44_48_76_30;

    public final File path;
    public final DhSectionPos pos;
    public final LodDataSource.DataSourceLoader loader;
    public final Class<?> dataType;

    public LodDataSource loadedData = null;

    public static DataFile readMeta(File path) throws IOException {
        try (FileInputStream fin = new FileInputStream(path)) {
            MappedByteBuffer buffer = fin.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, METADATA_SIZE);
            return new DataFile(path, buffer);
        }
    }

    public DataFile(File path, DhSectionPos pos, LodDataSource.DataSourceLoader loader, Class<?> dataType) {
        this.path = path;
        this.pos = pos;
        this.loader = loader;
        this.dataType = dataType;
    }

    DataFile(File path, MappedByteBuffer meta) throws IOException {
        this.path = path;

        int magic = meta.getInt();
        if (magic != METADATA_MAGIC_BYTES) {
            throw new IOException("Invalid file: Magic bytes check failed.");
        }
        int x = meta.getInt();
        int y = meta.getInt(); // Unused
        int z = meta.getInt();
        int checksum = meta.getInt();
        byte detailLevel = meta.get();
        byte dataDetailLevel = meta.get();
        byte loaderVersion = meta.get();
        byte unused = meta.get();
        long dataTypeId = meta.getLong();
        long unused2 = meta.getLong();
        LodUtil.assertTrue(meta.remaining() == 0);

        this.pos = new DhSectionPos(detailLevel, x, z);
        this.loader = LodDataSource.getLoader(dataTypeId, loaderVersion);
        if (loader == null) {
            throw new IOException("Invalid file: Data type loader not found: " + dataTypeId + "(v" + loaderVersion + ")");
        }
        this.dataType = LodDataSource.dataSourceTypeRegistry.get(dataTypeId);
    }

    LodDataSource load(DHLevel level) throws IOException {
        if (loadedData != null) return loadedData;
        FileInputStream fin = new FileInputStream(path);
        fin.skipNBytes(METADATA_SIZE);
        loadedData = loader.loadData(level, pos, fin);
        return loadedData;
    }
}
