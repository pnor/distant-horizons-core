package com.seibel.lod.core.a7.datatype.full;

import com.seibel.lod.core.a7.datatype.full.accessor.FullArrayView;

public class ChunkSizedData extends FullArrayView {
    private final long[][] chunkDataArray = new long[16*16][];

    public ChunkSizedData(long[][] dataArrays) {
        super(dataArrays, 16);
    }
}