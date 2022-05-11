package com.seibel.lod.core.objects.a7.render;

import com.seibel.lod.core.objects.a7.LodSection;
import com.seibel.lod.core.objects.a7.data.LodDataSource;
import com.seibel.lod.core.objects.a7.pos.DhSectionPos;
import com.seibel.lod.core.objects.opengl.RenderBuffer;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;

public abstract class RenderContainer {
    interface RenderContainerConstructor {
        // Can return null as meaning the requirement is not met
        RenderContainer testAndConstruct(LodDataSource dataSource, DhSectionPos sectionPos);
    }
    public static final SortedMap<Integer, RenderContainerConstructor>
            renderContainerLoaderRegistry = new TreeMap<Integer, RenderContainerConstructor>();
    public static void registorLoader(RenderContainerConstructor func, int priority) {
        if (func == null) {
            throw new IllegalArgumentException("loader must be non-null");
        }
        renderContainerLoaderRegistry.put(priority, func);
    }

    public static RenderContainer tryConstruct(LodDataSource dataSource, DhSectionPos pos) {
        for (RenderContainerConstructor func : renderContainerLoaderRegistry.values()) {
            RenderContainer container = func.testAndConstruct(dataSource, pos);
            if (container != null) {
                return container;
            }
        }
        return null;
    }

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
