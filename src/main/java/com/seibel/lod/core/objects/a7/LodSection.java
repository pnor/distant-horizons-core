package com.seibel.lod.core.objects.a7;

import com.seibel.lod.core.objects.a7.pos.DhSectionPos;

public class LodSection {
    public static final int SUB_REGION_DATA_WIDTH = 16*16;

    public final DhSectionPos pos;

    /* Following used for LodQuadTree tick() method, and ONLY for that method! */
    // the number of children of this section
    // (Should always be 4 after tick() is done, or 0 only if this is an unloaded node)
    public byte childCount = 0;


    private RenderDataContainer levelContainer;
    private RenderContainer renderContainer = null;

    // Create sub region
    public LodSection(DhSectionPos pos) {
        this.pos = pos;
        levelContainer = null;
    }
    LodSection(DhSectionPos pos, RenderDataContainer levelContainer) {
        this.pos = pos;
        this.levelContainer = levelContainer;
    }

    // Return null if data does not exist
    public boolean load(RenderDataSource renderDataSource) {
        if (isLoaded()) throw new IllegalStateException("LodSection is already loaded");
        levelContainer = renderDataSource.createRenderData(pos);
        return levelContainer != null;
    }
    public void unload() {
        if (!isLoaded()) throw new IllegalStateException("LodSection is not loaded");
        levelContainer = null;
    }

    public boolean isLoaded() {
        return levelContainer != null;
    }
}
