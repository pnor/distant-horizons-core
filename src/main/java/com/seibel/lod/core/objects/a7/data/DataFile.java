package com.seibel.lod.core.objects.a7.data;

import com.seibel.lod.core.objects.a7.DHLevel;
import com.seibel.lod.core.objects.a7.datatype.column.DataSourceSaver;
import com.seibel.lod.core.objects.a7.datatype.column.OldDataSourceLoader;
import com.seibel.lod.core.objects.a7.pos.DhSectionPos;
import com.seibel.lod.core.util.LodUtil;

import java.io.*;
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
    public byte dataLevel;
    public DataSourceLoader loader;
    public byte loaderVersion;
    public Class<?> dataType;

    public LodDataSource loadedData = null;

    public static DataFile readMeta(File path) throws IOException {
        try (FileInputStream fin = new FileInputStream(path)) {
            MappedByteBuffer buffer = fin.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, METADATA_SIZE);
            return new DataFile(path, buffer);
        }
    }

    public DataFile(File path, DataSourceLoader loader, LodDataSource loadedData) {
        this.path = path;
        this.pos = loadedData.getSectionPos();
        this.loader = loader;
        this.dataType = loader.clazz;
        this.dataLevel = loadedData.getDataDetail();
        this.loadedData = loadedData;
        this.loaderVersion = loader.loaderSupportedVersions[loader.loaderSupportedVersions.length - 1]; // get latest version
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
        dataLevel = meta.get();
        byte loaderVersion = meta.get();
        byte unused = meta.get();
        long dataTypeId = meta.getLong();
        long unused2 = meta.getLong();
        LodUtil.assertTrue(meta.remaining() == 0);

        this.pos = new DhSectionPos(detailLevel, x, z);
        this.loader = DataSourceLoader.getLoader(dataTypeId, loaderVersion);
        if (loader == null) {
            throw new IOException("Invalid file: Data type loader not found: " + dataTypeId + "(v" + loaderVersion + ")");
        }
        this.dataType = loader.clazz;
        this.loaderVersion = loaderVersion;
    }
    public FileInputStream getDataContent() throws IOException {
        FileInputStream fin = new FileInputStream(path);
        fin.skipNBytes(METADATA_SIZE);
        return fin;
    }

    LodDataSource load(DHLevel level) {
        if (loadedData != null) return loadedData;
        try {
            loadedData = loader.loadData(this, level);
            return loadedData;
        } catch (IOException e) {
            //FIXME: Log and review this handling
            return null;
        }
    }

    public boolean verifyPath() {
        return path.exists() && path.isFile() && path.canRead() && path.canWrite();
    }

    public void save(DHLevel level) throws IOException {
        if (loadedData == null) throw new IllegalStateException("No data loaded");
        if (!verifyPath()) throw new IOException("File path became invalid");
        DataSourceSaver saver;
        if (loader instanceof DataSourceSaver) saver = (DataSourceSaver) loader;
        else if (loader instanceof OldDataSourceLoader) saver = ((OldDataSourceLoader) loader).getNewSaver();
        else saver = null;
        if (saver == null) return;

        byte newDataLevel = loadedData.getDataDetail();

        try (FileOutputStream fout = new FileOutputStream(path, false)) {
            try (DataOutputStream out = new DataOutputStream(fout)) {

                out.writeInt(METADATA_MAGIC_BYTES);

                // Write x, y, z, checksum
                out.writeInt(pos.sectionX);
                out.writeInt(Integer.MIN_VALUE); // not used for now
                out.writeInt(pos.sectionZ);
                out.writeInt(Integer.MIN_VALUE); // not used for now

                // Write detail level, data level, loader version
                out.writeByte(pos.sectionDetail);
                out.writeByte(loadedData.getDataDetail());
                out.writeByte(saver.getSaverVersion());

                // Write unused
                out.writeByte((byte) 0);

                // Write data type id
                out.writeLong(saver.datatypeId);

                // Write unused
                out.writeLong(Long.MIN_VALUE);
                // Write data
                saver.saveData(level, loadedData, out);
            }
        }

        dataLevel = newDataLevel;
        loader = saver;


    }
}
