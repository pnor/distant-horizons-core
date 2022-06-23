package com.seibel.lod.core.a7.datatype.full;

import com.seibel.lod.core.a7.datatype.full.accessor.FullArrayView;

public class ChunkSizedData extends FullArrayView {
    public ChunkSizedData() {
        super(new IdBiomeBlockStateMap(), new long[16*16][0], 16);
    }

    public void setSingleColumn(long[] data, int x, int z) {
        dataArrays[x*16+z] = data;
    }
}