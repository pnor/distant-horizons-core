package com.seibel.lod.core.objects.a7.datatype.column;

import com.seibel.lod.core.objects.LodDataView;
import com.seibel.lod.core.objects.a7.data.LodDataSource;

public interface IColumnDatatype extends LodDataSource {
    byte getDetailOffset();
    default int getDataSize() {
        return 1 << getDetailOffset();
    }
    int getMaxNumberOfLods();
    long getRoughRamUsage();

    int getVerticalSize();
    boolean doesItExist(int posX, int posZ);
    long getData(int posX, int posZ, int verticalIndex);
    default long getSingleData(int posX, int posZ) {return getData(posX, posZ, 0);}
    long[] getAllData(int posX, int posZ);
    ColumnArrayView getVerticalDataView(int posX, int posZ);
    ColumnQuadView getDataInQuad(int quadX, int quadZ, int quadXSize, int quadZSize);
    ColumnQuadView getFullQuad();

    /**
     * This method will clear all data at relative section position
     */
    void clear(int posX, int posZ);
    /**
     * This method will add the data given in input at the relative position and vertical index
     */
    boolean addData(long data, int posX, int posZ, int verticalIndex);
    /**
     * This methods will add the data in the given position if certain condition are satisfied
     * @param override if override is true we can override data created with same generation mode
     */
    boolean copyVerticalData(LodDataView data, int posX, int posZ, boolean override);
    void generateData(ColumnDatatype lowerDataContainer, int posX, int posZ);
}