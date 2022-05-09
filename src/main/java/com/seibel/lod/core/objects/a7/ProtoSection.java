package com.seibel.lod.core.objects.a7;

import com.seibel.lod.core.wrapperInterfaces.block.IBlockDetailWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IBiomeWrapper;

public class ProtoSection
{
	public int CHUNK_BLOCK_SIZE = LodSection.SUB_REGION_DATA_WIDTH;
	public int CHUNK_BIOME_SIZE = LodSection.SUB_REGION_DATA_WIDTH;
	
	public int blockVerticalSize;
	public int biomeVerticalSize;
	
	
	public IBlockDetailWrapper[] blockData;
	public IBiomeWrapper[] biomeData;
	
	public int[] blockVerticalData;
	public int[] biomeVerticalData;
}
