package com.seibel.lod.core.a7.datatype.column;

import com.seibel.lod.core.a7.data.DataSourceLoader;
import com.seibel.lod.core.a7.pos.DhSectionPos;
import com.seibel.lod.core.objects.LodDataView;
import com.seibel.lod.core.a7.level.DhClientServerLevel;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class OldColumnDatatype implements IColumnDatatype {
    public static final boolean DO_SAFETY_CHECKS = true;
    public static final int LATEST_VERSION = 10;
    public static final long DATA_TYPE_ID = "OldColumnDatatype".hashCode();

    public final byte sectionSizeOffset;
    public final int getSectSize() {
        return 1 << sectionSizeOffset;
    }

    public final int verticalSize;
    public final DhSectionPos sectionPos;
    public final int yOffset;
    public final long[] dataContainer;

    /**
     * Constructor of the ColumnDataType
     * @param maxVerticalSize the maximum vertical size of the container
     */
    public OldColumnDatatype(DhSectionPos sectionPos, int maxVerticalSize, int yOffset, int sectionSizeOffset) {
        this.sectionSizeOffset = (byte) sectionSizeOffset;
        verticalSize = maxVerticalSize;
        dataContainer = new long[getSectSize() * getSectSize() * verticalSize];
        this.sectionPos = sectionPos;
        this.yOffset = yOffset;
    }

    private long[] loadData(DataInputStream inputData, int version, int verticalSize) throws IOException {
        switch (version) {
            case 6:
                return readDataVersion6(inputData, verticalSize);
            case 7:
                return readDataVersion7(inputData, verticalSize);
            case 8:
                return readDataVersion8(inputData, verticalSize);
            case 9:
            case 10:
                return readDataVersion9(inputData, verticalSize);
            default:
                throw new IOException("Invalid Data: The version of the data is not supported");
        }
    }

    // Load from data stream with maxVerticalSize loaded from the data stream
    public OldColumnDatatype(DhSectionPos sectionPos, DataInputStream inputData, int version, DhClientServerLevel level, int sectionSizeOffset) throws IOException {
        this.sectionSizeOffset = (byte) sectionSizeOffset;
        this.sectionPos = sectionPos;
        yOffset = level.getMinY();
        byte detailLevel = inputData.readByte();
        if (sectionPos.sectionDetail - sectionSizeOffset != detailLevel) {
            throw new IOException("Invalid data: detail level does not match");
        }
        verticalSize = inputData.readByte() & 0b01111111;
        dataContainer = loadData(inputData, version, verticalSize);
    }

    @Override
    public byte getDetailOffset() {
        return sectionSizeOffset;
    }

    public void clear(int posX, int posZ)
    {
        throw new UnsupportedOperationException("OldColumnDatatype only supports read-only access." +
                " Convert to ColumnDatatype first before doing any modifications.");
    }

    public boolean addData(long data, int posX, int posZ, int verticalIndex)
    {
        throw new UnsupportedOperationException("OldColumnDatatype only supports read-only access." +
                " Convert to ColumnDatatype first before doing any modifications.");
    }

    public boolean copyVerticalData(LodDataView data, int posX, int posZ, boolean override) {
        throw new UnsupportedOperationException("OldColumnDatatype only supports read-only access." +
                " Convert to ColumnDatatype first before doing any modifications.");
    }

    public long getData(int posX, int posZ, int verticalIndex)
    {
        return dataContainer[posX * getSectSize() * verticalSize + posZ * verticalSize + verticalIndex];
    }

    public long[] getAllData(int posX, int posZ)
    {
        long[] result = new long[verticalSize];
        int index = posX * getSectSize() * verticalSize + posZ * verticalSize;
        System.arraycopy(dataContainer, index, result, 0, verticalSize);
        return result;
    }

    public ColumnArrayView getVerticalDataView(int posX, int posZ) {
        return new ColumnArrayView(dataContainer, verticalSize,
                posX * getSectSize() * verticalSize + posZ * verticalSize, verticalSize);
    }

    public ColumnQuadView getDataInQuad(int quadX, int quadZ, int quadXSize, int quadZSize) {
        return new ColumnQuadView(dataContainer, getSectSize(), verticalSize, quadX, quadZ, quadXSize, quadZSize);
    }
    public ColumnQuadView getFullQuad() {
        return new ColumnQuadView(dataContainer, getSectSize(), verticalSize, 0, 0, getSectSize(), getSectSize());
    }

    public int getVerticalSize()
    {
        return verticalSize;
    }

    public boolean doesItExist(int posX, int posZ)
    {
        return ColumnDataPoint.doesItExist(getSingleData(posX, posZ));
    }

    @Override
    public void generateData(ColumnDatatype lowerDataContainer, int posX, int posZ) {
        throw new UnsupportedOperationException("OldColumnDatatype only supports read-only access." +
                " Convert to ColumnDatatype first before doing any modifications.");
    }

    private long[] readDataVersion6(DataInputStream inputData, int tempMaxVerticalData) throws IOException {
        int x = getSectSize() * getSectSize() * tempMaxVerticalData;
        byte[] data = new byte[x * Long.BYTES];
        ByteBuffer bb = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        inputData.readFully(data);
        long[] result = new long[x];
        bb.asLongBuffer().get(result);
        patchVersion9Reorder(result);
        patchHeightAndDepth(result,-yOffset);
        return result;
    }
    private long[] readDataVersion7(DataInputStream inputData, int tempMaxVerticalData) throws IOException {
        int x = getSectSize() * getSectSize() * tempMaxVerticalData;
        byte[] data = new byte[x * Long.BYTES];
        ByteBuffer bb = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        inputData.readFully(data);
        long[] result = new long[x];
        bb.asLongBuffer().get(result);
        patchVersion9Reorder(result);
        patchHeightAndDepth(result, 64 - yOffset);
        return result;
    }
    private long[] readDataVersion8(DataInputStream inputData, int tempMaxVerticalData) throws IOException {
        int x = getSectSize() * getSectSize() * tempMaxVerticalData;
        byte[] data = new byte[x * Long.BYTES];
        short tempMinHeight = Short.reverseBytes(inputData.readShort());
        ByteBuffer bb = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        inputData.readFully(data);
        long[] result = new long[x];
        bb.asLongBuffer().get(result);
        patchVersion9Reorder(result);
        if (tempMinHeight != yOffset) {
            patchHeightAndDepth(result,tempMinHeight - yOffset);
        }
        return result;
    }
    private long[] readDataVersion9(DataInputStream inputData, int tempMaxVerticalData) throws IOException {
        int x = getSectSize() * getSectSize() * tempMaxVerticalData;
        byte[] data = new byte[x * Long.BYTES];
        short tempMinHeight = Short.reverseBytes(inputData.readShort());
        ByteBuffer bb = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        inputData.readFully(data);
        long[] result = new long[x];
        bb.asLongBuffer().get(result);
        if (tempMinHeight != yOffset) {
            patchHeightAndDepth(result,tempMinHeight - yOffset);
        }
        return result;
    }

    private static void patchHeightAndDepth(long[] data, int offset) {
        for (int i=0; i<data.length; i++) {
            data[i] = ColumnDataPoint.shiftHeightAndDepth(data[i], (short)offset);
        }
    }

    private static void patchVersion9Reorder(long[] data) {
        for (int i=0; i<data.length; i++) {
            data[i] = ColumnDataPoint.version9Reorder(data[i]);
        }
    }

    @Override
    public String toString()
    {
        String LINE_DELIMITER = "\n";
        String DATA_DELIMITER = " ";
        String SUBDATA_DELIMITER = ",";
        StringBuilder stringBuilder = new StringBuilder();
        int size = sectionPos.getWidth().value;
        stringBuilder.append(sectionPos);
        stringBuilder.append("(LEGACY-READ-ONLY)");
        stringBuilder.append(LINE_DELIMITER);
        for (int z = 0; z < size; z++)
        {
            for (int x = 0; x < size; x++)
            {
                for (int y = 0; y < verticalSize; y++) {
                    //Converting the dataToHex
                    stringBuilder.append(Long.toHexString(getData(x,z,y)));
                    if (y != verticalSize-1) stringBuilder.append(SUBDATA_DELIMITER);
                }
                if (x != size-1) stringBuilder.append(DATA_DELIMITER);
            }
            if (z != size-1) stringBuilder.append(LINE_DELIMITER);
        }
        return stringBuilder.toString();
    }

    @Override
    public int getMaxNumberOfLods()
    {
        return getSectSize() * getSectSize() * getVerticalSize();
    }

    @Override
    public long getRoughRamUsage()
    {
        return (long) dataContainer.length * Long.BYTES;
    }

    @Override
    public DataSourceLoader getLatestLoader() {
        return Alpha6DataLoader.INSTANCE;
    }

    @Override
    public DhSectionPos getSectionPos() {
        return sectionPos;
    }

    @Override
    public byte getDataDetail() {
        return (byte) (sectionPos.sectionDetail - sectionSizeOffset);
    }


}
