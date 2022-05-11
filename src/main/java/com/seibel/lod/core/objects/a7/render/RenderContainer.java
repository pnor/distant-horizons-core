package com.seibel.lod.core.objects.a7.render;

import com.seibel.lod.core.objects.opengl.RenderBuffer;
import com.seibel.lod.core.objects.opengl.RenderRegion;

import java.util.concurrent.atomic.AtomicReference;

public abstract class RenderContainer {

    private boolean isLoaded = false;
    public final void load() {
        isLoaded = true;
        notifyLoad();
    }
    public final void unload() {
        isLoaded = false;
        notifyUnload();
    }
    public final void dispose() {
        if (isLoaded) {
            unload();
        }
        notifyDispose();
    }

    public final boolean isLoaded() {
        return isLoaded;
    }

    protected abstract void notifyLoad(); // notify the container that it is now loaded and therefore may be rendered
    protected abstract void notifyUnload(); // notify the container that it is now unloaded and therefore will not be rendered
    protected abstract void notifyDispose(); // notify the container that the parent lodSection is now disposed

    /**
     * Try and swap in new render buffer for this section. Note that before this call, there should be no other
     *  places storing or referencing the render buffer.
     * @param referenceSlot The slot for swapping in the new buffer.
     * @return True if the swap was successful. False if swap is not needed or if it is in progress.
     */
    public abstract boolean trySwapRenderBuffer(AtomicReference<RenderBuffer> referenceSlot);
}
