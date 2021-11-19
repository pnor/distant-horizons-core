package com.seibel.lod.core.wrapperAdapters.block;

import com.seibel.lod.core.enums.LodDirection;

public abstract class AbstractBlockPosWrapper
{
	public AbstractBlockPosWrapper()
	{
		
	}
	
	public AbstractBlockPosWrapper(int x, int y, int z)
	{
		
	}
	
	public abstract void set(int x, int y, int z);
	
	public abstract int getX();
	public abstract int getY();
	public abstract int getZ();
	
	public abstract int get(LodDirection.Axis axis);
	
	/** returns itself */
	public abstract AbstractBlockPosWrapper offset(int x, int y, int z);
}
