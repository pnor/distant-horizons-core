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

public class RenderDataContaioner
{
    public static final boolean DO_SAFETY_CHECKS = true;
    
    private final short minHeight;
    public final byte detailLevel;
    public final int SECTION_SIZE = 128;
    public final int verticalSize;
    
    public final long[] dataContainer;
    
    public RenderDataContaioner(byte detailLevel)
    {
        this.detailLevel = detailLevel;
        verticalSize = DetailDistanceUtil.getMaxVerticalData(detailLevel);
        dataContainer = new long[SECTION_SIZE * SECTION_SIZE * DetailDistanceUtil.getMaxVerticalData(detailLevel)];
        minHeight = SingletonHandler.get(IMinecraftClientWrapper.class).getWrappedClientWorld().getMinHeight();
    }
    
    public byte getDetailLevel()
    {
        return detailLevel;
    }
    
    public void clear(int posX, int posZ)
    {
        for (int verticalIndex = 0; verticalIndex < verticalSize; verticalIndex++)
            dataContainer[posX * SECTION_SIZE * verticalSize + posZ * verticalSize + verticalIndex] = DataPointUtil.EMPTY_DATA;
    }
    
    public boolean addData(long data, int posX, int posZ, int verticalIndex)
    {
        dataContainer[posX * SECTION_SIZE * verticalSize + posZ * verticalSize + verticalIndex] = data;
        return true;
    }
    
    private void forceWriteVerticalData(long[] data, int posX, int posZ)
    {
        int index = posX * SECTION_SIZE * verticalSize + posZ * verticalSize;
        if (verticalSize >= 0) System.arraycopy(data, 0, dataContainer, index + 0, verticalSize);
    }
    
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
    
    public RenderDataContaioner(DataInputStream inputData, int version, byte expectedDetailLevel) throws IOException {
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
    }
    
    public LevelContainer expand()
    {
        return new VerticalLevelContainer((byte) (getDetailLevel() - 1));
    }
    
    private static final ThreadLocal<long[][]> tLocalVerticalUpdateArrays = ThreadLocal.withInitial(() ->
    {
        return new long[LodUtil.DETAIL_OPTIONS - 1][];
    });
    
    public void updateData(LevelContainer lowerLevelContainer, int posX, int posZ)
    {
        //We reset the array
        long[][] verticalUpdateArrays = tLocalVerticalUpdateArrays.get();
        long[] dataToMerge = verticalUpdateArrays[detailLevel-1];
        int arrayLength = DetailDistanceUtil.getMaxVerticalData(detailLevel-1) * 4;
        if (dataToMerge == null || dataToMerge.length != arrayLength) {
            dataToMerge = new long[arrayLength];
            verticalUpdateArrays[detailLevel-1] = dataToMerge;
        } else Arrays.fill(dataToMerge, 0);
        
        int lowerMaxVertical = dataToMerge.length / 4;
        int childPosX;
        int childPosZ;
        long[] data;
        boolean anyDataExist = false;
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
        data = DataPointUtil.mergeMultiData(dataToMerge, lowerMaxVertical, getVerticalSize());
        if (!anyDataExist)
            throw new RuntimeException("Update data called but no child datapoint exist!");
        
        if ((!DataPointUtil.doesItExist(data[0])) && anyDataExist)
            throw new RuntimeException("Update data called but higher level datapoint doesn't exist even though child data does exist!");
        
        //FIXME: Disabled check if genMode for old data is already invalid due to having genMode 0.
        if (DataPointUtil.getGenerationMode(data[0]) != DataPointUtil.getGenerationMode(lowerLevelContainer.getSingleData(posX*2, posZ*2)))
            throw new RuntimeException("Update data called but higher level datapoint does not have the same GenerationMode as the top left corner child datapoint!");
        
        forceWriteVerticalData(data, posX, posZ);
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
    
    
}
