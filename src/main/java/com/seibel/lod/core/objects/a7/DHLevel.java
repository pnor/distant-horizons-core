package com.seibel.lod.core.objects.a7;

import com.seibel.lod.core.handlers.dependencyInjection.SingletonHandler;
import com.seibel.lod.core.objects.a7.data.DataFileHandler;
import com.seibel.lod.core.objects.a7.pos.DhBlockPos2D;
import com.seibel.lod.core.objects.a7.render.RenderBufferHandler;
import com.seibel.lod.core.render.LodRenderProgram;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IWorldWrapper;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

public class DHLevel extends LodQuadTree {
    private static final ILodConfigWrapperSingleton CONFIG = SingletonHandler.get(ILodConfigWrapperSingleton.class);
    private static final IMinecraftClientWrapper MC = SingletonHandler.get(IMinecraftClientWrapper.class);
    public final File saveFolder; // Could be null, for no saving
    public final DataFileHandler dataFileHandler; // Could be null, for no saving
    public final RenderBufferHandler renderBufferHandler;
    public final ExecutorService dhTickerThread = LodUtil.makeSingleThreadPool("DHLevelTickerThread", 2);
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    public final IWorldWrapper level;

    public DHLevel(File saveFolder, IWorldWrapper level) {
        super(CONFIG.client().graphics().quality().getLodChunkRenderDistance()*16,
                MC.getPlayerBlockPos().x,
                MC.getPlayerBlockPos().z);
        this.saveFolder = saveFolder;
        if (saveFolder != null) {
            dataFileHandler = new DataFileHandler(saveFolder);
        } else {
            dataFileHandler = null;
        }
        renderBufferHandler = new RenderBufferHandler(this);
        this.level = level;
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
    public RenderDataSource getRenderDataSource() {
        return dataFileHandler;
    }

    public void render(LodRenderProgram renderContext) {
        renderBufferHandler.render(renderContext);
    }
}
