package com.seibel.lod.core.a7.render;

import com.seibel.lod.core.a7.LodQuadTree;

import java.util.concurrent.atomic.AtomicReference;

public class EmptyRenderContainer implements RenderDataSource {
    public static final EmptyRenderContainer INSTANCE = new EmptyRenderContainer();

    // NOTE: No register() needed since this should never be loaded from a actual data.


    @Override
    public void enableRender(LodQuadTree quadTree) {

    }

    @Override
    public void disableRender() {

    }

    @Override
    public boolean isRenderReady() {
        return false;
    }

    @Override
    public void dispose() {

    }

    @Override
    public byte getDetailOffset() {
        return 0;
    }

    @Override
    public boolean trySwapRenderBuffer(LodQuadTree quadTree, AtomicReference<RenderBuffer> referenceSlot) {
        return false; // no swap
    }
}
