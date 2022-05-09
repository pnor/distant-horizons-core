package com.seibel.lod.core.objects.a7;

import com.seibel.lod.core.wrapperInterfaces.block.IBlockDetailWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IBiomeWrapper;

public class DhChunk
{
    public int CHUNK_BLOCK_SIZE = 16;
    public int CHUNK_BIOME_SIZE = 4;
    
    public int blockVerticalSize;
    public int biomeVerticalSize;
    
    
    public IBlockDetailWrapper[] blockData;
    public IBiomeWrapper[] biomeData;
    
    public int[] blockVerticalData;
    public int[] biomeVerticalData;
    
    public int chunkPosX;
    public int chunkPosZ;
    public byte generationMode;
    
    public DhChunk(byte generationMode, int chunkPosX, int chunkPosZ, int blockVerticalSize, int biomeVerticalSize)
    {
        this.blockVerticalSize = blockVerticalSize;
        this.biomeVerticalSize = biomeVerticalSize;
        this.chunkPosX = chunkPosX;
        this.chunkPosZ = chunkPosZ;
        this.generationMode = generationMode;
    
        this.blockData = new IBlockDetailWrapper[CHUNK_BLOCK_SIZE * CHUNK_BLOCK_SIZE * blockVerticalSize];
        this.biomeData = new IBiomeWrapper[CHUNK_BLOCK_SIZE * CHUNK_BLOCK_SIZE * biomeVerticalSize];
        
        this.blockVerticalData = new int[CHUNK_BIOME_SIZE * CHUNK_BIOME_SIZE * blockVerticalSize];
        this.biomeVerticalData = new int[CHUNK_BIOME_SIZE * CHUNK_BIOME_SIZE * biomeVerticalSize];
    }
    
    public void addBlockData(IBlockDetailWrapper block, int verticalData, int relPosX, int relPosZ, int verticalIndex)
    {
        int index = CHUNK_BLOCK_SIZE * blockVerticalSize * relPosX + blockVerticalSize *relPosZ + verticalIndex;
        blockData[index] = block;
        blockVerticalData[index] = verticalData;
    }
    
    public void addBiomeData(IBiomeWrapper biome, int verticalData, int relPosX, int relPosZ, int verticalIndex)
    {
        int index = CHUNK_BIOME_SIZE * biomeVerticalSize*relPosX + biomeVerticalSize*relPosZ + verticalIndex;
        biomeData[index] = biome;
        biomeVerticalData[index] = verticalData;
    }
    
    public long[] getData()
    {
        long[] data = new long[3];
        
        return data;
    }
}
