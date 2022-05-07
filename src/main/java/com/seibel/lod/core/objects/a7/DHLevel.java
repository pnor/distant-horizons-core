package com.seibel.lod.core.objects.a7;

import com.seibel.lod.core.handlers.dependencyInjection.SingletonHandler;
import com.seibel.lod.core.objects.a7.data.DataHandler;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;

import java.io.File;

public class DHLevel {
    private static final ILodConfigWrapperSingleton CONFIG = SingletonHandler.get(ILodConfigWrapperSingleton.class);
    private static final IMinecraftClientWrapper MC = SingletonHandler.get(IMinecraftClientWrapper.class);
    public final File saveFolder; // Could be null, for no saving
    public final DataHandler dataHandler; // Could be null, for no saving
    public LodQuadTree lodQuadTree;
    public DHLevel(File saveFolder) {
        this.saveFolder = saveFolder;
        lodQuadTree = new LodQuadTree(
                CONFIG.client().graphics().quality().getLodChunkRenderDistance()*16,
                MC.getPlayerBlockPos().x,
                MC.getPlayerBlockPos().z
        );
        if (saveFolder != null) {
            dataHandler = new DataHandler(saveFolder);
        } else {
            dataHandler = null;
        }
    }


}
