package com.seibel.lod.core.config.gui;

/**
 * @author coolGi2007
 */
public class ConfigScreen extends AbstractScreen {
    @Override
    public void init() {
        System.out.println("init");
    }

    @Override
    public void render(float delta) {
        System.out.println("Updated config screen with the delta of " + delta);
    }

    @Override
    public void tick() {
        System.out.println("Ticked");
    }
}
