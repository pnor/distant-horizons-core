package com.seibel.lod.core.a7.save.structure;

import com.seibel.lod.core.handlers.dependencyInjection.SingletonHandler;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftRenderWrapper;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftSharedWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.ILevelWrapper;

import java.io.File;

public class LocalSaveStructure extends SaveStructure {
    private static final IMinecraftSharedWrapper MC = SingletonHandler.get(IMinecraftSharedWrapper.class);

    private final File folder;

    // Fit for Client_Server & Server_Only environment
    public LocalSaveStructure() {
        folder = MC.getInstallationDirectory();
    }

    @Override
    public File getRenderCacheFolder(ILevelWrapper world) {
        return null;
    }

    @Override
    public File getDataFolder(ILevelWrapper world) {
        return null;
    }
}
