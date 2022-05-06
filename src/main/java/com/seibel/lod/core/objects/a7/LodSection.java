package com.seibel.lod.core.objects.a7;

import com.seibel.lod.core.objects.lod.VerticalLevelContainer;

public class LodSection {
    public static final int SUB_REGION_DATA_WIDTH = 16*16;

    public final byte detailLevel;
    public final int x;
    public final int z;
    private RenderDataContaioner levelContainer;
    private RenderContainer renderContainer = null;

    // Create sub region
    public LodSection(byte detailLevel, int x, int z) {
        this.detailLevel = detailLevel;
        this.x = x;
        this.z = z;
        levelContainer = null;
    }
    LodSection(byte detailLevel, int x, int z, RenderDataContaioner levelContainer) {
        this.detailLevel = detailLevel;
        this.x = x;
        this.z = z;
        this.levelContainer = levelContainer;
    }

    // Return null if data does not exist
    public static LodSection loadSection(byte detailLevel, int x, int z, LodDataSource lodDataSource) {
        RenderDataContaioner data = lodDataSource.createRenderData(detailLevel, x, z);
        if (data == null) {
            return null;
        }
        return new LodSection(detailLevel, x, z, data);
    }


}
