package com.seibel.lod.core.objects.a7;

import com.seibel.lod.core.handlers.dependencyInjection.SingletonHandler;
import com.seibel.lod.core.objects.LodDataView;
import com.seibel.lod.core.objects.lod.LevelContainer;
import com.seibel.lod.core.objects.lod.VerticalLevelContainer;
import com.seibel.lod.core.util.DataPointUtil;
import com.seibel.lod.core.util.DetailDistanceUtil;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class RenderDataContainer
{
    public static final boolean DO_SAFETY_CHECKS = true;
    
    private final short minHeight;
    public final byte detailLevel;
    public final int SECTION_SIZE = 128;
    public final int AIR_LODS_SIZE = 16;
    public final int AIR_SECTION_SIZE = SECTION_SIZE/AIR_LODS_SIZE;
    public final int verticalSize;
    
    public final long[] dataContainer;
    public final int[] airDataContainer;
    
    /**
     * Constructor of the RenderDataContainer
     * @param detailLevel
     */
    public RenderDataContainer(byte detailLevel)
    {
        this.detailLevel = detailLevel;
        verticalSize = DetailDistanceUtil.getMaxVerticalData(detailLevel);
        
        dataContainer = new long[SECTION_SIZE * SECTION_SIZE * DetailDistanceUtil.getMaxVerticalData(detailLevel)];
        airDataContainer = new int[AIR_SECTION_SIZE * AIR_SECTION_SIZE * DetailDistanceUtil.getMaxVerticalData(detailLevel)];
        
        minHeight = SingletonHandler.get(IMinecraftClientWrapper.class).getWrappedClientWorld().getMinHeight();
    }
    
    public byte getDetailLevel()
    {
        return detailLevel;
    }
    
    /**
     * This method will clear all data at relative section position
     * @param posX
     * @param posZ
     */
    public void clear(int posX, int posZ)
    {
        for (int verticalIndex = 0; verticalIndex < verticalSize; verticalIndex++)
            dataContainer[posX * SECTION_SIZE * verticalSize + posZ * verticalSize + verticalIndex] = DataPointUtil.EMPTY_DATA;
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
    public boolean addVerticalData(long[] data, int posX, int posZ, boolean override)
    {
        int index = posX * SECTION_SIZE * verticalSize + posZ * verticalSize;
        int compare = DataPointUtil.compareDatapointPriority(data[0], dataContainer[index]);
        if (override) {
            if (compare<0) return false;
        } else {
            if (compare<=0) return false;
        }
        forceWriteVerticalData(data, posX, posZ);
        return true;
    }
    
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
        int compare = DataPointUtil.compareDatapointPriority(data.get(0), dataContainer[index]);
        if (override) {
            if (compare<0) return false;
        } else {
            if (compare<=0) return false;
        }
        data.copyTo(dataContainer, index);
        return true;
    }
    
    public boolean addChunkOfData(long[] data, int posX, int posZ, int widthX, int widthZ, boolean override)
    {
        boolean anyChange = false;
        if (posX+widthX > SECTION_SIZE || posZ+widthZ > SECTION_SIZE)
            throw new IndexOutOfBoundsException("addChunkOfData param not inside valid range");
        if (widthX*widthZ*verticalSize != data.length)
            throw new IndexOutOfBoundsException("addChunkOfData data array not sized correctly to contain the data to be copied");
        if (posX<0 || posZ<0 || widthX<0 || widthZ<0)
            throw new IndexOutOfBoundsException("addChunkOfData param is negative");
        
        for (int ox=0; ox<widthX; ox++) {
            anyChange = DataPointUtil.mergeTwoDataArray(
                    dataContainer, ((ox+posX)* SECTION_SIZE +posZ) * verticalSize,
                    data, ox*widthX*verticalSize,
                    widthZ, verticalSize, override);
        }
        return anyChange;
    }
    
    public boolean copyChunkOfData(LodDataView data, int posX, int posZ, int widthX, int widthZ, boolean override) {
        boolean anyChange = false;
        if (posX+widthX > SECTION_SIZE || posZ+widthZ > SECTION_SIZE)
            throw new IndexOutOfBoundsException("addChunkOfData param not inside valid range");
        if (widthX*widthZ*verticalSize != data.size())
            throw new IndexOutOfBoundsException("addChunkOfData data array not sized correctly to contain the data to be copied");
        if (posX<0 || posZ<0 || widthX<0 || widthZ<0)
            throw new IndexOutOfBoundsException("addChunkOfData param is negative");
        
        for (int ox=0; ox<widthX; ox++) {
            anyChange |= new LodDataView(dataContainer, widthX*verticalSize,
                    ((ox+posX)* SECTION_SIZE +posZ) * verticalSize)
                                 .mergeWith(data, verticalSize, override);
        }
        return anyChange;
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
    
    public LodDataView getVerticalDataView(int posX, int posZ) {
        return new LodDataView(dataContainer, verticalSize, posX * SECTION_SIZE * verticalSize + posZ * verticalSize);
    }
    
    public int getVerticalSize()
    {
        return verticalSize;
    }
    
    public int getSECTION_SIZE()
    {
        return SECTION_SIZE;
    }
    
    public boolean doesItExist(int posX, int posZ)
    {
        return DataPointUtil.doesItExist(getSingleData(posX, posZ));
    }
    
    private long[] readDataVersion6(DataInputStream inputData, int tempMaxVerticalData) throws IOException {
        int x = SECTION_SIZE * SECTION_SIZE * tempMaxVerticalData;
        byte[] data = new byte[x * Long.BYTES];
        ByteBuffer bb = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        inputData.readFully(data);
        long[] result = new long[x];
        bb.asLongBuffer().get(result);
        patchVersion9Reorder(result);
        patchHeightAndDepth(result,-minHeight);
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
        patchHeightAndDepth(result, 64 - minHeight);
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
        if (tempMinHeight != minHeight) {
            patchHeightAndDepth(result,tempMinHeight - minHeight);
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
        if (tempMinHeight != minHeight) {
            patchHeightAndDepth(result,tempMinHeight - minHeight);
        }
        return result;
    }
    
    private static void patchHeightAndDepth(long[] data, int offset) {
        for (int i=0; i<data.length; i++) {
            data[i] = DataPointUtil.shiftHeightAndDepth(data[i], (short)offset);
        }
    }
    
    private static void patchVersion9Reorder(long[] data) {
        for (int i=0; i<data.length; i++) {
            data[i] = DataPointUtil.version9Reorder(data[i]);
        }
    }
    
    public RenderDataContainer(DataInputStream inputData, int version, byte expectedDetailLevel) throws IOException {
        minHeight = SingletonHandler.get(IMinecraftClientWrapper.class).getWrappedClientWorld().getMinHeight();
        detailLevel = inputData.readByte();
        if (detailLevel != expectedDetailLevel)
            throw new IOException("Invalid Data: The expected detail level should be "+expectedDetailLevel+
                                          " but the data header say it's "+detailLevel);
        int fileMaxVerticalData = inputData.readByte() & 0b01111111;
        long[] tempDataContainer = null;
        
        switch (version) {
        case 6:
            tempDataContainer = readDataVersion6(inputData, fileMaxVerticalData);
            break;
        case 7:
            tempDataContainer = readDataVersion7(inputData, fileMaxVerticalData);
            break;
        case 8:
            tempDataContainer = readDataVersion8(inputData, fileMaxVerticalData);
            break;
        case 9:
            tempDataContainer = readDataVersion9(inputData, fileMaxVerticalData);
            break;
        default:
            assert false;
        }
        
        int targetMaxVerticalData = DetailDistanceUtil.getMaxVerticalData(detailLevel);
        verticalSize = targetMaxVerticalData;
        dataContainer = DataPointUtil.changeMaxVertSize(tempDataContainer, fileMaxVerticalData, verticalSize);
        airDataContainer = new int[AIR_SECTION_SIZE * AIR_SECTION_SIZE * DetailDistanceUtil.getMaxVerticalData(detailLevel)];
    }
    
    public LevelContainer expand()
    {
        return new VerticalLevelContainer((byte) (getDetailLevel() - 1));
    }
    
    private static final ThreadLocal<long[][]> tLocalVerticalUpdateArrays = ThreadLocal.withInitial(() ->
    {
        return new long[LodUtil.DETAIL_OPTIONS - 1][];
    });
    
    public void updateData(RenderDataContainer lowerRenderContainer, int posX, int posZ)
    {
        //We reset the array
        long[][] verticalUpdateArrays = tLocalVerticalUpdateArrays.get();
        long[] dataToMerge = verticalUpdateArrays[detailLevel-1];
        int arrayLength = DetailDistanceUtil.getMaxVerticalData(detailLevel-1) * 4;
        if (dataToMerge == null || dataToMerge.length != arrayLength) {
            dataToMerge = new long[arrayLength];
            verticalUpdateArrays[detailLevel-1] = dataToMerge;
        } else Arrays.fill(dataToMerge, 0);
        
        //int lowerMaxVertical = dataToMerge.length / 4;
        int lowerSectionSize = lowerRenderContainer.getSECTION_SIZE();
        int childPosStartX = Math.floorMod(2 * posX, lowerSectionSize);
        int childPosEndX = Math.floorMod(2 * posX + 1, lowerSectionSize);
        int childPosStartZ = Math.floorMod(2 * posZ, lowerSectionSize);
        int childPosEndZ = Math.floorMod(2 * posZ + 1, lowerSectionSize);
        
        long[] data;
        boolean anyDataExist = false;
    
        mergeAndAddDataFromOtherContainer(posX, posZ, lowerRenderContainer, childPosStartX, childPosEndX, childPosStartZ, childPosEndZ);
        /*
        TODO remove this old code when we are sure that this works
        for (int x = 0; x <= 1; x++)
        {
            for (int z = 0; z <= 1; z++)
            {
                childPosX = 2 * posX + x;
                childPosZ = 2 * posZ + z;
                if (lowerLevelContainer.doesItExist(childPosX, childPosZ)) anyDataExist = true;
                for (int verticalIndex = 0; verticalIndex < lowerMaxVertical; verticalIndex++)
                    dataToMerge[(z * 2 + x) * lowerMaxVertical + verticalIndex] = lowerLevelContainer.getData(childPosX, childPosZ, verticalIndex);
            }
        }
        if (!anyDataExist)
            throw new RuntimeException("Update data called but no child datapoint exist!");
        
        if ((!DataPointUtil.doesItExist(data[0])) && anyDataExist)
            throw new RuntimeException("Update data called but higher level datapoint doesn't exist even though child data does exist!");
        
        //FIXME: Disabled check if genMode for old data is already invalid due to having genMode 0.
        if (DataPointUtil.getGenerationMode(data[0]) != DataPointUtil.getGenerationMode(lowerLevelContainer.getSingleData(posX*2, posZ*2)))
            throw new RuntimeException("Update data called but higher level datapoint does not have the same GenerationMode as the top left corner child datapoint!");
        
        forceWriteVerticalData(data, posX, posZ);*/
    }
    
    public boolean writeData(DataOutputStream output) throws IOException {
        output.writeByte(detailLevel);
        output.writeByte((byte) verticalSize);
        output.writeByte((byte) (minHeight & 0xFF));
        output.writeByte((byte) ((minHeight >> 8) & 0xFF));
        boolean allGenerated = true;
        int x = SECTION_SIZE * SECTION_SIZE;
        for (int i = 0; i < x; i++)
        {
            for (int j = 0; j < verticalSize; j++)
            {
                long current = dataContainer[i * verticalSize + j];
                output.writeLong(Long.reverseBytes(current));
            }
            if (!DataPointUtil.doesItExist(dataContainer[i]))
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
        int size = 1 << (LodUtil.REGION_DETAIL_LEVEL - detailLevel);
        stringBuilder.append(detailLevel);
        stringBuilder.append(LINE_DELIMITER);
        for (int z = 0; z < size; z++)
        {
            for (int x = 0; x < size; x++)
            {
                for (int y = 0; y < verticalSize; y++) {
                    //Converting the dataToHex
                    stringBuilder.append(Long.toHexString(getData(x,z,y)));
                    if (y != verticalSize) stringBuilder.append(SUBDATA_DELIMITER);
                }
                if (x != size) stringBuilder.append(DATA_DELIMITER);
            }
            if (z != size) stringBuilder.append(LINE_DELIMITER);
        }
        return stringBuilder.toString();
    }
    
    public int getMaxNumberOfLods()
    {
        return SECTION_SIZE * SECTION_SIZE * getVerticalSize();
    }
    
    public long getRoughRamUsage()
    {
        return dataContainer.length * Long.BYTES;
    }
    
    
    
    private static final ThreadLocal<short[]> tLocalHeightAndDepth = new ThreadLocal<short[]>();
    private static final ThreadLocal<int[]> tDataIndexCache = new ThreadLocal<int[]>();
    /**
     *
     * This method merge column of multiple data together
     */
    // TODO: Make this operate on a out param array, to allow skipping copy array on use
    public void mergeAndAddDataFromOtherContainer(int mergeInX, int mergeInZ, RenderDataContainer lowerDataContainer, int mergeFromX, int mergeToX, int mergeFromZ, int mergeToZ)
    {
        int outBaseIndex = mergeInX * SECTION_SIZE * verticalSize + mergeInZ*verticalSize;
        int inputVerticalSize = lowerDataContainer.verticalSize;
        int inputSectionSize = lowerDataContainer.SECTION_SIZE;
        int mergeFromToX;
        int xSize = (mergeFromX - mergeToX + 1);
        int zSize = (mergeFromZ - mergeToZ + 1);
        //size indicate how many position we are merging in one position
        
        // We initialize the arrays that are going to be used
        int heightAndDepthLength = (DataPointUtil.MAX_WORLD_Y_SIZE / 2 + 16) * 2;
        short[] heightAndDepth = tLocalHeightAndDepth.get();
        if (heightAndDepth==null || heightAndDepth.length != heightAndDepthLength) {
            heightAndDepth = new short[heightAndDepthLength];
            tLocalHeightAndDepth.set(heightAndDepth);
        }
        int dataPointLength = verticalSize;
        
        int firstIndex = mergeFromX*SECTION_SIZE*inputVerticalSize + mergeFromZ * inputVerticalSize;
        byte genMode = DataPointUtil.getGenerationMode(lowerDataContainer.dataContainer[firstIndex]);
        if (genMode == 0) genMode = 1; // FIXME: Hack to make the version 10 genMode never be 0.
        boolean allEmpty = true;
        boolean allVoid = true;
        boolean limited = false;
        boolean allDefault;
        long singleData;
        
        
        short depth;
        short height;
        int count = 0;
        int i;
        int ii;
        
        //We collect the indexes of the data, ordered by the depth
        int dataIndex = 0;
        int x;
        int z;
        int y;
        for (x = mergeFromX; x <= mergeToX; x++)
        {
            for (z = mergeFromZ; z <= mergeToZ; z++)
            {
                if (x == mergeFromX && z == mergeFromZ)
                {
                    for (y = 0; y < inputVerticalSize; y++)
                    {
                        dataIndex = x * inputSectionSize * inputVerticalSize + z * inputVerticalSize + y;
                        singleData = lowerDataContainer.dataContainer[dataIndex];
                        if (DataPointUtil.doesItExist(singleData))
                        {
                            //genMode = Math.min(genMode, getGenerationMode(singleData));
                            allEmpty = false;
                            if (!DataPointUtil.isVoid(singleData))
                            {
                                allVoid = false;
                                count++;
                                heightAndDepth[dataIndex * 2] = DataPointUtil.getHeight(singleData);
                                heightAndDepth[dataIndex * 2 + 1] = DataPointUtil.getDepth(singleData);
                            }
                        }
                        else
                            break;
                    }
                }
                else
                {
                    for (y = 0; y < inputVerticalSize; y++)
                    {
                        dataIndex = x * inputSectionSize * inputVerticalSize + z * inputVerticalSize + y;
                        singleData = lowerDataContainer.dataContainer[dataIndex];
                        if (DataPointUtil.doesItExist(singleData))
                        {
                            //genMode = Math.min(genMode, getGenerationMode(singleData));
                            allEmpty = false;
                            if (!DataPointUtil.isVoid(singleData))
                            {
                                allVoid = false;
                                depth = DataPointUtil.getDepth(singleData);
                                height = DataPointUtil.getHeight(singleData);
                        
                                int botPos = -1;
                                int topPos = -1;
                                //values fall in between and possibly require extension of array
                                boolean botExtend = false;
                                boolean topExtend = false;
                                for (i = 0; i < count; i++)
                                {
                                    if (depth < heightAndDepth[i * 2] && depth >= heightAndDepth[i * 2 + 1])
                                    {
                                        botPos = i;
                                        break;
                                    }
                                    else if (depth < heightAndDepth[i * 2 + 1] && ((i + 1 < count && depth >= heightAndDepth[(i + 1) * 2]) || i + 1 == count))
                                    {
                                        botPos = i;
                                        botExtend = true;
                                        break;
                                    }
                                }
                                for (i = 0; i < count; i++)
                                {
                                    if (height <= heightAndDepth[i * 2] && height > heightAndDepth[i * 2 + 1])
                                    {
                                        topPos = i;
                                        break;
                                    }
                                    else if (height <= heightAndDepth[i * 2 + 1] && ((i + 1 < count && height > heightAndDepth[(i + 1) * 2]) || i + 1 == count))
                                    {
                                        topPos = i;
                                        topExtend = true;
                                        break;
                                    }
                                }
                                if (topPos == -1)
                                {
                                    if (botPos == -1)
                                    {
                                        //whole block falls above
                                        DataPointUtil.extendArray(heightAndDepth, 2, 0, 1, count);
                                        heightAndDepth[0] = height;
                                        heightAndDepth[1] = depth;
                                        count++;
                                    }
                                    else if (!botExtend)
                                    {
                                        //only top falls above extending it there, while bottom is inside existing
                                        DataPointUtil.shrinkArray(heightAndDepth, 2, 0, botPos, count);
                                        heightAndDepth[0] = height;
                                        count -= botPos;
                                    }
                                    else
                                    {
                                        //top falls between some blocks, extending those as well
                                        DataPointUtil.shrinkArray(heightAndDepth, 2, 0, botPos, count);
                                        heightAndDepth[0] = height;
                                        heightAndDepth[1] = depth;
                                        count -= botPos;
                                    }
                                }
                                else if (!topExtend)
                                {
                                    if (!botExtend)
                                        //both top and bottom are within some exiting blocks, possibly merging them
                                        heightAndDepth[topPos * 2 + 1] = heightAndDepth[botPos * 2 + 1];
                                    else
                                        //top falls between some blocks, extending it there
                                        heightAndDepth[topPos * 2 + 1] = depth;
                                    DataPointUtil.shrinkArray(heightAndDepth, 2, topPos + 1, botPos - topPos, count);
                                    count -= botPos - topPos;
                                }
                                else
                                {
                                    if (!botExtend)
                                    {
                                        //only top is within some exiting block, extending it
                                        topPos++; //to make it easier
                                        heightAndDepth[topPos * 2] = height;
                                        heightAndDepth[topPos * 2 + 1] = heightAndDepth[botPos * 2 + 1];
                                        DataPointUtil.shrinkArray(heightAndDepth, 2, topPos + 1, botPos - topPos, count);
                                        count -= botPos - topPos;
                                    }
                                    else
                                    {
                                        //both top and bottom are outside existing blocks
                                        DataPointUtil.shrinkArray(heightAndDepth, 2, topPos + 1, botPos - topPos, count);
                                        count -= botPos - topPos;
                                        DataPointUtil.extendArray(heightAndDepth, 2, topPos + 1, 1, count);
                                        count++;
                                        heightAndDepth[topPos * 2 + 2] = height;
                                        heightAndDepth[topPos * 2 + 3] = depth;
                                    }
                                }
                            }
                        }
                        else
                            break;
                    }
                }
            }
        }
        //We check if there is any data that's not empty or void
        if (allEmpty)
            return;
        if (allVoid)
        {
            dataContainer[outBaseIndex] = DataPointUtil.createVoidDataPoint(genMode);
            return;
        }
        
        //we limit the vertical portion to maxVerticalData
        int j = 0;
        while (count > verticalSize)
        {
            limited = true;
            ii = DataPointUtil.MAX_WORLD_Y_SIZE;
            for (i = 0; i < count - 1; i++)
            {
                if (heightAndDepth[i * 2 + 1] - heightAndDepth[(i + 1) * 2] <= ii)
                {
                    ii = heightAndDepth[i * 2 + 1] - heightAndDepth[(i + 1) * 2];
                    j = i;
                }
            }
            heightAndDepth[j * 2 + 1] = heightAndDepth[(j + 1) * 2 + 1];
            for (i = j + 1; i < count - 1; i++)
            {
                heightAndDepth[i * 2] = heightAndDepth[(i + 1) * 2];
                heightAndDepth[i * 2 + 1] = heightAndDepth[(i + 1) * 2 + 1];
            }
            //System.arraycopy(heightAndDepth, j + 1, heightAndDepth, j, count - j - 1);
            count--;
        }
        int yOut;
        //As standard the vertical lods are ordered from top to bottom
        if (!limited && xSize*zSize == 1)
        {
            for (yOut = 0; yOut < count; yOut++)
                dataIndex = mergeFromX*inputSectionSize*inputVerticalSize + mergeFromZ*inputVerticalSize + yOut;
                dataContainer[outBaseIndex + yOut] = lowerDataContainer.dataContainer[dataIndex];
        }
        else
        {
            
            //We want to efficiently memorize indexes
            int[] dataIndexesCache = tDataIndexCache.get();
            if (dataIndexesCache==null || dataIndexesCache.length != xSize*zSize) {
                dataIndexesCache = new int[xSize*zSize];
                tDataIndexCache.set(dataIndexesCache);
            }
            Arrays.fill(dataIndexesCache,0);
            
            //For each lod height-depth value we have found we now want to generate the rest of the data
            //by merging all lods at lower level that are contained inside the new ones
            for (yOut = 0; yOut < count; yOut++)
            {
                //We firstly collect height and depth data
                //this will be added to each realtive long DataPoint
                height = heightAndDepth[yOut * 2];
                depth = heightAndDepth[yOut * 2 + 1];
                
                //if both height and depth are at 0 then we finished
                if ((depth == 0 && height == 0) || yOut >= heightAndDepth.length / 2)
                    break;
                
                //We initialize data useful for the merge
                int numberOfChildren = 0;
                allEmpty = true;
                allVoid = true;
                
                //We initialize all the new values that we are going to put in the dataPoint
                int tempAlpha = 0;
                int tempRed = 0;
                int tempGreen = 0;
                int tempBlue = 0;
                int tempLightBlock = 0;
                int tempLightSky = 0;
                long data = 0;
                
                int index;
                
                //For each position that we want to merge
                for(x = mergeFromX; x <= mergeToX; x++)
                {
                    for (z = mergeFromZ; z <= mergeToZ; z++)
                    {
                        index = x * xSize + z;
                        //we scan the lods in the position from top to bottom
                        while (dataIndexesCache[index] < inputVerticalSize)
                        {
                            y = dataIndexesCache[index];
                            dataIndex = x * inputSectionSize * inputVerticalSize + z * inputVerticalSize + y;
        
                            singleData = lowerDataContainer.dataContainer[dataIndex];
                            if (DataPointUtil.doesItExist(singleData) && !DataPointUtil.isVoid(singleData))
                            {
                                dataIndexesCache[index]++;
                                if ((depth <= DataPointUtil.getDepth(singleData) && DataPointUtil.getDepth(singleData) < height)
                                            || (depth < DataPointUtil.getHeight(singleData) && DataPointUtil.getHeight(singleData) <= height))
                                {
                                    data = singleData;
                                    break;
                                }
                            }
                            else
                                break;
                        }
                        if (!DataPointUtil.doesItExist(data))
                        {
                            data = DataPointUtil.createVoidDataPoint(genMode);
                        }
    
                        if (DataPointUtil.doesItExist(data))
                        {
                            allEmpty = false;
                            if (!DataPointUtil.isVoid(data))
                            {
                                numberOfChildren++;
                                allVoid = false;
                                tempAlpha = Math.max(DataPointUtil.getAlpha(data), tempAlpha);
                                tempRed += DataPointUtil.getRed(data) * DataPointUtil.getRed(data);
                                tempGreen += DataPointUtil.getGreen(data) * DataPointUtil.getGreen(data);
                                tempBlue += DataPointUtil.getBlue(data) * DataPointUtil.getBlue(data);
                                tempLightBlock += DataPointUtil.getLightBlock(data);
                                tempLightSky += DataPointUtil.getLightSky(data);
                            }
                        }
                    }
                }
                
                if (allEmpty)
                    //no child has been initialized
                    dataContainer[outBaseIndex + yOut] = DataPointUtil.EMPTY_DATA;
                else if (allVoid)
                    //all the children are void
                    dataContainer[outBaseIndex + yOut] = DataPointUtil.createVoidDataPoint(genMode);
                else
                {
                    //we have at least 1 child
                    if (xSize*zSize != 1)
                    {
                        tempRed = tempRed / numberOfChildren;
                        tempGreen = tempGreen / numberOfChildren;
                        tempBlue = tempBlue / numberOfChildren;
                        tempLightBlock = tempLightBlock / numberOfChildren;
                        tempLightSky = tempLightSky / numberOfChildren;
                    }
                    //data = createDataPoint(tempAlpha, tempRed, tempGreen, tempBlue, height, depth, tempLightSky, tempLightBlock, tempGenMode, allDefault);
                    //if (j > 0 && getColor(data) == getColor(dataPoint[j]))
                    //{
                    //	add simplification at the end due to color
                    //}
                    dataContainer[outBaseIndex + yOut] = DataPointUtil.createDataPoint((int) Math.sqrt(tempAlpha), (int) Math.sqrt(tempRed), (int) Math.sqrt(tempGreen), (int) Math.sqrt(tempBlue), height, depth, tempLightSky, tempLightBlock, genMode);
                }
            }
        }
    }
    
}
