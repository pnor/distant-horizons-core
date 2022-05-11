package com.seibel.lod.core.objects.a7.render;

import com.seibel.lod.core.objects.a7.pos.DhSectionPos;
import com.seibel.lod.core.objects.opengl.RenderBuffer;
import com.seibel.lod.core.objects.opengl.RenderRegion;
import com.seibel.lod.core.util.LodUtil;

import java.util.concurrent.atomic.AtomicReference;

public class ColumnRenderContainer extends RenderContainer {
    public static final int columnWidth = DhSectionPos.DATA_WIDTH_PER_SECTION;
    public static final int columnCount = LodUtil.pow2(DhSectionPos.DATA_WIDTH_PER_SECTION);
    private long[] columnData;
    public final int maxColumnHeight;
    public final int minWorldHeight;

    public RenderRegion renderRegion = null;

    public ColumnRenderContainer(int maxColumnHeight, int minWorldHeight) {
        this.maxColumnHeight = maxColumnHeight;
        columnData = new long[columnCount * maxColumnHeight];
        this.minWorldHeight = minWorldHeight;
        renderRegion = new RenderRegion();
    }

    @Override
    public void notifyRenderable() {

    }

    @Override
    public void notifyUnrenderable() {

    }

    @Override
    public boolean isRenderable() {
        return false;
    }

    @Override
    public void notifyLoad() {

    }

    @Override
    public void notifyUnload() {

    }

    @Override
    public void notifyDispose() {

    }

    @Override
    public boolean trySwapRenderBuffer(AtomicReference<RenderBuffer> referenceSlot) {
        return false;
    }
}
