package com.seibel.lod.wrappers;

import com.seibel.lod.core.wrapperAdapters.IWrapperFactory;
import com.seibel.lod.core.wrapperAdapters.block.AbstractBlockPosWrapper;
import com.seibel.lod.wrappers.block.BlockPosWrapper;

/**
 * 
 * @author James Seibel
 * @version 11-18-2021
 */
public class WrapperFactory implements IWrapperFactory
{
	public static final WrapperFactory INSTANCE = new WrapperFactory();
	
	
	@Override
	public AbstractBlockPosWrapper createBlockPos()
	{
		return new BlockPosWrapper();
	}
	
	@Override
	public AbstractBlockPosWrapper createBlockPos(int x, int y, int z)
	{
		return new BlockPosWrapper(x,y,z);
	}
	
}
