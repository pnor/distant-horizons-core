package com.seibel.lod.core.objects.a7;

import com.seibel.lod.core.objects.a7.io.DHFolderHandler;
import com.seibel.lod.core.objects.a7.io.LevelToFileMatcher;
import com.seibel.lod.core.wrapperInterfaces.block.IBlockDetailWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IBiomeWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IWorldWrapper;

import java.io.File;
import java.util.HashMap;

public class DhChunk
{
    public int CHUNK_SIZE = 16;
    public IBlockDetailWrapper[] blockData;
    public IBiomeWrapper[] biomeData;
    public int[] verticalData;
    public int verticalSize;
    public int chunkPosX;
    public int chunkPosZ;
    public byte generationMode;
    
    public DhChunk(byte generationMode, int chunkPosX, int chunkPosZ, int verticalSize)
    {
        this.verticalSize = verticalSize;
        this.chunkPosX = chunkPosX;
        this.chunkPosZ = chunkPosZ;
        this.generationMode = generationMode;
    
        this.blockData = new IBlockDetailWrapper[CHUNK_SIZE*CHUNK_SIZE*verticalSize];
        this.biomeData = new IBiomeWrapper[CHUNK_SIZE*CHUNK_SIZE*verticalSize];
        this.verticalData = new int[CHUNK_SIZE*CHUNK_SIZE*verticalSize];
    }
    
    public void addData(IBlockDetailWrapper block, IBiomeWrapper biome, int singelVerticalData, int relPosX, int relPosZ, int verticalIndex)
    {
        int index = CHUNK_SIZE*verticalSize*relPosX + verticalSize*relPosZ + verticalIndex;
        blockData[index] = block;
        biomeData[index] = biome;
        verticalData[index] = singelVerticalData;
    }
    
    public long[] getData()
    {
        long[] data = new long[3];
        //long data = something created using block biome and singelVerticalData;
        return data;
    }
}
