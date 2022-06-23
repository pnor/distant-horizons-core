package com.seibel.lod.core.a7.datatype.full;

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

public class FullDataSource implements LodDataSource { // 1 chunk
    private DhSectionPos sectionPos;
    ArrayList<String> idMap;
    protected FullDataSource() {
        idMap = new ArrayList<String>();
    }

    @Override
    public DhSectionPos getSectionPos() {
        return sectionPos;
    }

    @Override
    public byte getDataDetail() {
        return 0;
    }

    @Override
    public void setLocalVersion(int localVer) {

    }

    @Override
    public byte getDataVersion() {
        return 0;
    }

    @Override
    public void saveData(ILevel level, DataMetaFile file, OutputStream dataStream) throws IOException {

    }

    public static FullDataSource createNewFromChunk(IChunkWrapper chunk) {
        FullDataSource dataContainer = new FullDataSource();
        HashMap<String, Integer> idMap = new HashMap<String, Integer>();
        
        idMap.put(IdMappingUtil.BLOCKSTATE_ID_AIR, 0);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int y = chunk.getMaxY(x, z);
                String currentBlockState = IdMappingUtil.BLOCKSTATE_ID_AIR;
                // FIXME: Move LodBuilder code to here
            }
        }
        return dataContainer;
    }
}
