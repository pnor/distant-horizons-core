package com.seibel.lod.core.a7.render;

import com.seibel.lod.core.a7.data.LodDataSource;
import com.seibel.lod.core.a7.pos.DhSectionPos;
import com.seibel.lod.core.a7.DHLevel;
import com.seibel.lod.core.objects.a7.data.DataFile;

import java.util.*;
import java.util.stream.Collectors;

public abstract class RenderDataSourceLoader {
    public final int detailOffset;
    public RenderDataSourceLoader(int detailOffset) {
        this.detailOffset = detailOffset;
    }

    public abstract RenderDataSource construct(List<LodDataSource> dataSources, DhSectionPos sectionPos, DHLevel level);

    public List<DataFile> selectFiles(DhSectionPos sectionPos, DHLevel level, List<DataFile>[] availableFiles) {
        return Arrays.stream(availableFiles).flatMap(Collection::stream).collect(Collectors.toList());
    }

}
