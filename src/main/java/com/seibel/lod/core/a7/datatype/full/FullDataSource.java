package com.seibel.lod.core.a7.datatype.full;

import com.seibel.lod.core.a7.datatype.column.ColumnRenderSource;
import com.seibel.lod.core.a7.datatype.full.accessor.FullArrayView;
import com.seibel.lod.core.a7.level.ILevel;
import com.seibel.lod.core.a7.save.io.file.DataMetaFile;
import com.seibel.lod.core.a7.datatype.LodDataSource;
import com.seibel.lod.core.a7.util.IdMappingUtil;
import com.seibel.lod.core.a7.pos.DhSectionPos;
import com.seibel.lod.core.wrapperInterfaces.chunk.IChunkWrapper;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;

public class FullDataSource extends FullArrayView implements LodDataSource { // 1 chunk
    public static final byte SECTION_SIZE_OFFSET = ColumnRenderSource.SECTION_SIZE_OFFSET;
    public static final int SECTION_SIZE = 1 << SECTION_SIZE_OFFSET;
    public static final byte LATEST_VERSION = 0;
    private final DhSectionPos sectionPos;
    private int localVersion = 0;
    protected FullDataSource(DhSectionPos sectionPos) {
        super(new IdBiomeBlockStateMap(), new long[SECTION_SIZE*SECTION_SIZE][0], SECTION_SIZE);
        this.sectionPos = sectionPos;
    }

    @Override
    public DhSectionPos getSectionPos() {
        return sectionPos;
    }

    @Override
    public byte getDataDetail() {
        return (byte) (sectionPos.sectionDetail-SECTION_SIZE_OFFSET);
    }

    @Override
    public void setLocalVersion(int localVer) {
        localVersion = localVer;
    }

    @Override
    public byte getDataVersion() {
        return LATEST_VERSION;
    }

    @Override
    public void saveData(ILevel level, DataMetaFile file, OutputStream dataStream) throws IOException {
        //TODO
    }
}
