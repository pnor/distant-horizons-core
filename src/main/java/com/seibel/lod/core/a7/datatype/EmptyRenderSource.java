package com.seibel.lod.core.a7.datatype;

import com.seibel.lod.core.a7.datatype.full.ChunkSizedData;
import com.seibel.lod.core.a7.level.IClientLevel;
import com.seibel.lod.core.a7.render.LodQuadTree;
import com.seibel.lod.core.a7.render.RenderBuffer;
import com.seibel.lod.core.a7.save.io.render.RenderMetaFile;
import com.seibel.lod.core.objects.DHChunkPos;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicReference;

public class EmptyRenderSource implements LodRenderSource {
    public static final EmptyRenderSource INSTANCE = new EmptyRenderSource();

    @Override
    public void enableRender(LodQuadTree quadTree) {
    }

    @Override
    public void disableRender() {
    }

    @Override
    public boolean isRenderReady() {
        return false;
    }

    @Override
    public void dispose() {
    }

    @Override
    public byte getDetailOffset() {
        return 0;
    }

    @Override
    public boolean trySwapRenderBuffer(LodQuadTree quadTree, AtomicReference<RenderBuffer> referenceSlot) {
        return false;
    }

    @Override
    public void saveRender(IClientLevel level, RenderMetaFile file, OutputStream dataStream) throws IOException {
        throw new UnsupportedOperationException("EmptyRenderSource should NEVER be saved!");
    }

    @Override
    public void update(DHChunkPos chunkPos, ChunkSizedData chunkData) {

    }
}
