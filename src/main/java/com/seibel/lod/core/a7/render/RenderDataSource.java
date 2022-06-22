package com.seibel.lod.core.a7.render;

import com.seibel.lod.core.a7.LodQuadTree;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Example on how to register a loader:
 * <pre>
     public static RenderDataSource testAndConstruct(LodDataSource dataSource, DhSectionPos sectionPos) {
        ColumnRenderContainer container = new ColumnRenderContainer(10, -100);
        container.startFillData(dataSource);
        return container;
     }
     static {
        RenderDataSource.registorLoader(ColumnRenderContainer::testAndConstruct, 0);
     }
 </pre>
 */
public interface RenderDataSource {
    // Don't think this is needed with the newer quad tree structure...
//    interface RenderContainerConstructor {
//        // Can return null as meaning the requirement is not met
//        RenderDataSource testAndConstruct(LodDataSource dataSource, DhSectionPos sectionPos);
//    }
//    SortedMap<Integer, RenderContainerConstructor>
//            renderContainerLoaderRegistry = new TreeMap<Integer, RenderContainerConstructor>();
//    static void registorLoader(RenderContainerConstructor func, int priority) {
//        if (func == null) {
//            throw new IllegalArgumentException("loader must be non-null");
//        }
//        renderContainerLoaderRegistry.put(priority, func);
//    }
//
//    static RenderDataSource tryConstruct(LodDataSource dataSource, DhSectionPos pos) {
//        for (RenderContainerConstructor func : renderContainerLoaderRegistry.values()) {
//            RenderDataSource container = func.testAndConstruct(dataSource, pos);
//            if (container != null) {
//                return container;
//            }
//        }
//        return null;
//    }

    void enableRender(LodQuadTree quadTree);
    void disableRender();
    boolean isRenderReady();
    void dispose(); // notify the container that the parent lodSection is now disposed (can be in loaded or unloaded state)

    byte getDetailOffset();

    /**
     * Try and swap in new render buffer for this section. Note that before this call, there should be no other
     *  places storing or referencing the render buffer.
     * @param referenceSlot The slot for swapping in the new buffer.
     * @return True if the swap was successful. False if swap is not needed or if it is in progress.
     */
    boolean trySwapRenderBuffer(LodQuadTree quadTree, AtomicReference<RenderBuffer> referenceSlot);

}
