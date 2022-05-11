package com.seibel.lod.core.objects.a7;

import com.seibel.lod.core.objects.a7.pos.DhSectionPos;
import com.seibel.lod.core.objects.a7.render.RenderContainer;
import com.seibel.lod.core.util.LodUtil;

public class LodSection {
    public static final int SUB_REGION_DATA_WIDTH = 16*16;

    public final DhSectionPos pos;

    /* Following used for LodQuadTree tick() method, and ONLY for that method! */
    // the number of children of this section
    // (Should always be 4 after tick() is done, or 0 only if this is an unloaded node)
    public byte childCount = 0;

    // TODO: Should I provide a way to change the render source?
    private RenderContainer renderContainer;

    // Create sub region
    public LodSection(DhSectionPos pos, RenderDataSource renderSource) {
        this.pos = pos;
        this.renderContainer = renderSource.createRenderData(pos);
    }

    public void load() {
        LodUtil.assertTrue(!isLoaded());
        renderContainer.load();
    }
    public void unload() {
        LodUtil.assertTrue(isLoaded());
        renderContainer.unload();
    }

    public void dispose() {
        if (renderContainer != null) renderContainer.dispose();
    }

    public boolean isLoaded() {
        return renderContainer != null && renderContainer.isLoaded();
    }

    public RenderContainer getRenderContainer() {
        return renderContainer;
    }

}
