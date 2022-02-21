package com.seibel.lod.core.wrapperInterfaces.block;

import com.seibel.lod.core.enums.config.BlocksToAvoid;
import com.seibel.lod.core.util.ColorUtil;

public class BlockDetail
{
	public final int color;
	public final boolean isFullBlock;
	public final boolean hasNoCollision;
	public final boolean hasOnlyNonFullFace;
	
	public BlockDetail(int c, boolean full, boolean noCol, boolean nonFullFace) {
		color = c;
		isFullBlock = full;
		hasNoCollision = noCol;
		hasOnlyNonFullFace = nonFullFace;
	}
	
	public boolean shouldRender(BlocksToAvoid mode) {
		return !((mode.noCollision && hasNoCollision) || (mode.nonFull && hasOnlyNonFullFace));
	}
	
	public String toString() {
		return "[color: "+ColorUtil.toString(color)+", isFullBlock: "+isFullBlock
				+", hasNoCollision: "+hasNoCollision+", hasOnlyNonFullFace: "+hasOnlyNonFullFace+"]";
	}
}
