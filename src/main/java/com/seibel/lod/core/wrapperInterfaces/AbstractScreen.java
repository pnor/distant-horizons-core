package com.seibel.lod.core.wrapperInterfaces;

/**
 * The bace for all screens
 *
 * @author coolGi
 */
public abstract class AbstractScreen {
    public int width;
    public int height;
    public int mouseX = 0;
    public int mouseY = 0;
    public boolean shouldCloseOnEsc = true;


    public abstract void init();
    public abstract void render(float delta);
    public void tick() {}


    public void onClose() {}
}
