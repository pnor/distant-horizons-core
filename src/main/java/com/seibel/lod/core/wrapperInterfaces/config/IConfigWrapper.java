package com.seibel.lod.core.wrapperInterfaces.config;

import com.seibel.lod.core.handlers.dependencyInjection.IBindable;

public interface IConfigWrapper extends IBindable {

    boolean langExists(String str);

    String getLang(String str);
}
