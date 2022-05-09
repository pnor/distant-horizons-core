package com.seibel.lod.core.objects.a7;

import com.seibel.lod.core.handlers.dependencyInjection.SingletonHandler;
import com.seibel.lod.core.objects.a7.data.DataHandler;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;

import java.io.File;

public class DHLevel extends LodQuadTree {
    private static final ILodConfigWrapperSingleton CONFIG = SingletonHandler.get(ILodConfigWrapperSingleton.class);
    private static final IMinecraftClientWrapper MC = SingletonHandler.get(IMinecraftClientWrapper.class);
    public final File saveFolder; // Could be null, for no saving
    public final DataHandler dataHandler; // Could be null, for no saving
    public DHLevel(File saveFolder) {
        super(CONFIG.client().graphics().quality().getLodChunkRenderDistance()*16,
                MC.getPlayerBlockPos().x,
                MC.getPlayerBlockPos().z);
        this.saveFolder = saveFolder;
        if (saveFolder != null) {
            dataHandler = new DataHandler(saveFolder);
        } else {
            dataHandler = null;
        }
    }


    @Override
    public RenderDataSource getRenderDataSource() {
        return dataHandler;
    }
}
