package com.seibel.lod.core.objects.a7.render;

import com.seibel.lod.core.objects.a7.LodSection;
import com.seibel.lod.core.objects.a7.RenderDataContainer;
import com.seibel.lod.core.objects.a7.data.LodDataSource;
import com.seibel.lod.core.objects.a7.pos.DhSectionPos;
import com.seibel.lod.core.objects.opengl.RenderBuffer;
import com.seibel.lod.core.objects.opengl.RenderRegion;
import com.seibel.lod.core.util.LodUtil;

import java.util.concurrent.atomic.AtomicReference;

public class ColumnRenderContainer extends RenderContainer {
    public static final int columnWidth = DhSectionPos.DATA_WIDTH_PER_SECTION;
    public static final int columnCount = LodUtil.pow2(DhSectionPos.DATA_WIDTH_PER_SECTION);

    public RenderDataContainer dataContainer = null;

    public final int maxColumnHeight;
    public final int minWorldHeight;

    public static RenderContainer testAndConstruct(LodDataSource dataSource, DhSectionPos sectionPos) {
        ColumnRenderContainer container = new ColumnRenderContainer(10, -100); //FIXME: Use actual config value
        container.startFillData(dataSource);
        return container;
    }
    static {
        RenderContainer.registorLoader(ColumnRenderContainer::testAndConstruct, 0);
    }

    public ColumnRenderContainer(int maxColumnHeight, int minWorldHeight) {
        this.maxColumnHeight = maxColumnHeight;
        //columnData = new long[columnCount * maxColumnHeight];
        this.minWorldHeight = minWorldHeight;
    }

    private void startFillData(LodDataSource dataSource) {
        //TODO
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
