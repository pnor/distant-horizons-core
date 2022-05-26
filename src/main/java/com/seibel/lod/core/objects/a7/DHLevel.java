package com.seibel.lod.core.objects.a7;

import com.seibel.lod.core.api.internal.InternalApiShared;
import com.seibel.lod.core.api.internal.a7.ClientApi;
import com.seibel.lod.core.handlers.dependencyInjection.SingletonHandler;
import com.seibel.lod.core.objects.a7.data.DataFileHandler;
import com.seibel.lod.core.objects.a7.pos.DhBlockPos2D;
import com.seibel.lod.core.objects.a7.render.RenderBufferHandler;
import com.seibel.lod.core.objects.math.Mat4f;
import com.seibel.lod.core.render.a7LodRenderer;
import com.seibel.lod.core.util.DetailDistanceUtil;
import com.seibel.lod.core.util.EventLoop;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IProfilerWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IWorldWrapper;

import java.io.Closeable;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

public class DHLevel extends LodQuadTree implements Closeable {
    private static final ILodConfigWrapperSingleton CONFIG = SingletonHandler.get(ILodConfigWrapperSingleton.class);
    private static final IMinecraftClientWrapper MC = SingletonHandler.get(IMinecraftClientWrapper.class);
    public final File saveFolder; // Could be null, for no saving
    public final DataFileHandler dataFileHandler; // Could be null, for no saving
    public final RenderBufferHandler renderBufferHandler;
    public final ExecutorService dhTickerThread = LodUtil.makeSingleThreadPool("DHLevelTickerThread", 2);
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    public final IWorldWrapper level;
    public a7LodRenderer renderer;
    public final DHWorld world;

    public EventLoop eventLoop;

    public DHLevel(DHWorld world, File saveFolder, IWorldWrapper level) {
        super(CONFIG.client().graphics().quality().getLodChunkRenderDistance()*16,
                MC.getPlayerBlockPos().x,
                MC.getPlayerBlockPos().z);
        this.world = world;
        this.saveFolder = saveFolder;
        if (saveFolder != null) {
            dataFileHandler = new DataFileHandler(saveFolder, this);
        } else {
            dataFileHandler = null;
        }
        renderBufferHandler = new RenderBufferHandler(this);
        this.level = level;
        eventLoop = new EventLoop(world.dhTickerThread, this::tick);
    }

    // Should be called by server tick thread, or called by render thread but only 20 times per second, or less?
    public void update() {

        if (!isRunning.getAndSet(true)) {
            dhTickerThread.submit(() -> {
                try {
                    tick();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    isRunning.set(false);
                }
            });
        }
    }

    private void tick() {
        super.tick(new DhBlockPos2D(MC.getPlayerBlockPos()));
        renderBufferHandler.update();
    }

    @Override
    public RenderDataProvider getRenderDataProvider() {
        return dataFileHandler;
    }

    public void render(Mat4f mcModelViewMatrix, Mat4f mcProjectionMatrix, float partialTicks, IProfilerWrapper profiler) {
        if (renderer == null) {
            renderer = new a7LodRenderer(this);
        }
        renderer.drawLODs(mcModelViewMatrix, mcProjectionMatrix, partialTicks, profiler);
    }

    public int getMinY() {
        return level.getMinHeight();
    }
    public void dumpRamUsage() {
        //TODO
    }
    public void asyncTick() {
        eventLoop.tick();
    }
    public void close() {
        eventLoop.halt();
        if (dataFileHandler != null) {
            dataFileHandler.close();
        }
    }
    public void saveFlush() {
        if (dataFileHandler != null) {
            dataFileHandler.save();
        }
    }
}
