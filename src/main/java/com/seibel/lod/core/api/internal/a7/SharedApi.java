package com.seibel.lod.core.api.internal.a7;

import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.objects.a7.DHWorld;
import com.seibel.lod.core.objects.a7.Server;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftSharedWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IWorldWrapper;
import org.apache.logging.log4j.Logger;

public class SharedApi {
    public static DHWorld currentWorld;
    public static Server currentServer;
    public static IMinecraftSharedWrapper MC;
    public static Logger LOGGER = DhLoggerBuilder.getLogger("DH Events");
    public static boolean inDedicatedEnvironment;
}
