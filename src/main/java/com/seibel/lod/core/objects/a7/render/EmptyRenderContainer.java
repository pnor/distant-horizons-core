package com.seibel.lod.core.objects.a7.render;

import com.seibel.lod.core.objects.a7.data.LodDataSource;
import com.seibel.lod.core.objects.a7.pos.DhSectionPos;
import com.seibel.lod.core.objects.opengl.RenderBuffer;

import java.util.concurrent.atomic.AtomicReference;

public class EmptyRenderContainer implements RenderDataSource {
    public static final EmptyRenderContainer INSTANCE = new EmptyRenderContainer();

    // NOTE: No register() needed since this should never be loaded from a actual data.

    @Override
    public void load() {

    }

    @Override
    public void unload() {

    }

    @Override
    public void dispose() {

    }

    @Override
    public boolean trySwapRenderBuffer(AtomicReference<RenderBuffer> referenceSlot) {
        return false; // no swap
    }
}
