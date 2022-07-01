package com.seibel.lod.core.api.internal.a7;

import com.seibel.lod.core.a7.world.WorldEnvironment;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.a7.world.DhWorld;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftSharedWrapper;
import org.apache.logging.log4j.Logger;

public class SharedApi {
    public static final Logger LOGGER = DhLoggerBuilder.getLogger("DH Events");
    public static IMinecraftSharedWrapper MC;
    public static DhWorld currentWorld;

    //TODO: Should this be in core and able to be accessed by core, or should this be in common, and only effect
    //      how common calls back into the internal APIs?
    public static boolean inDedicatedEnvironment;

    public static WorldEnvironment getEnvironment() {
        return currentWorld==null ? null : currentWorld.environment;
    }
}
