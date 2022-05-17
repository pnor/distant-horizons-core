package com.seibel.lod.core.objects.a7.datatype.full;

import com.seibel.lod.core.objects.a7.data.LodDataSource;
import com.seibel.lod.core.objects.a7.pos.DhSectionPos;

public class FullDatatype implements LodDataSource {
    @Override
    public DataSourceLoader getLatestLoader() {
        return null;
    }

    @Override
    public <T> T[] getData() {
        return null;
    }

    @Override
    public DhSectionPos getSectionPos() {
        return null;
    }

    @Override
    public byte getDataDetail() {
        return 0;
    }
}
