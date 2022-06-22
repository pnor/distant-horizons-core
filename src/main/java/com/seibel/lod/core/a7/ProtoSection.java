package com.seibel.lod.core.a7;

import com.seibel.lod.core.wrapperInterfaces.block.IBlockDetailWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IBiomeWrapper;

public class ProtoSection
{
	public int CHUNK_BLOCK_SIZE = LodSection.SUB_REGION_DATA_WIDTH;
	/**TODO make biome resolution a costant somewhere*/
	public int CHUNK_BIOME_SIZE = LodSection.SUB_REGION_DATA_WIDTH/4;
	
	public int blockVerticalSize;
	public int biomeVerticalSize;
	
	
	public IBlockDetailWrapper[] blockData;
	public IBiomeWrapper[] biomeData;
	
	public int[] blockVerticalData;
	public int[] biomeVerticalData;
}
