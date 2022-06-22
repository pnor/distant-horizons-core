package com.seibel.lod.core.api.internal.a7;

import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.a7.world.DhWorld;
import com.seibel.lod.core.a7.Server;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftSharedWrapper;
import org.apache.logging.log4j.Logger;

public class SharedApi {
    public static DhWorld currentWorld;
    public static Server currentServer;
    public static IMinecraftSharedWrapper MC;
    public static Logger LOGGER = DhLoggerBuilder.getLogger("DH Events");
    public static boolean inDedicatedEnvironment;
}
