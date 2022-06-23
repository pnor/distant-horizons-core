package com.seibel.lod.core.a7;

import com.seibel.lod.core.a7.datatype.column.ColumnRenderLoader;
import com.seibel.lod.core.a7.render.LodQuadTree;

public class Initializer {
    public static void init() {
        ColumnRenderLoader columnRenderLoader = new ColumnRenderLoader();
        LodQuadTree.registerLayerLoader(columnRenderLoader, (byte) 7); // 7 or above


    }
}
