package com.seibel.lod.core.a7.datatype.full.accessor;

public class FullArrayView implements IFullDataView {
    private final long[][] dataArrays;
    private final int offset;
    private final int size;

    public FullArrayView(long[][] dataArrays, int size) {
        if (dataArrays.length != size*size)
            throw new IllegalArgumentException(
                    "tried constructing dataArrayView with invalid input!");
        this.dataArrays = dataArrays;
        this.size = size;
        offset = 0;
    }
    public FullArrayView(FullArrayView source, int size, int offsetX, int offsetZ) {
        if (source.size < size || source.size < size+offsetX || source.size < size+offsetZ)
            throw new IllegalArgumentException(
                    "tried constructing dataArrayView subview with invalid input!");
        dataArrays = source.dataArrays;
        this.size = size;
        offset = source.offset + offsetX * size + offsetZ;
    }

    @Override
    public SingleFullArrayView get(int index) {
        return new SingleFullArrayView(dataArrays, index + offset);
    }

    @Override
    public SingleFullArrayView get(int x, int z) {
        return new SingleFullArrayView(dataArrays, x*size + z + offset);
    }

    @Override
    public int width() {
        return size;
    }

    @Override
    public IFullDataView subView(int size, int ox, int oz) {
        return new FullArrayView(this, size, ox, oz);
    }

    public void copyTo(FullArrayView target) {
        if (target.size != size)
            throw new IllegalArgumentException("Target view must have same size as this view");
        for (int x=0; x<size; x++) {
            System.arraycopy(dataArrays, offset+x*size,
                    target.dataArrays, offset+x*size, size);
        }
    }
}
