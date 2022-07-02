package com.seibel.lod.core.a7;

import com.seibel.lod.core.a7.datatype.column.ColumnRenderLoader;

public class Initializer {
    public static void init() {
        ColumnRenderLoader unused = new ColumnRenderLoader(); // Auto register into the loader system
    }
}
