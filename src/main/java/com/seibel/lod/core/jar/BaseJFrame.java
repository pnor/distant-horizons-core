package com.seibel.lod.core.jar;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.seibel.lod.core.JarMain;
import com.seibel.lod.core.handlers.dependencyInjection.SingletonHandler;
import com.seibel.lod.core.wrapperInterfaces.config.IConfigWrapper;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;

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
            setIconImage(new FlatSVGIcon(JarMain.accessFile("icon.svg")).getImage());
        } catch (Exception e) {e.printStackTrace();}
        setSize(720, 480);
        setLocationRelativeTo(null); // Puts the window at the middle of the screen
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public BaseJFrame addExtraButtons() {
        // ========== LANGUAGE ==========
        int langBoxHeight = 25;
        int langBoxWidth = 100;

        // Creates a list with all the options in it
        List<String> langsToChoose = new ArrayList<>();
        try(
                final InputStreamReader isr = new InputStreamReader(JarMain.accessFile("assets/lod/lang"), StandardCharsets.UTF_8);
                final BufferedReader br = new BufferedReader(isr)
        ) {
            List<Object> col = Collections.unmodifiableList(new ArrayList<>(Arrays.asList(br.lines().toArray())));
            for (Object obj: col)
                langsToChoose.add(((String) obj).replaceAll("\\.json", ""));
        } catch (Exception e) {e.printStackTrace();}

        // Creates the box
        JComboBox<String> languageBox = new JComboBox(new DefaultComboBoxModel(langsToChoose.toArray()));
        languageBox.setSelectedIndex(langsToChoose.indexOf(Locale.getDefault().toString().toLowerCase()));
        languageBox.addActionListener( e -> {
            Locale.setDefault(Locale.forLanguageTag(languageBox.getSelectedItem().toString())); // Change lang on update
        } );
        // Set where it goes
        languageBox.setBounds(0, getHeight()-(langBoxHeight*2), langBoxWidth, langBoxHeight);
        // And finally add it
        add(languageBox);



        // ========== THEMING ==========
        int themeButtonSize = 25;
        JButton lightMode = null;
        JButton darkMode = null;
        // Try to set the icons for them
        try {
            lightMode = new JButton(new ImageIcon(
                    new FlatSVGIcon(JarMain.accessFile("assets/lod/textures/jar/themeLight.svg")).getImage() // Get the image
                            .getScaledInstance(themeButtonSize, themeButtonSize, Image.SCALE_DEFAULT) // Scale it to the correct size
            ));
            darkMode = new JButton(new ImageIcon(
                    new FlatSVGIcon(JarMain.accessFile("assets/lod/textures/jar/themeDark.svg")).getImage() // Get the image
                            .getScaledInstance(themeButtonSize, themeButtonSize, Image.SCALE_DEFAULT) // Scale it to the correct size
            ));
        } catch (Exception e) {e.printStackTrace();}
        // Where do the buttons go
        lightMode.setBounds(0, getHeight()-(themeButtonSize*2)-langBoxHeight, themeButtonSize, themeButtonSize);
        darkMode.setBounds(themeButtonSize, getHeight()-(themeButtonSize*2)-langBoxHeight, themeButtonSize, themeButtonSize);
        // Tell buttons what to do
        lightMode.addActionListener(e -> {
            FlatLightLaf.setup();
            FlatLightLaf.updateUI();
        });
        darkMode.addActionListener(e -> {
            FlatDarkLaf.setup();
            FlatDarkLaf.updateUI();
        });
        // Finally add the buttons
        add(lightMode);
        add(darkMode);


        return this;
    }
}
