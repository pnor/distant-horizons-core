package com.seibel.lod.core.a7.datatype.full.accessor;

public class SingleFullArrayView implements IFullDataView {
    private final long[][] dataArrays;
    private final int offset;
    public SingleFullArrayView(long[][] dataArrays, int offset) {
        this.dataArrays = dataArrays;
        this.offset = offset;
    }

    public boolean doesItExist() {
        return dataArrays[offset].length!=0;
    }

    @Override
    public SingleFullArrayView get(int index) {
        if (index != 0) throw new IllegalArgumentException("Only contains 1 column of full data!");
        return this;
    }

    @Override
    public SingleFullArrayView get(int x, int z) {
        if (x != 0 || z != 0) throw new IllegalArgumentException("Only contains 1 column of full data!");
        return this;
    }

    public long getSingle(int yIndex) {
        return dataArrays[offset][yIndex];
    }
    public void setSingle(int yIndex, long value) {
        dataArrays[offset][yIndex] = value;
    }
    public void setNew(long[] newArray) {
        dataArrays[offset] = newArray;
    }

    @Override
    public int width() {
        return 1;
    }

    @Override
    public IFullDataView subView(int size, int ox, int oz) {
        if (size != 1 || ox != 1 || oz != 1)
            throw new IllegalArgumentException("Getting invalid range of subView from SingleFullArrayView!");
        return this;
    }

}
