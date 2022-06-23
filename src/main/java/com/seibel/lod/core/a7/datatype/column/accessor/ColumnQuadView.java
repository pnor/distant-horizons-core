package com.seibel.lod.core.a7.datatype.column.accessor;

public class ColumnQuadView implements ColumnDataView {
    private final long[] data;
    private final int perRowOffset; // per row offset in longs
    private final int xSize; // x size in datapoints
    private final int zSize; // x size in datapoints
    private final int offset; // offset in longs
    private final int vertSize; // vertical size in longs

    public ColumnQuadView(long[] data, int dataZWidth, int dataVertSize, int viewXOffset, int viewZOffset, int xSize, int zSize) {
        if (viewXOffset + xSize > (data.length / (dataZWidth* dataVertSize)) || viewZOffset + zSize > dataZWidth)
            throw new IllegalArgumentException("View is out of bounds");
        this.data = data;
        this.xSize = xSize;
        this.zSize = zSize;
        this.vertSize = dataVertSize;
        this.perRowOffset = dataZWidth * dataVertSize;
        this.offset = (viewXOffset * perRowOffset + viewZOffset) * dataVertSize;
    }
    private ColumnQuadView(long[] data, int perRowOffset, int offset, int vertSize, int xSize, int zSize) {
        this.data = data;
        this.perRowOffset = perRowOffset;
        this.offset = offset;
        this.vertSize = vertSize;
        this.xSize = xSize;
        this.zSize = zSize;
    }

    @Override
    public long get(int index) {
        int x = index % (xSize * vertSize);
        int z = index / (xSize * vertSize);
        int v = index % vertSize;
        return get(x, z, v);
    }

    public long get(int x, int z, int v) {
        return data[offset + x * perRowOffset + z * vertSize + v];
    }

    public long set(int x, int z, int v, long value) {
        return data[offset + x * perRowOffset + z * vertSize + v] = value;
    }

    public ColumnArrayView get(int x, int z) {
        return new ColumnArrayView(data, vertSize, offset + x * perRowOffset + z * vertSize, vertSize);
    }

    public ColumnArrayView getRow(int x) {
        return new ColumnArrayView(data, vertSize, offset + x * perRowOffset, zSize * vertSize);
    }

    public void set(int x, int z, ColumnDataView singleColumn) {
        if (singleColumn.verticalSize() != vertSize) throw new IllegalArgumentException("Vertical size of singleColumn must be equal to vertSize");
        if (singleColumn.dataCount() != 1) throw new IllegalArgumentException("SingleColumn must contain exactly one data point");
        singleColumn.copyTo(data, x * perRowOffset + z * vertSize);
    }

    @Override
    public int size() {
        return xSize * zSize * vertSize;
    }

    @Override
    public int verticalSize() {
        return vertSize;
    }

    @Override
    public int dataCount() {
        return xSize * zSize;
    }

    @Override
    public ColumnDataView subView(int dataIndexStart, int dataCount) {
        if (dataCount != 1) throw new UnsupportedOperationException("Fixme: subView for QUadView only support one data point!");
        int x = dataIndexStart % xSize;
        int z = dataIndexStart / xSize;
        return new ColumnArrayView(data, vertSize, offset + x * perRowOffset + z * vertSize, vertSize);
    }

    public ColumnQuadView subView(int xOffset, int zOffset, int xSize, int zSize) {
        if (xOffset + xSize > this.xSize || zOffset + zSize > this.zSize) throw new IllegalArgumentException("SubView is out of bounds");
        return new ColumnQuadView(data, perRowOffset, offset + xOffset * perRowOffset + zOffset * vertSize, vertSize, xSize, zSize);
    }

    @Override
    public void copyTo(long[] target, int offset) {
        for (int x = 0; x < xSize; x++) {
            System.arraycopy(data, this.offset + x * perRowOffset, target, offset + x * xSize * vertSize, zSize * vertSize);
        }
    }

    public void copyTo(ColumnQuadView target) {
        if (target.xSize != xSize || target.zSize != zSize)
            throw new IllegalArgumentException("Target view must have same size as this view");

        for (int x = 0; x < xSize; x++) {
            target.getRow(x).changeVerticalSizeFrom(getRow(x));
        }
    }

    public void mergeMultiColumnFrom(ColumnQuadView source) {
        if (source.xSize == xSize && source.zSize == zSize)
        {
            source.copyTo(this);
            return;
        }
        if (source.xSize < xSize || source.zSize < zSize)
            throw new IllegalArgumentException("Source view must have same or larger size as this view");

        int srcXPerTrgX = source.xSize / xSize;
        int srcZPerTrgZ = source.zSize / zSize;
        if (source.xSize % xSize != 0 || source.zSize % zSize != 0)
            throw new IllegalArgumentException("Source view's size must be a multiple of this view's size");

        for (int x = 0; x < xSize; x++) {
            for (int z = 0; z < zSize; z++) {
                ColumnQuadView srcBlock = source.subView(x * srcXPerTrgX, z * srcZPerTrgZ, srcXPerTrgX, srcZPerTrgZ);
                get(x, z).mergeMultiDataFrom(srcBlock);
            }
        }
    }
}
