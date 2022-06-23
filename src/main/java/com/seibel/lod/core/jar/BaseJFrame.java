package com.seibel.lod.core.jar;

import com.seibel.lod.core.JarMain;
import com.seibel.lod.core.handlers.dependencyInjection.SingletonHandler;
import com.seibel.lod.core.wrapperInterfaces.config.IConfigWrapper;

import javax.imageio.ImageIO;
import javax.swing.*;

public class BaseJFrame extends JFrame {
    public BaseJFrame() {
        init();
    }
    public BaseJFrame(boolean show, boolean resizable) {
        init();
        setVisible(show);
        setResizable(resizable);
    }

    public void init() {
        setTitle(SingletonHandler.get(IConfigWrapper.class).getLang("lod.title"));
        try {
            setIconImage(ImageIO.read(JarMain.accessFile("icon.png")));
        } catch (Exception e) {e.printStackTrace();}
        setSize(1280,720);
        setLocationRelativeTo(null); // Puts the window at the middle of the screen
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}
