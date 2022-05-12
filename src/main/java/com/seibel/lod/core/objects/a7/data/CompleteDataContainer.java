package com.seibel.lod.core.objects.a7.data;

import com.seibel.lod.core.objects.a7.IdMappingUtil;
import com.seibel.lod.core.objects.a7.pos.DhSectionPos;
import com.seibel.lod.core.wrapperInterfaces.chunk.IChunkWrapper;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Function;

public class CompleteDataContainer implements LodDataSource { // 1 chunk
    private DhSectionPos sectionPos;
    ArrayList<String> idMap;

    protected CompleteDataContainer() {
        idMap = new ArrayList<String>();
    }

    @Override
    public DataSourceLoader getLatestLoader() {
        return (DhSectionPos sectionPos, InputStream inputStream) -> {
            //TODO: Implement
            return null;
        };
    }

    @Override
    public <T> T[] getData() {
        return null;
    }

    @Override
    public DhSectionPos getSectionPos() {
        return sectionPos;
    }

    public static CompleteDataContainer createNewFromChunk(IChunkWrapper chunk) {
        CompleteDataContainer dataContainer = new CompleteDataContainer();
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
