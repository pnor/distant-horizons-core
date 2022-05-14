package com.seibel.lod.core.objects.a7;

import com.seibel.lod.core.objects.a7.pos.DhSectionPos;
import com.seibel.lod.core.objects.a7.render.RenderDataSource;
import com.seibel.lod.core.util.LodUtil;

public class LodSection {
    public static final int SUB_REGION_DATA_WIDTH = 16*16;

    public final DhSectionPos pos;

    /* Following used for LodQuadTree tick() method, and ONLY for that method! */
    // the number of children of this section
    // (Should always be 4 after tick() is done, or 0 only if this is an unloaded node)
    public byte childCount = 0;

    // TODO: Should I provide a way to change the render source?
    private RenderDataSource renderDataSource;
    private boolean isLoaded = false;

    // Create sub region
    public LodSection(DhSectionPos pos, RenderDataProvider renderDataProvider, Class<? extends RenderDataSource> renderDataSourceClass) {
        this.pos = pos;
        this.renderDataSource = renderDataSourceClass == null ?
                null : renderDataProvider.createRenderData(pos);
    }

    public void load() {
        if (renderDataSource != null) {
            LodUtil.assertTrue(!isLoaded());
            renderDataSource.load();
            isLoaded = true;
        }
    }
    public void unload() {
        if (renderDataSource != null) {
            LodUtil.assertTrue(isLoaded());
            renderDataSource.unload();
            isLoaded = false;
        }
    }

    public void dispose() {
        if (renderDataSource != null) {
            if (isLoaded()) renderDataSource.unload();
            renderDataSource.dispose();
        }
    }

    public boolean isLoaded() {
        return renderDataSource != null && isLoaded;
    }

    public RenderDataSource getRenderContainer() {
        return renderDataSource;
    }

}
