package com.seibel.lod.core.objects.a7.datatype.column;

import com.seibel.lod.core.objects.LodDataView;
import com.seibel.lod.core.objects.a7.DHLevel;
import com.seibel.lod.core.objects.a7.LodQuadTree;
import com.seibel.lod.core.objects.a7.data.DataFile;
import com.seibel.lod.core.objects.a7.data.LodDataSource;
import com.seibel.lod.core.objects.a7.datatype.full.FullDatatype;
import com.seibel.lod.core.objects.a7.pos.DhSectionPos;
import com.seibel.lod.core.objects.a7.render.RenderDataSource;
import com.seibel.lod.core.objects.a7.render.RenderDataSourceLoader;
import com.seibel.lod.core.objects.opengl.RenderBuffer;
import com.seibel.lod.core.util.DetailDistanceUtil;
import com.seibel.lod.core.util.LodUtil;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
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

    public boolean writeData(DataOutputStream output) throws IOException {
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

    public static LodDataSource loadFile(DHLevel level, DhSectionPos pos, InputStream is, int version) {
        try (DataInputStream dis = new DataInputStream(is)) {
            return new ColumnDatatype(pos, dis, version, level);
        } catch (IOException e) {
            //FIXME: Log error
            return null;
        }
    }

    public static RenderDataSourceLoader COLUMN_LAYER_LOADER = new RenderDataSourceLoader(4) {
            @Override
            public RenderDataSource construct(List<LodDataSource> dataSources, DhSectionPos sectionPos, DHLevel level) {
                if (dataSources.size() == 0) return null;

                // Check for direct casting
                if (dataSources.size() == 1 && dataSources.get(0) instanceof ColumnDatatype
                    && dataSources.get(0).getSectionPos().equals(sectionPos)
                    && dataSources.get(0).getDataDetail() == sectionPos.sectionDetail-SECTION_SIZE_OFFSET) {
                    // Directly using the data source as the render data source is possible.
                    return (ColumnDatatype) dataSources.get(0);
                }

                // Otherwise, we need to create a new render data source, and copy the data from the data sources.
                ColumnDatatype renderDataSource = new ColumnDatatype(sectionPos,
                        DetailDistanceUtil.getMaxVerticalData(sectionPos.sectionDetail-SECTION_SIZE_OFFSET),
                        level.getMinY());
                boolean completeCopy = dataSources.get(0).getSectionPos().getWidth().toBlock() >= sectionPos.getWidth().toBlock();

                if (completeCopy) {
                    // If there is only one data source, we need to insure on copy, we don't copy out of bounds as we
                    //  may just need to copy partial section of the data source.
                    LodUtil.assertTrue(dataSources.size() == 1, "Expected only one data source for complete copy");
                    byte targetDataLevel = (byte) (sectionPos.sectionDetail-SECTION_SIZE_OFFSET);
                    byte sourceDataLevel = dataSources.get(0).getDataDetail();
                    LodUtil.assertTrue(targetDataLevel >= sourceDataLevel);
                    if (dataSources.get(0) instanceof ColumnDatatype) {
                        ColumnDatatype dataSource = (ColumnDatatype) dataSources.get(0);
                        DhSectionPos srcPos = dataSource.getSectionPos();

                        // Note that in here, the source data level will be always < target section level
                        int trgX = sectionPos.getCorner().getX().toBlock();
                        int trgZ = sectionPos.getCorner().getZ().toBlock();
                        int trgMaxX = trgX + sectionPos.getWidth().toBlock() - 1;
                        int trgMaxZ = trgZ + sectionPos.getWidth().toBlock() - 1;
                        int trgXSizeInSrc = (trgX >> sourceDataLevel) - (trgMaxX >> sourceDataLevel) + 1;
                        int trgZSizeInSrc = (trgZ >> sourceDataLevel) - (trgMaxZ >> sourceDataLevel) + 1;
                        int trgXInSrc = (trgX >> sourceDataLevel) % srcPos.getWidth(sourceDataLevel).value;
                        int trgZInSrc = (trgZ >> sourceDataLevel) % srcPos.getWidth(sourceDataLevel).value;

                        ColumnQuadView srcView = dataSource.getDataInQuad(trgXInSrc, trgZInSrc, trgXSizeInSrc, trgZSizeInSrc);
                        ColumnQuadView trgView = renderDataSource.getFullQuad();
                        trgView.mergeMultiColumnFrom(srcView);
                    } else {
                        if (!(dataSources.get(0) instanceof FullDatatype))
                            throw new IllegalArgumentException("Unsupported data source type: " + dataSources.get(0).getClass().getName());
                        FullDatatype dataSource = (FullDatatype) dataSources.get(0);
                        DhSectionPos srcPos = dataSource.getSectionPos();
                        //TODO: Impl this
                        LodUtil.assertTrue(false,"Not implemented yet");
                    }
                } else {
                    // If there are multiple data sources, we need to merge them into the target data source
                    for (LodDataSource dataSource : dataSources) {
                        byte targetDataLevel = (byte) (sectionPos.sectionDetail-SECTION_SIZE_OFFSET);
                        byte sourceDataLevel = dataSource.getDataDetail();
                        DhSectionPos srcPos = dataSource.getSectionPos();

                        if (dataSource instanceof ColumnDatatype) {
                            ColumnDatatype clDataSource = (ColumnDatatype) dataSource;

                            // Note that targetDataLevel can be > source section level
                            int srcX = srcPos.getCorner().getX().toBlock();
                            int srcZ = srcPos.getCorner().getZ().toBlock();
                            int srcMaxX = srcX + srcPos.getWidth().toBlock() - 1;
                            int srcMaxZ = srcZ + srcPos.getWidth().toBlock() - 1;
                            int srcXSizeInTrg = (srcX >> targetDataLevel) - (srcMaxX >> targetDataLevel) + 1;
                            int srcZSizeInTrg = (srcZ >> targetDataLevel) - (srcMaxZ >> targetDataLevel) + 1;
                            int srcXInTrg = (srcX >> targetDataLevel) % SECTION_SIZE;
                            int srcZInTrg = (srcZ >> targetDataLevel) % SECTION_SIZE;

                            ColumnQuadView srcView = clDataSource.getFullQuad();
                            ColumnQuadView trgView = renderDataSource.getDataInQuad(srcXInTrg, srcZInTrg, srcXSizeInTrg, srcZSizeInTrg);
                            trgView.mergeMultiColumnFrom(srcView);
                        } else {
                            if (!(dataSource instanceof FullDatatype))
                                throw new IllegalArgumentException("Unsupported data source type: " + dataSource.getClass().getName());
                            FullDatatype flDataSource = (FullDatatype) dataSource;
                            //TODO: Impl this
                            LodUtil.assertTrue(false,"Not implemented yet");
                        }
                    }
                }

                return renderDataSource;
            }
            @Override
            public List<DataFile> selectFiles(DhSectionPos sectionPos, DHLevel level, List<DataFile>[] availableFiles) {
                byte targetDataLevel = (byte) (sectionPos.sectionDetail - SECTION_SIZE_OFFSET);
                //No support for loading higher than the target level yet.
                byte maxDataLevel = LodUtil.min((byte) (availableFiles.length-1), targetDataLevel);
                byte topValidDataLevel = Byte.MIN_VALUE;
                List<DataFile> selectedFiles = new LinkedList<>();

                for (int detail = maxDataLevel; detail >= 0; detail--) {
                    if (availableFiles[detail] == null) continue;
                    if (topValidDataLevel == Byte.MIN_VALUE) {
                        for (DataFile dataFile : availableFiles[detail]) {
                            if (dataFile.dataLevel > targetDataLevel) continue;
                            if (dataFile.dataType == ColumnDatatype.class || dataFile.dataType == FullDatatype.class) {
                                topValidDataLevel = LodUtil.max(topValidDataLevel, dataFile.dataLevel);
                                break;
                            }
                        }
                    }
                    if (topValidDataLevel == Byte.MIN_VALUE) continue;


                    DataFile singleCoveringColumnFile = null;
                    DataFile singleCoveringFullFile = null;

                    for (DataFile dataFile : availableFiles[detail]) {
                        if (dataFile.pos.getWidth().toBlock() == sectionPos.getWidth().toBlock()) {
                            if (dataFile.dataType == ColumnDatatype.class) {
                                singleCoveringColumnFile = dataFile;
                                break;
                            }
                            else if (dataFile.dataType == FullDatatype.class) {
                                singleCoveringFullFile = dataFile;
                                // Don't break as there may be a column file later.
                            }
                        } else if (dataFile.pos.getWidth().toBlock() > sectionPos.getWidth().toBlock()) {
                            if (dataFile.dataType == ColumnDatatype.class && singleCoveringColumnFile == null)
                                singleCoveringColumnFile = dataFile;
                            else if (dataFile.dataType == FullDatatype.class && singleCoveringFullFile == null)
                                singleCoveringFullFile = dataFile;
                        }
                    }

                    // First, try select single file that has enough width to cover the section
                    if (singleCoveringColumnFile != null) return Collections.singletonList(singleCoveringColumnFile);
                    if (singleCoveringFullFile != null) return Collections.singletonList(singleCoveringFullFile);

                    // If no single file covers the section, try to select all files without any duplicates
                    for (DataFile dataFile : availableFiles[detail]) {
                        boolean isDuplicate = false;
                        boolean isSet = false;
                        for (int i = 0; i < selectedFiles.size(); i++) {
                            DataFile selectedFile = selectedFiles.get(i);
                            if (selectedFile == null) continue;
                            if (selectedFile.pos.overlaps(dataFile.pos)) {
                                // Now, the already selected file muct have same or higher data level
                                //  so, we just select the file with a position that covers the most area.
                                // Therefore, we choose the file with the higher section level.
                                if (selectedFile.pos.sectionDetail < dataFile.pos.sectionDetail) {
                                    if (isSet) selectedFiles.set(i, null);
                                    else selectedFiles.set(i, dataFile);
                                    isSet = true;
                                } else {
                                    LodUtil.assertTrue(!isSet); // We should not have encountered a smaller section level.
                                    // This mean its completely covered by the selected file, so we can skip it.
                                    isDuplicate = true;
                                    break;
                                }
                            }
                        }
                        if (!isDuplicate && !isSet) selectedFiles.add(dataFile);
                    }
                }
                if (topValidDataLevel == Byte.MIN_VALUE) return Collections.emptyList();
                selectedFiles.removeIf(Objects::isNull);
                return selectedFiles;
            }
        };
    static {
        LodQuadTree.registerLayerLoader(COLUMN_LAYER_LOADER, (byte) 7); // 7 or above
    }

    @Override
    public DataSourceLoader getLatestLoader() {
        return (DHLevel level, DhSectionPos sectionPos, InputStream data) -> loadFile(level, sectionPos, data, LATEST_VERSION);
    }

    @Override
    public <T> T[] getData() {
        return null;
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
        return 0;
    }

    @Override
    public boolean trySwapRenderBuffer(AtomicReference<RenderBuffer> referenceSlot) {
        return false;
    }
}
