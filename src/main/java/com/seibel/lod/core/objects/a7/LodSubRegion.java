package com.seibel.lod.core.objects.a7;

import com.seibel.lod.core.objects.lod.LevelContainer;
import com.seibel.lod.core.objects.lod.VerticalLevelContainer;

public class LodSubRegion {
    public static final int SUB_REGION_DATA_WIDTH = 16*16;

    public final byte detailLevel;
    public final int x;
    public final int z;
    private LevelContainer levelContainer;
    private RenderContainer renderContainer = null;

    // Create sub region
    public LodSubRegion(byte detailLevel, int x, int z) {
        this.detailLevel = detailLevel;
        this.x = x;
        this.z = z;
        levelContainer = new VerticalLevelContainer(detailLevel);
    }
    LodSubRegion(byte detailLevel, int x, int z, LevelContainer levelContainer) {
        this.detailLevel = detailLevel;
        this.x = x;
        this.z = z;
        this.levelContainer = levelContainer;
    }

    // Return null if data does not exist
    public static LodSubRegion loadSubRegion(byte detailLevel, int x, int z, LodDataSource lodDataSource) {
        LevelContainer data = lodDataSource.readLodData(detailLevel, x, z);
        if (data == null) {
            return null;
        }
        return new LodSubRegion(detailLevel, x, z, data);
    }
}
