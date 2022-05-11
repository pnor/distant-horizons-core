package com.seibel.lod.core.objects.a7.render;

import com.seibel.lod.core.objects.opengl.RenderBuffer;

import java.util.concurrent.atomic.AtomicReference;

public class EmptyRenderContainer extends RenderContainer {
    public static final EmptyRenderContainer INSTANCE = new EmptyRenderContainer();

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
        return false; // no swap
    }
}
