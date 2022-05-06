package com.seibel.lod.core.objects.a7;

public interface LodDataSource {
    RenderDataContaioner createRenderData(byte detailLevel, int x, int z);


    boolean saveLodData(RenderDataContaioner levelContainer, byte detailLevel, int x, int z);
}
