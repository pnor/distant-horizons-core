package com.seibel.lod.core.objects.a7;

import com.seibel.lod.core.objects.lod.LevelContainer;

public interface LodDataSource {
    LevelContainer readLodData(byte detailLevel, int x, int z);
    boolean canSave();
    boolean saveLodData(LevelContainer levelContainer, byte detailLevel, int x, int z);
}
