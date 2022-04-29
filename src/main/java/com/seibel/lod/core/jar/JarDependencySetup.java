package com.seibel.lod.core.jar;

import com.seibel.lod.core.jar.wrapperInterfaces.config.ConfigWrapper;
import com.seibel.lod.core.util.SingletonHandler;
import com.seibel.lod.core.wrapperInterfaces.config.IConfigWrapper;

public class JarDependencySetup {
    public static void createInitialBindings() {
        SingletonHandler.bind(IConfigWrapper.class, ConfigWrapper.INSTANCE);
        ConfigWrapper.init();
    }
}
