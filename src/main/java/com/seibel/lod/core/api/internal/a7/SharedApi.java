package com.seibel.lod.core.api.internal.a7;

import com.seibel.lod.core.objects.a7.DHWorld;
import com.seibel.lod.core.objects.a7.Server;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftSharedWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IWorldWrapper;

public class SharedApi {
    public static DHWorld currentWorld;
    public static Server currentServer;
    public static IMinecraftSharedWrapper MC;
    public static IMinecraftClientWrapper MC_CLIENT;

    public static void onServerStart() {
        if (MC.isServerJar()) {
            ServerApi.INSTANCE.serverWorldLoadEvent();
        } else if (MC_CLIENT.hasSinglePlayerServer()) {
            ServerApi.INSTANCE.serverWorldLoadEvent();
        } // else do nothing
    }

    public static void onServerStop() {
        if (MC.isServerJar()) {
            ServerApi.INSTANCE.serverWorldUnloadEvent();
        } else if (MC_CLIENT.hasSinglePlayerServer()) {
            ServerApi.INSTANCE.serverWorldUnloadEvent();
        } // else do nothing
    }

    public static void onLevelLoad(IWorldWrapper world) {
        if (MC.isServerJar()) {
            ServerApi.INSTANCE.serverLevelLoadEvent(world);
        } else if (MC_CLIENT.hasSinglePlayerServer()) {
            ServerApi.INSTANCE.serverLevelLoadEvent(world);
        } else {
            ClientApi.INSTANCE.clientLevelLoadEvent(world);
        }
    }

    public static void onLevelUnload(IWorldWrapper world) {
        if (MC.isServerJar()) {
            ServerApi.INSTANCE.serverLevelUnloadEvent(world);
        } else if (MC_CLIENT.hasSinglePlayerServer()) {
            ServerApi.INSTANCE.serverLevelUnloadEvent(world);
        } else {
            ClientApi.INSTANCE.clientLevelUnloadEvent(world);
        }
    }

}
