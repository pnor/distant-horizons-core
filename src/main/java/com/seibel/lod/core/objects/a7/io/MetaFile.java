package com.seibel.lod.core.objects.a7.io;

import java.io.*;

import com.seibel.lod.core.objects.a7.pos.DhSectionPos;

public abstract class MetaFile {
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

    protected final File path;
    protected final DhSectionPos pos;
    public byte dataLevel;
    public byte loaderVersion;
    
    public MetaFile(File path, DhSectionPos pos)
    {
    	this.path = path;
    	this.pos = pos;
    }
    
}
