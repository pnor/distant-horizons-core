package com.seibel.lod.core.wrapperAdapters;

import com.seibel.lod.core.wrapperAdapters.block.AbstractBlockPosWrapper;

/**
 * 
 * @author James Seibel
 * @version 11-18-2021
 */
public interface IWrapperFactory
{	
	public AbstractBlockPosWrapper createBlockPos();
	public AbstractBlockPosWrapper createBlockPos(int x, int y, int z);
}
