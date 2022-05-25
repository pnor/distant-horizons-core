package com.seibel.lod.core.objects.a7;

import com.seibel.lod.core.objects.a7.pos.DhSectionPos;
import com.seibel.lod.core.objects.a7.render.RenderDataSource;
import com.seibel.lod.core.objects.a7.render.RenderDataSourceLoader;

import java.util.concurrent.CompletableFuture;

public class LodSection {
    public static final int SUB_REGION_DATA_WIDTH = 16*16;

    public final DhSectionPos pos;

    /* Following used for LodQuadTree tick() method, and ONLY for that method! */
    // the number of children of this section
    // (Should always be 4 after tick() is done, or 0 only if this is an unloaded node)
    public byte childCount = 0;

    // TODO: Should I provide a way to change the render source?
    private RenderDataSource renderDataSource;
    private CompletableFuture<RenderDataSource> loadFuture;
    private boolean isRenderEnabled = false;

    // Create sub region
    public LodSection(DhSectionPos pos) {
        this.pos = pos;
    }

    public void enableRender(LodQuadTree quadTree) {
        if (isRenderEnabled) return;
        if (renderDataSource != null) {
            renderDataSource.enableRender(quadTree);
        }
        isRenderEnabled = true;
    }
    public void disableRender() {
        if (!isRenderEnabled) return;
        if (renderDataSource != null) {
            renderDataSource.disableRender();
        }
        isRenderEnabled = false;
    }

    public void load(RenderDataProvider renderDataProvider, RenderDataSourceLoader renderDataSourceClass) {
        if (loadFuture != null || renderDataSource != null) throw new IllegalStateException("Reloading is not supported!");
        loadFuture = renderDataProvider.createRenderData(renderDataSourceClass, pos);
    }

    public void tick(LodQuadTree quadTree) {
        if (loadFuture != null && loadFuture.isDone()) {
            renderDataSource = loadFuture.join();
            loadFuture = null;
            if (isRenderEnabled) {
                renderDataSource.enableRender(quadTree);
            }
        }
    }

    public void dispose() {
        if (renderDataSource != null) {
            renderDataSource.dispose();
        } else if (loadFuture != null) {
            loadFuture.cancel(true);
        }
    }

    public boolean canRender() {
        return isLoaded() && renderDataSource.isRenderReady();
    }

    public boolean isLoaded() {
        return renderDataSource != null;
    }

    public boolean isLoading() {
        return loadFuture != null;
    }

    public RenderDataSource getRenderContainer() {
        return renderDataSource;
    }

}
