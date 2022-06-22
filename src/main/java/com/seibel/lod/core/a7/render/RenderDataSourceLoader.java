package com.seibel.lod.core.a7.render;

import com.seibel.lod.core.a7.data.LodDataSource;
import com.seibel.lod.core.a7.pos.DhSectionPos;
import com.seibel.lod.core.a7.level.DhClientServerLevel;
import com.seibel.lod.core.a7.data.DataFile;

import java.util.*;
import java.util.stream.Collectors;

public abstract class RenderDataSourceLoader {
    public final int detailOffset;
    public RenderDataSourceLoader(int detailOffset) {
        this.detailOffset = detailOffset;
    }

    public abstract RenderDataSource construct(List<LodDataSource> dataSources, DhSectionPos sectionPos, DhClientServerLevel level);

    public List<DataFile> selectFiles(DhSectionPos sectionPos, DhClientServerLevel level, List<DataFile>[] availableFiles) {
        return Arrays.stream(availableFiles).flatMap(Collection::stream).collect(Collectors.toList());
    }

}
