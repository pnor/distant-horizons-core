package com.seibel.lod.wrappers.block;

import com.seibel.lod.core.wrapperAdapters.block.IBlockColorSingletonWrapper;
import com.seibel.lod.core.wrapperAdapters.block.IBlockColorWrapper;

import net.minecraft.block.Blocks;


/**
 * This class wraps the minecraft Block class
 * 
 * @author ??
 * @version 11-17-2021
 */
public class BlockColorSingletonWrapper implements IBlockColorSingletonWrapper
{
	public static final BlockColorSingletonWrapper INSTANCE = new BlockColorSingletonWrapper(); 
	
	/** return base color of water (grey value) */
	@Override
	public IBlockColorWrapper getWaterColor()
	{
		return BlockColorWrapper.getBlockColorWrapper(Blocks.WATER);
	}
}

