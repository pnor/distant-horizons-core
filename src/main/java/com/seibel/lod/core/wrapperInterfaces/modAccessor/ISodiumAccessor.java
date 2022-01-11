package com.seibel.lod.core.wrapperInterfaces.modAccessor;

import java.util.HashSet;

import com.seibel.lod.core.wrapperInterfaces.chunk.AbstractChunkPosWrapper;

public interface ISodiumAccessor extends IModAccessor {
	HashSet<AbstractChunkPosWrapper> getNormalRenderedChunks();
}
