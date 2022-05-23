package com.seibel.lod.core.objects.a7.data;

import com.seibel.lod.core.objects.a7.pos.DhSectionPos;

import java.util.HashMap;
import java.util.Objects;

public interface LodDataSource {
    DataSourceLoader getLatestLoader();
    DhSectionPos getSectionPos();
    byte getDataDetail();
}
