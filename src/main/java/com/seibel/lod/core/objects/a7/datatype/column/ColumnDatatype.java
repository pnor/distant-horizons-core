package com.seibel.lod.core.objects.a7.datatype.column;

import com.seibel.lod.core.objects.LodDataView;
import com.seibel.lod.core.objects.a7.DHLevel;
import com.seibel.lod.core.objects.a7.LodQuadTree;
import com.seibel.lod.core.objects.a7.data.DataSourceLoader;
import com.seibel.lod.core.objects.a7.data.LodDataSource;
import com.seibel.lod.core.objects.a7.pos.DhSectionPos;
import com.seibel.lod.core.objects.a7.render.RenderDataSource;
import com.seibel.lod.core.objects.a7.render.RenderDataSourceLoader;
import com.seibel.lod.core.objects.opengl.RenderBuffer;
import com.seibel.lod.core.util.LodUtil;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicReference;

public class ColumnDatatype implements LodDataSource, RenderDataSource {
    public static final boolean DO_SAFETY_CHECKS = true;
    public static final byte SECTION_SIZE_OFFSET = 6;
    public static final int SECTION_SIZE = 1 << SECTION_SIZE_OFFSET;
    public static final int LATEST_VERSION = 9;

    public static final long DATA_TYPE_ID = "ColumnDatatype".hashCode();
    public final int AIR_LODS_SIZE = 16;
    public final int AIR_SECTION_SIZE = SECTION_SIZE/AIR_LODS_SIZE;
    public final int verticalSize;
    public final DhSectionPos sectionPos;
    public final int yOffset;

    public final long[] dataContainer;
    public final int[] airDataContainer;

    /**
     * Constructor of the ColumnDataType
     * @param maxVerticalSize the maximum vertical size of the container
     */
    public ColumnDatatype(DhSectionPos sectionPos, int maxVerticalSize, int yOffset) {
        verticalSize = maxVerticalSize;
        dataContainer = new long[SECTION_SIZE * SECTION_SIZE * verticalSize];
        airDataContainer = new int[AIR_SECTION_SIZE * AIR_SECTION_SIZE * verticalSize];
        this.sectionPos = sectionPos;
        this.yOffset = yOffset;
    }

    // Load from data stream with maxVerticalSize loaded from the data stream
    public ColumnDatatype(DhSectionPos sectionPos, DataInputStream inputData, int version, DHLevel level) throws IOException {
        this.sectionPos = sectionPos;
        this.yOffset = level.getMinY();
        byte detailLevel = inputData.readByte();
        if (sectionPos.sectionDetail - SECTION_SIZE_OFFSET != detailLevel) {
            throw new IOException("Invalid data: detail level does not match");
        }
        verticalSize = inputData.readByte() & 0b01111111;
        switch (version) {
            case 6:
                dataContainer = readDataVersion6(inputData, verticalSize);
                break;
            case 7:
                dataContainer = readDataVersion7(inputData, verticalSize);
                break;
            case 8:
                dataContainer = readDataVersion8(inputData, verticalSize);
                break;
            case 9:
            case 10:
                dataContainer = readDataVersion9(inputData, verticalSize);
                break;
            default:
                throw new IOException("Invalid Data: The version of the data is not supported");
        }
        airDataContainer = new int[AIR_SECTION_SIZE * AIR_SECTION_SIZE * verticalSize];
    }

    // Load from data stream with new maxVerticalSize
    public ColumnDatatype(DhSectionPos sectionPos, DataInputStream inputData, int version, DHLevel level, int maxVerticalSize) throws IOException {
        verticalSize = maxVerticalSize;
        this.yOffset = level.getMinY();
        this.sectionPos = sectionPos;
        byte detailLevel = inputData.readByte();
        if (sectionPos.sectionDetail - SECTION_SIZE_OFFSET  != detailLevel) {
            throw new IOException("Invalid data: detail level does not match");
        }
        int fileMaxVerticalSize = inputData.readByte() & 0b01111111;
        long[] fileDataContainer = null;
        switch (version) {
            case 6:
                fileDataContainer = readDataVersion6(inputData, fileMaxVerticalSize);
                break;
            case 7:
                fileDataContainer = readDataVersion7(inputData, fileMaxVerticalSize);
                break;
            case 8:
                fileDataContainer = readDataVersion8(inputData, fileMaxVerticalSize);
                break;
            case 9:
            case 10:
                fileDataContainer = readDataVersion9(inputData, fileMaxVerticalSize);
                break;
            default:
                throw new IOException("Invalid Data: The version of the data is not supported");
        }
        dataContainer = new long[SECTION_SIZE * SECTION_SIZE * verticalSize];
        new ColumnArrayView(dataContainer, dataContainer.length, 0, verticalSize).changeVerticalSizeFrom(
                new ColumnArrayView(fileDataContainer, fileDataContainer.length, 0, fileMaxVerticalSize));
        airDataContainer = new int[AIR_SECTION_SIZE * AIR_SECTION_SIZE * verticalSize];
    }

    /**
     * This method will clear all data at relative section position
     * @param posX
     * @param posZ
     */
    public void clear(int posX, int posZ)
    {
        for (int verticalIndex = 0; verticalIndex < verticalSize; verticalIndex++)
            dataContainer[posX * SECTION_SIZE * verticalSize + posZ * verticalSize + verticalIndex] =
                    ColumnDataPoint.EMPTY_DATA;
    }

    /**
     * This method will add the data given in input at the relative position and vertical index
     * @param data
     * @param posX
     * @param posZ
     * @param verticalIndex
     * @return
     */
    public boolean addData(long data, int posX, int posZ, int verticalIndex)
    {
        dataContainer[posX * SECTION_SIZE * verticalSize + posZ * verticalSize + verticalIndex] = data;
        return true;
    }

    /**
     * This section will fill the data given in input at the given position
     * @param data
     * @param posX
     * @param posZ
     */
    private void forceWriteVerticalData(long[] data, int posX, int posZ)
    {
        int index = posX * SECTION_SIZE * verticalSize + posZ * verticalSize;
        if (verticalSize >= 0) System.arraycopy(data, 0, dataContainer, index + 0, verticalSize);
    }

    /**
     * This methods will add the data in the given position if certain condition are satisfied
     * @param data
     * @param posX
     * @param posZ
     * @param override if override is true we can override data created with same generation mode
     * @return
     */
    public boolean copyVerticalData(LodDataView data, int posX, int posZ, boolean override) {
        if (DO_SAFETY_CHECKS) {
            if (data.size() != verticalSize)
                throw new IllegalArgumentException("data size not the same as vertical size");
            if (posX < 0 || posX >= SECTION_SIZE)
                throw new IllegalArgumentException("X position is out of bounds");
            if (posZ < 0 || posZ >= SECTION_SIZE)
                throw new IllegalArgumentException("Z position is out of bounds");
        }
        int index = posX * SECTION_SIZE * verticalSize + posZ * verticalSize;
        int compare = ColumnDataPoint.compareDatapointPriority(data.get(0), dataContainer[index]);
        if (override) {
            if (compare<0) return false;
        } else {
            if (compare<=0) return false;
        }
        data.copyTo(dataContainer, index);
        return true;
    }

    public long getData(int posX, int posZ, int verticalIndex)
    {
        return dataContainer[posX * SECTION_SIZE * verticalSize + posZ * verticalSize + verticalIndex];
    }

    public long getSingleData(int posX, int posZ)
    {
        return dataContainer[posX * SECTION_SIZE * verticalSize + posZ * verticalSize];
    }

    public long[] getAllData(int posX, int posZ)
    {
        long[] result = new long[verticalSize];
        int index = posX * SECTION_SIZE * verticalSize + posZ * verticalSize;
        System.arraycopy(dataContainer, index, result, 0, verticalSize);
        return result;
    }

    public ColumnArrayView getVerticalDataView(int posX, int posZ) {
        return new ColumnArrayView(dataContainer, verticalSize,
                posX * SECTION_SIZE * verticalSize + posZ * verticalSize, verticalSize);
    }

    public ColumnQuadView getDataInQuad(int quadX, int quadZ, int quadXSize, int quadZSize) {
        return new ColumnQuadView(dataContainer, SECTION_SIZE, verticalSize, quadX, quadZ, quadXSize, quadZSize);
    }
    public ColumnQuadView getFullQuad() {
        return new ColumnQuadView(dataContainer, SECTION_SIZE, verticalSize, 0, 0, SECTION_SIZE, SECTION_SIZE);
    }

    public int getVerticalSize()
    {
        return verticalSize;
    }

    public boolean doesItExist(int posX, int posZ)
    {
        return ColumnDataPoint.doesItExist(getSingleData(posX, posZ));
    }
    private long[] readDataVersion6(DataInputStream inputData, int tempMaxVerticalData) throws IOException {
        int x = SECTION_SIZE * SECTION_SIZE * tempMaxVerticalData;
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
        int x = SECTION_SIZE * SECTION_SIZE * tempMaxVerticalData;
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
        int x = SECTION_SIZE * SECTION_SIZE * tempMaxVerticalData;
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
        int x = SECTION_SIZE * SECTION_SIZE * tempMaxVerticalData;
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

    private static final ThreadLocal<long[][]> tLocalVerticalUpdateArrays = ThreadLocal.withInitial(() ->
    {
        return new long[LodUtil.DETAIL_OPTIONS - 1][];
    });

    public void generateData(ColumnDatatype lowerDataContainer, int posX, int posZ)
    {
        ColumnQuadView quadView = lowerDataContainer.getDataInQuad(posX*2, posZ*2, 2,2);
        ColumnArrayView outputView = getVerticalDataView(posX, posZ);
        outputView.mergeMultiDataFrom(quadView);
    }

    boolean writeData(DataOutputStream output) throws IOException {
        output.writeByte(getDataDetail());
        output.writeByte((byte) verticalSize);
        // FIXME: yOffset is a int, but we only are writing a short.
        output.writeByte((byte) (yOffset & 0xFF));
        output.writeByte((byte) ((yOffset >> 8) & 0xFF));
        boolean allGenerated = true;
        int x = SECTION_SIZE * SECTION_SIZE;
        for (int i = 0; i < x; i++)
        {
            for (int j = 0; j < verticalSize; j++)
            {
                long current = dataContainer[i * verticalSize + j];
                output.writeLong(Long.reverseBytes(current));
            }
            if (!ColumnDataPoint.doesItExist(dataContainer[i]))
                allGenerated = false;
        }
        return allGenerated;
    }

    public String toString()
    {
        String LINE_DELIMITER = "\n";
        String DATA_DELIMITER = " ";
        String SUBDATA_DELIMITER = ",";
        StringBuilder stringBuilder = new StringBuilder();
        int size = sectionPos.getWidth().value;
        stringBuilder.append(sectionPos);
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

    public int getMaxNumberOfLods()
    {
        return SECTION_SIZE * SECTION_SIZE * getVerticalSize();
    }

    public long getRoughRamUsage()
    {
        return (long) dataContainer.length * Long.BYTES;
    }


    // Called by ColumnDataLoader
    static LodDataSource loadFile(DHLevel level, DhSectionPos pos, InputStream is, int version) {
        try (DataInputStream dis = new DataInputStream(is)) {
            return new ColumnDatatype(pos, dis, version, level);
        } catch (IOException e) {
            //FIXME: Log error
            return null;
        }
    }

    public static final ColumnRenderLoader COLUMN_LAYER_LOADER = new ColumnRenderLoader();
    public static final ColumnDataLoader COLUMN_DATA_LOADER = ColumnDataLoader.INSTANCE;
    static {
        LodQuadTree.registerLayerLoader(COLUMN_LAYER_LOADER, (byte) 7); // 7 or above
    }

    @Override
    public DataSourceLoader getLatestLoader() {
        return COLUMN_DATA_LOADER;
    }

    @Override
    public DhSectionPos getSectionPos() {
        return sectionPos;
    }

    @Override
    public byte getDataDetail() {
        return (byte) (sectionPos.sectionDetail - SECTION_SIZE_OFFSET);
    }

    @Override
    public void enableRender() {

    }

    @Override
    public void disableRender() {

    }

    @Override
    public boolean isRenderReady() {
        return false;
    }

    @Override
    public void dispose() {
    }

    @Override
    public byte getDetailOffset() {
        return SECTION_SIZE_OFFSET;
    }

    @Override
    public boolean trySwapRenderBuffer(AtomicReference<RenderBuffer> referenceSlot) {
        return false;
    }

}
