package com.seibel.lod.core.a7.save.io;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.function.BiConsumer;
import java.util.zip.Adler32;
import java.util.zip.CheckedOutputStream;

import com.seibel.lod.core.a7.data.DataSourceLoader;
import com.seibel.lod.core.a7.pos.DhSectionPos;
import com.seibel.lod.core.util.LodUtil;

public class MetaFile {
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
    //    8 bytes: timestamp

    // Total size: 32 bytes

    public static final int METADATA_SIZE = 32;
    public static final int METADATA_MAGIC_BYTES = 0x44_48_76_30;

    public final DhSectionPos pos;

    public File path;
    public int checksum;
    public long timestamp;
    public byte dataLevel;

    //Loader stuff
    public DataSourceLoader loader;
    public Class<?> dataType;
    public byte loaderVersion;

    // Load a metaFile in this path. It also automatically read the metadata.
    protected MetaFile(File path) throws IOException {
        validatePath();
        try (FileInputStream fin = new FileInputStream(path)) {
            MappedByteBuffer buffer = fin.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, METADATA_SIZE);
            this.path = path;

            int magic = buffer.getInt();
            if (magic != METADATA_MAGIC_BYTES) {
                throw new IOException("Invalid file: Magic bytes check failed.");
            }
            int x = buffer.getInt();
            int y = buffer.getInt(); // Unused
            int z = buffer.getInt();
            int checksum = buffer.getInt();
            byte detailLevel = buffer.get();
            dataLevel = buffer.get();
            byte loaderVersion = buffer.get();
            byte unused = buffer.get();
            long dataTypeId = buffer.getLong();
            long timestamp = buffer.getLong();
            LodUtil.assertTrue(buffer.remaining() == 0);

            this.pos = new DhSectionPos(detailLevel, x, z);
            this.loader = DataSourceLoader.getLoader(dataTypeId, loaderVersion);
            if (loader == null) {
                throw new IOException("Invalid file: Data type loader not found: " + dataTypeId + "(v" + loaderVersion + ")");
            }
            this.dataType = loader.clazz;
            this.loaderVersion = loaderVersion;
        }
    }

    // Make a new MetaFile. It doesn't load or write any metadata itself.
    protected MetaFile(File path, DhSectionPos pos) {
        this.path = path;
        this.pos = pos;
    }

    protected void save() {} //TODO: Implement

    private void validatePath() throws IOException {
        if (!path.exists()) throw new IOException("File missing");
        if (!path.isFile()) throw new IOException("Not a file");
        if (!path.canRead()) throw new IOException("File not readable");
        if (!path.canWrite()) throw new IOException("File not writable");
    }

    protected void updateMetaData() throws IOException {
        validatePath();
        try (FileChannel channel = FileChannel.open(path.toPath(), StandardOpenOption.READ)) {
            MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, METADATA_SIZE);
            int magic = buffer.getInt();
            if (magic != METADATA_MAGIC_BYTES) {
                throw new IOException("Invalid file: Magic bytes check failed.");
            }
            int x = buffer.getInt();
            int y = buffer.getInt(); // Unused
            int z = buffer.getInt();
            int checksum = buffer.getInt();
            byte detailLevel = buffer.get();
            dataLevel = buffer.get();
            byte loaderVersion = buffer.get();
            byte unused = buffer.get();
            long dataTypeId = buffer.getLong();
            long timestamp = buffer.getLong();
            LodUtil.assertTrue(buffer.remaining() == 0);

            DhSectionPos newPos = new DhSectionPos(detailLevel, x, z);
            if (!newPos.equals(pos)) {
                throw new IOException("Invalid file: Section position changed.");
            }
            this.loader = DataSourceLoader.getLoader(dataTypeId, loaderVersion);
            if (loader == null) {
                throw new IOException("Invalid file: Data type loader not found: " + dataTypeId + "(v" + loaderVersion + ")");
            }
            this.dataType = loader.clazz;
            this.loaderVersion = loaderVersion;
        }
    }

    protected void writeData(BiConsumer<MetaFile, OutputStream> dataWriter) throws IOException {
        validatePath();
        File tempFile = File.createTempFile("", "tmp", path.getParentFile());
        tempFile.deleteOnExit();
        try (FileChannel file = FileChannel.open(tempFile.toPath(),
                StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            {
                file.position(METADATA_SIZE);
                int checksum;
                try (OutputStream channelOut = Channels.newOutputStream(file);
                     BufferedOutputStream bufferedOut = new BufferedOutputStream(channelOut); // TODO: Is default buffer size ok? Do we even need to buffer?
                     CheckedOutputStream checkedOut = new CheckedOutputStream(bufferedOut, new Adler32())) { // TODO: Is Adler32 ok?
                    dataWriter.accept(this, checkedOut);
                    checksum = (int) checkedOut.getChecksum().getValue();
                    timestamp = System.currentTimeMillis(); // TODO: Do we need to use server synced time?
                                                            // Warn: This may become an attack vector! Be careful!
                }
                file.position(0);
                // Write metadata
                ByteBuffer buff = ByteBuffer.allocate(METADATA_SIZE);
                buff.putInt(METADATA_MAGIC_BYTES);
                buff.putInt(pos.sectionX);
                buff.putInt(Integer.MIN_VALUE); // Unused
                buff.putInt(pos.sectionZ);
                buff.putInt(checksum);
                buff.put(pos.sectionDetail);
                buff.put(dataLevel);
                buff.put(loaderVersion);
                buff.put(Byte.MIN_VALUE); // Unused
                buff.putLong(loader.datatypeId);
                buff.putLong(timestamp);
                LodUtil.assertTrue(buff.remaining() == 0);
                buff.flip();
                file.write(buff);
            }
            file.close();
            // Atomic move / replace the actual file
            Files.move(tempFile.toPath(), path.toPath(), StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.COPY_ATTRIBUTES);
        } finally {
            try {
                boolean i = tempFile.delete(); // Delete temp file. Ignore errors if fails.
            } catch (Exception ignored) {}
        }
    }
}
