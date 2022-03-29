package com.seibel.lod.core.config.gui;

import com.seibel.lod.core.wrapperInterfaces.AbstractScreen;

/**
 * @author coolGi2007
 * @author leetom?
 */
public class ConfigScreen extends AbstractScreen {
    @Override
    public void init() {
        System.out.println("init");
    }

    @Override
    public void render(float delta) {
        System.out.println(delta);
    }
}
