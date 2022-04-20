package com.seibel.lod.core.config.gui;

/**
 * The base for all screens
 *
 * @author coolGi
 */
public abstract class AbstractScreen {
    public int width;
    public int height;
    public int mouseX = 0;
    public int mouseY = 0;
    /** Weather it should close when you press the escape key */
    public boolean shouldCloseOnEsc = true;


    /** Called once when the screen is opened */
    public abstract void init();
    /** Called every frame */
    public abstract void render(float delta);
    public void tick() {}

    /** What happens when the user closes the screen*/
    public void onClose() {}
}
