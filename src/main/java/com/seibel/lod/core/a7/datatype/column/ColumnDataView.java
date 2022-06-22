package com.seibel.lod.core.a7.datatype.column;

import java.util.Iterator;

public interface ColumnDataView {
    long get(int index);

    int size();

    default Iterator<Long> iterator() {
        return new Iterator<Long>() {
            private int index = 0;
            private final int size = size();
            @Override
            public boolean hasNext() {
                return index < size;
            }

            @Override
            public Long next() {
                return get(index++);
            }
        };
    }

    int verticalSize();

    int dataCount();

    ColumnDataView subView(int dataIndexStart, int dataCount);

    void copyTo(long[] target, int offset);
}
