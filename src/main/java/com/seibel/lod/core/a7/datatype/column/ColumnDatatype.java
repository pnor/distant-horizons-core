package com.seibel.lod.core.a7.datatype.column;

import com.seibel.lod.core.a7.data.DataSourceLoader;
import com.seibel.lod.core.a7.pos.DhSectionPos;
import com.seibel.lod.core.a7.render.RenderBuffer;
import com.seibel.lod.core.enums.ELodDirection;
import com.seibel.lod.core.objects.LodDataView;
import com.seibel.lod.core.a7.level.DhClientServerLevel;
import com.seibel.lod.core.a7.LodQuadTree;
import com.seibel.lod.core.a7.LodSection;
import com.seibel.lod.core.a7.render.RenderDataSource;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class ColumnDatatype implements RenderDataSource, IColumnDatatype {
    public static final boolean DO_SAFETY_CHECKS = true;
    public static final byte SECTION_SIZE_OFFSET = 6;
    public static final int SECTION_SIZE = 1 << SECTION_SIZE_OFFSET;
    public static final int LATEST_VERSION = 10;
    public static final long DATA_TYPE_ID = "ColumnDatatype".hashCode();
    public static final int AIR_LODS_SIZE = 16;
    public static final int AIR_SECTION_SIZE = SECTION_SIZE/AIR_LODS_SIZE;

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

    private long[] loadData(DataInputStream inputData, int version, int verticalSize) throws IOException {
        switch (version) {
            case 1:
                return readDataV1(inputData, verticalSize);
            default:
                throw new IOException("Invalid Data: The version of the data is not supported");
        }
    }
    private long[] readDataV1(DataInputStream inputData, int tempMaxVerticalData) throws IOException {
        int x = SECTION_SIZE * SECTION_SIZE * tempMaxVerticalData;
        byte[] data = new byte[x * Long.BYTES];
        short tempMinHeight = Short.reverseBytes(inputData.readShort());
        ByteBuffer bb = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        inputData.readFully(data);
        long[] result = new long[x];
        bb.asLongBuffer().get(result);
        if (tempMinHeight != yOffset) {
            for (int i=0; i<result.length; i++) {
                result[i] = ColumnDataPoint.shiftHeightAndDepth(result[i], (short) (tempMinHeight - yOffset));
            }
        }
        return result;
    }
    // Load from data stream with maxVerticalSize loaded from the data stream
    public ColumnDatatype(DhSectionPos sectionPos, DataInputStream inputData, int version, DhClientServerLevel level) throws IOException {
        this.sectionPos = sectionPos;
        yOffset = level.getMinY();
        byte detailLevel = inputData.readByte();
        if (sectionPos.sectionDetail - SECTION_SIZE_OFFSET != detailLevel) {
            throw new IOException("Invalid data: detail level does not match");
        }
        verticalSize = inputData.readByte() & 0b01111111;
        dataContainer = loadData(inputData, version, verticalSize);
        airDataContainer = new int[AIR_SECTION_SIZE * AIR_SECTION_SIZE * verticalSize];
    }

    @Override
    public void clear(int posX, int posZ)
    {
        for (int verticalIndex = 0; verticalIndex < verticalSize; verticalIndex++)
            dataContainer[posX * SECTION_SIZE * verticalSize + posZ * verticalSize + verticalIndex] =
                    ColumnDataPoint.EMPTY_DATA;
    }


    @Override
    public boolean addData(long data, int posX, int posZ, int verticalIndex)
    {
        dataContainer[posX * SECTION_SIZE * verticalSize + posZ * verticalSize + verticalIndex] = data;
        return true;
    }

    @Override
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

    @Override
    public long getData(int posX, int posZ, int verticalIndex)
    {
        return dataContainer[posX * SECTION_SIZE * verticalSize + posZ * verticalSize + verticalIndex];
    }

    @Override
    public long[] getAllData(int posX, int posZ)
    {
        long[] result = new long[verticalSize];
        int index = posX * SECTION_SIZE * verticalSize + posZ * verticalSize;
        System.arraycopy(dataContainer, index, result, 0, verticalSize);
        return result;
    }

    @Override
    public ColumnArrayView getVerticalDataView(int posX, int posZ) {
        return new ColumnArrayView(dataContainer, verticalSize,
                posX * SECTION_SIZE * verticalSize + posZ * verticalSize, verticalSize);
    }

    @Override
    public ColumnQuadView getDataInQuad(int quadX, int quadZ, int quadXSize, int quadZSize) {
        return new ColumnQuadView(dataContainer, SECTION_SIZE, verticalSize, quadX, quadZ, quadXSize, quadZSize);
    }
    @Override
    public ColumnQuadView getFullQuad() {
        return new ColumnQuadView(dataContainer, SECTION_SIZE, verticalSize, 0, 0, SECTION_SIZE, SECTION_SIZE);
    }

    @Override
    public int getVerticalSize()
    {
        return verticalSize;
    }

    @Override
    public boolean doesItExist(int posX, int posZ)
    {
        return ColumnDataPoint.doesItExist(getSingleData(posX, posZ));
    }


    @Override
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

    @Override
    public int getMaxNumberOfLods()
    {
        return SECTION_SIZE * SECTION_SIZE * getVerticalSize();
    }

    @Override
    public long getRoughRamUsage()
    {
        return (long) dataContainer.length * Long.BYTES;
    }

    public static ColumnRenderLoader COLUMN_LAYER_LOADER;

    private static boolean hasRendered = false;
    public static void REGISTER() { //FIXME: THIS IS A MESS
        if (hasRendered) return;
        COLUMN_LAYER_LOADER = new ColumnRenderLoader();
        LodQuadTree.registerLayerLoader(COLUMN_LAYER_LOADER, (byte) 7); // 7 or above
        hasRendered = true;
    }

    @Override
    public DataSourceLoader getLatestLoader() {
        return ColumnDataLoader.INSTANCE;
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
    public byte getDetailOffset() {
        return SECTION_SIZE_OFFSET;
    }

    private CompletableFuture<ColumnRenderBuffer> inBuildRenderBuffer = null;
    private ColumnRenderBuffer usedBuffer = null;


    private void tryBuildBuffer(LodQuadTree quadTree) {
        if (inBuildRenderBuffer == null) {
            ColumnDatatype[] data = new ColumnDatatype[ELodDirection.ADJ_DIRECTIONS.length];
            for (ELodDirection direction : ELodDirection.ADJ_DIRECTIONS) {
                LodSection section = quadTree.getSection(sectionPos.getAdjacent(direction)); //FIXME: Handle traveling through different detail levels
                if (section.getRenderContainer() != null && section.getRenderContainer() instanceof ColumnRenderBuffer) {
                    data[direction.ordinal()-2] = ((ColumnDatatype) section.getRenderContainer());
                }
            }
            inBuildRenderBuffer = ColumnRenderBuffer.build(usedBuffer, this, data);
        }
    }
    private void cancelBuildBuffer() {
        if (inBuildRenderBuffer != null) {
            inBuildRenderBuffer.cancel(false);
            inBuildRenderBuffer = null;
        }
    }
    @Override
    public void enableRender(LodQuadTree quadTree) {
        tryBuildBuffer(quadTree);
    }

    @Override
    public void disableRender() {
        cancelBuildBuffer();
    }

    @Override
    public boolean isRenderReady() {
        return (inBuildRenderBuffer != null && inBuildRenderBuffer.isDone());
    }

    @Override
    public void dispose() {
        cancelBuildBuffer();
    }


    @Override
    public boolean trySwapRenderBuffer(LodQuadTree quadTree, AtomicReference<RenderBuffer> referenceSlot) {
        if (inBuildRenderBuffer != null && inBuildRenderBuffer.isDone()) {
            RenderBuffer oldBuffer = referenceSlot.getAndSet(inBuildRenderBuffer.join());
            if (oldBuffer != null && oldBuffer instanceof ColumnRenderBuffer) usedBuffer = (ColumnRenderBuffer) oldBuffer;
            inBuildRenderBuffer = null;
            return true;
        } else {
            tryBuildBuffer(quadTree);
        }
        return false;
    }



}
