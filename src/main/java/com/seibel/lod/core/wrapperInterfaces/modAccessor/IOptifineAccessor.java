package com.seibel.lod.core.wrapperInterfaces.modAccessor;

import java.util.HashSet;
import java.util.Optional;

import com.seibel.lod.core.wrapperInterfaces.chunk.AbstractChunkPosWrapper;

public interface IOptifineAccessor extends IModAccessor 
{
	/** Can be null */
	HashSet<AbstractChunkPosWrapper> getNormalRenderedChunks();
}
