package com.seibel.lod.core.a7.level;

import com.seibel.lod.core.a7.LodQuadTree;
import com.seibel.lod.core.a7.io.file.LocalDataFileHandler;
import com.seibel.lod.core.a7.io.render.RenderFileHandler;
import com.seibel.lod.core.a7.pos.DhBlockPos2D;
import com.seibel.lod.core.a7.render.RenderBufferHandler;
import com.seibel.lod.core.a7.save.structure.LocalSaveStructure;
import com.seibel.lod.core.config.Config;
import com.seibel.lod.core.handlers.dependencyInjection.SingletonHandler;
import com.seibel.lod.core.objects.math.Mat4f;
import com.seibel.lod.core.render.a7LodRenderer;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IProfilerWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.ILevelWrapper;

import java.util.concurrent.CompletableFuture;

public class DhClientServerLevel implements IClientLevel, IServerLevel {
    private static final IMinecraftClientWrapper MC_CLIENT = SingletonHandler.get(IMinecraftClientWrapper.class);
    public final LocalSaveStructure save;
    public final LocalDataFileHandler dataFileHandler;
    public final RenderFileHandler renderFileHandler;
    public final RenderBufferHandler renderBufferHandler; //TODO: Should this be owned by renderer?
    public final ILevelWrapper level;
    public a7LodRenderer renderer = null;
    public LodQuadTree tree;

    public DhClientServerLevel(LocalSaveStructure save, ILevelWrapper level) {
        this.level = level;
        this.save = save;
        dataFileHandler = new LocalDataFileHandler(this, save.getDataFolder(level));
        renderFileHandler = new RenderFileHandler(dataFileHandler, save.getRenderCacheFolder(level));
        tree = new LodQuadTree(Config.Client.Graphics.Quality.lodChunkRenderDistance.get()*16,
                MC_CLIENT.getPlayerBlockPos().x, MC_CLIENT.getPlayerBlockPos().z, renderFileHandler);
        renderBufferHandler = new RenderBufferHandler(tree);
    }

    public void tick() {
        tree.tick(new DhBlockPos2D(MC_CLIENT.getPlayerBlockPos()));
        renderBufferHandler.update();
    }

    @Override
    public void startRenderer() {
        //TODO
    }

    @Override
    public void render(Mat4f mcModelViewMatrix, Mat4f mcProjectionMatrix, float partialTicks, IProfilerWrapper profiler) {
        if (renderer == null) {
            renderer = new a7LodRenderer(this);
        }
        renderer.drawLODs(mcModelViewMatrix, mcProjectionMatrix, partialTicks, profiler);
    }

    @Override
    public void stopRenderer() {
        renderFileHandler.flushAndSave(); //Ignore the completion feature so that this action is async
        //TODO
    }

    @Override
    public RenderBufferHandler getRenderBufferHandler() {
        return renderBufferHandler;
    }

    @Override
    public void dumpRamUsage() {
        //TODO
    }

    @Override
    public int getMinY() {
        return level.getMinHeight();
    }

    @Override
    public CompletableFuture<Void> save() {
        return renderFileHandler.flushAndSave();
        //Note: saving renderFileHandler will also save the dataFileHandler.
    }

    @Override
    public void close() {
        renderFileHandler.close();
        //Note: Closing renderFileHandler will also close the dataFileHandler.
    }
}
