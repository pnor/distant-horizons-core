package com.seibel.lod.core.wrapperInterfaces.block;

import com.seibel.lod.core.enums.LodDirection;
import com.seibel.lod.core.enums.config.BlocksToAvoid;
import com.seibel.lod.core.wrapperInterfaces.chunk.IChunkWrapper;


public abstract class IBlockDetailWrapper
{
	// Note: ALL value should be lazily-calculated
	
	// Note: This should be lazily-calculated if block needs tinting to be resolved
	public abstract int getAndResolveFaceColor(LodDirection dir, IChunkWrapper chunk, AbstractBlockPosWrapper blockPos);
	public abstract boolean hasFaceCullingFor(LodDirection dir);
	public abstract boolean hasNoCollision();
	public abstract boolean noFaceIsFullFace();
	
	public abstract String serialize();
	
	protected abstract boolean isSame(IBlockDetailWrapper iBlockDetail);
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof IBlockDetailWrapper)) return false;
		return isSame((IBlockDetailWrapper)o);
	}
	
	@Override
	public String toString() {
		return serialize();
	}
	
	public boolean shouldRender(BlocksToAvoid mode)
	{
		return !((mode.noCollision && hasNoCollision()) || (mode.nonFull && noFaceIsFullFace()));
	}
	
	
}
