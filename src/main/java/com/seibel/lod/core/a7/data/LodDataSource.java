package com.seibel.lod.core.a7.data;

import com.seibel.lod.core.a7.pos.DhSectionPos;

public interface LodDataSource {
    DataSourceLoader getLatestLoader();
    DhSectionPos getSectionPos();
    byte getDataDetail();
    void setLocalVersion(int localVer);
}
