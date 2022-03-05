package com.seibel.lod.core.wrapperInterfaces.modAccessor;

import com.seibel.lod.core.handlers.dependencyInjection.IBindable;

/**
 * Checks if a mod is loaded
 *
 * @author coolGi2007
 * @version 3-5-2022
 */
public interface IModChecker extends IBindable {
    /** Checks if a mod is loaded */
    boolean isModLoaded(String modid);
}
