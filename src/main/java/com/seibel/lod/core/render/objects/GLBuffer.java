package com.seibel.lod.core.render.objects;

import com.seibel.lod.core.enums.config.EGpuUploadMethod;
import com.seibel.lod.core.enums.rendering.EGLProxyContext;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.render.GLProxy;
import com.seibel.lod.core.util.UnitBytes;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL44;

import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

public class GLBuffer implements AutoCloseable
{
    private static final Logger LOGGER = DhLoggerBuilder.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());
    
    public static final double BUFFER_EXPANSION_MULTIPLIER = 1.3;
    public static final double BUFFER_SHRINK_TRIGGER = BUFFER_EXPANSION_MULTIPLIER * BUFFER_EXPANSION_MULTIPLIER;
    public static AtomicInteger count = new AtomicInteger(0);
    
    protected int id;
    public final int getId() {
        return id;
    }
    protected int size = 0;
    public int getSize() {
        return size;
    }
    protected boolean bufferStorage;
    public final boolean isBufferStorage() {
        return bufferStorage;
    }
    protected boolean isMapped = false;

    
    public GLBuffer(boolean isBufferStorage)
    {
        create(isBufferStorage);
    }

    
    // Should be override by subclasses
    public int getBufferBindingTarget() {
        return GL32.GL_COPY_READ_BUFFER;
    }

    public void bind() {
        GL32.glBindBuffer(getBufferBindingTarget(), id);
    }
    public void unbind() {
        GL32.glBindBuffer(getBufferBindingTarget(), 0);
    }

    protected void create(boolean asBufferStorage) {
        if (GLProxy.getInstance().getGlContext() == EGLProxyContext.NONE)
            throw new IllegalStateException("Thread [" +Thread.currentThread().getName() + "] tried to create a GLBuffer outside a OpenGL context.");
        this.id = GL32.glGenBuffers();
        this.bufferStorage = asBufferStorage;
        count.getAndIncrement();
    }

    //DEBUG USE
    //private StackTraceElement[] firstCloseCallStack = null;
    protected void destroy(boolean async) {
        if (this.id == 0) {
            //ApiShared.LOGGER.warn("Buffer double close! First close call stack: {}", Arrays.toString(firstCloseCallStack));
            throw new IllegalStateException("Buffer double close!");
        }
        if (async && GLProxy.getInstance().getGlContext() != EGLProxyContext.PROXY_WORKER) {
            GLProxy.getInstance().recordOpenGlCall(() -> destroy((false)));
        } else {
            GL32.glDeleteBuffers(id);
            //firstCloseCallStack = Thread.currentThread().getStackTrace();
            id = 0;
            size = 0;
            if (count.decrementAndGet()==0)
                LOGGER.info("All GLBuffer is freed.");
        }
    }

    // Requires already binded
    protected void uploadBufferStorage(ByteBuffer bb, int bufferStorageHint) {
        if (!bufferStorage) throw new IllegalStateException("Buffer is not bufferStorage but its trying to use bufferStorage upload method!");
        int bbSize = bb.limit() - bb.position();
        destroy(false);
        create(true);
        bind();
        GL44.glBufferStorage(getBufferBindingTarget(), bb, bufferStorageHint);
        size = bbSize;
    }

    // Requires already binded
    protected void uploadBufferData(ByteBuffer bb, int bufferDataHint) {
        if (bufferStorage) throw new IllegalStateException("Buffer is bufferStorage but its trying to use Data upload method!");
        int bbSize = bb.limit() - bb.position();
        GL32.glBufferData(getBufferBindingTarget(), bb, bufferDataHint);
        size = bbSize;
    }

    // Requires already binded
    protected void uploadSubData(ByteBuffer bb, int maxExpansionSize, int bufferDataHint) {
        if (bufferStorage) throw new IllegalStateException("Buffer is bufferStorage but its trying to use SubData upload method!");
        int bbSize = bb.limit() - bb.position();
        if (size < bbSize || size > bbSize * BUFFER_SHRINK_TRIGGER) {
            int newSize = (int) (bbSize * BUFFER_EXPANSION_MULTIPLIER);
            if (newSize > maxExpansionSize) newSize = maxExpansionSize;
            GL32.glBufferData(getBufferBindingTarget(), newSize, bufferDataHint);
            size = newSize;
        }
        GL32.glBufferSubData(getBufferBindingTarget(), 0, bb);
    }

    // Requires already binded
    public void uploadBuffer(ByteBuffer bb, EGpuUploadMethod uploadMethod, int maxExpansionSize, int bufferHint) {
        if (uploadMethod.useEarlyMapping)
            throw new IllegalArgumentException("UploadMethod signal that this should use Mapping instead of uploadBuffer!");
        int bbSize = bb.limit()-bb.position();
        if (bbSize > maxExpansionSize)
            throw new IllegalArgumentException("maxExpansionSize is "+maxExpansionSize+" but buffer size is "+bbSize+"!");
        GLProxy.GL_LOGGER.debug("Uploading buffer with {}.", new UnitBytes(bbSize));
        // If size is zero, just ignore it.
        if (bbSize == 0) return;
        boolean useBuffStorage = uploadMethod.useBufferStorage;
        if (useBuffStorage != bufferStorage) {
            destroy(false);
            create(useBuffStorage);
            bind();
        }
        switch (uploadMethod) {
            case AUTO:
                throw new IllegalArgumentException("GpuUploadMethod AUTO must be resolved before call to uploadBuffer()!");
            case BUFFER_STORAGE:
                uploadBufferStorage(bb, bufferHint);
                break;
            case DATA:
                uploadBufferData(bb, bufferHint);
                break;
            case SUB_DATA:
                uploadSubData(bb, maxExpansionSize, bufferHint);
                break;
            default:
                throw new IllegalArgumentException("Invalid GpuUploadMethod enum");
        }
    }

    public ByteBuffer mapBuffer(int targetSize, EGpuUploadMethod uploadMethod, int maxExpensionSize, int bufferHint, int mapFlags) {
        if (targetSize == 0) throw new IllegalArgumentException("MapBuffer targetSize is 0!");
        if (!uploadMethod.useEarlyMapping) throw new IllegalStateException("Upload method must be one that use mappings in order to call mapBuffer!");
        if (isMapped) throw new IllegalStateException("Map Buffer called but buffer is already mapped!");
        boolean useBuffStorage = uploadMethod.useBufferStorage;
        if (useBuffStorage != bufferStorage) {
            destroy(false);
            create(useBuffStorage);
        }
        bind();
        ByteBuffer vboBuffer;

        if (size < targetSize || size > targetSize * BUFFER_SHRINK_TRIGGER) {
            int newSize = (int) (targetSize * BUFFER_EXPANSION_MULTIPLIER);
            if (newSize > maxExpensionSize) newSize = maxExpensionSize;
            size = newSize;
            if (bufferStorage) {
                GL32.glDeleteBuffers(id);
                id = GL32.glGenBuffers();
                GL32.glBindBuffer(getBufferBindingTarget(), id);
                GL32.glBindBuffer(getBufferBindingTarget(), id);
                GL44.glBufferStorage(getBufferBindingTarget(), newSize, bufferHint);
            } else {
                GL32.glBufferData(GL32.GL_ARRAY_BUFFER, newSize, bufferHint);
            }
        }

        vboBuffer = GL32.glMapBufferRange(GL32.GL_ARRAY_BUFFER, 0, targetSize, mapFlags);
        isMapped = true;
        return vboBuffer;
    }

    // Requires already binded
    public void unmapBuffer()
    {
        if (!isMapped) throw new IllegalStateException("Unmap Buffer called but buffer is already not mapped!");
        bind();
        GL32.glUnmapBuffer(getBufferBindingTarget());
        isMapped = false;
    }

    @Override
    public void close()
    {
        destroy(true);
    }

    @Override
    public String toString() {
        return (bufferStorage ? "" : "Static-")+ getClass().getSimpleName() +
                "[id:"+id+",size:"+size+(isMapped?",MAPPED" : "")+"]";
    }
}
