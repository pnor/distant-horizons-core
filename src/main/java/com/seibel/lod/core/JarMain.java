package com.seibel.lod.core;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.seibel.lod.core.handlers.dependencyInjection.SingletonHandler;
import com.seibel.lod.core.jar.DarkModeDetector;
import com.seibel.lod.core.jar.BaseJFrame;
import com.seibel.lod.core.jar.JarDependencySetup;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * The main class when you run the standalone jar
 *
 * @author coolGi
 */
public class JarMain {
    public static final boolean isDarkTheme = DarkModeDetector.isDarkMode();

    public static void main(String[] args) {
        // Sets up the local
        if (JarMain.accessFile("assets/lod/lang/"+Locale.getDefault().toString().toLowerCase()+".json") == null) {
            System.out.println("The language setting ["+Locale.getDefault().toString().toLowerCase()+"] isn't allowed yet. Defaulting to ["+Locale.US.toString().toLowerCase()+"].");
            Locale.setDefault(Locale.US);
        }
        // Set up the theme
        if (isDarkTheme)
            FlatDarkLaf.setup();
        else
            FlatLightLaf.setup();


        JarDependencySetup.createInitialBindings();
        SingletonHandler.finishBinding();
        System.out.println("WARNING: The standalone jar still work in progress");

        JOptionPane.showMessageDialog(null, "The GUI for the standalone jar isn't made yet\nIf you want to use the mod then put it in your mods folder", "Distant Horizons", JOptionPane.WARNING_MESSAGE);

        if (!getOperatingSystem().equals(OperatingSystem.LINUX)) {
            System.out.println("If you want the installer then please use linux for the time being.\nWindows and MacOS support will come later on");
        }

        BaseJFrame frame = new BaseJFrame(false, false).addExtraButtons();
        String[] optionsToChoose = {"Apple", "Orange", "Banana", "Pineapple"};
        JComboBox<String> jTest = new JComboBox<>(optionsToChoose);
        jTest.setBounds(400, 250, 140, 20);
        frame.add(jTest);
        jTest.addActionListener(e -> { System.out.println("test"); });

        int logoHeight = 200;
        int logoWidth = (int) ((double) logoHeight *2.21);
        JPanel pane = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                try {
                    g.drawImage(ImageIO.read(JarMain.accessFile("logo.png")), (frame.getWidth()/2)-(logoWidth/2), 0,   logoWidth, logoHeight,this);
                } catch (Exception e) {e.printStackTrace();}
            }
        };
        pane.setBounds(0, 0, pane.getWidth(), pane.getHeight());

        frame.add(pane);



        frame.validate(); // Update to add the widgets
        frame.setVisible(true); // Start the ui
    }



    public enum OperatingSystem {WINDOWS, MACOS, LINUX, NONE}
    public static OperatingSystem getOperatingSystem() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return OperatingSystem.WINDOWS;
        } else if (os.contains("mac")) {
            return OperatingSystem.MACOS;
        } else if (os.contains("nix") || os.contains("nux")) {
            return OperatingSystem.LINUX;
        } else {
            return OperatingSystem.NONE; // If you are the 0.00001% who don't use one of these 3 os's then you get light theme
        }
    }

    /** Get a file within the mods resources */
    public static InputStream accessFile(String resource) {
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        // this is the path within the jar file
        InputStream input = loader.getResourceAsStream("/resources/" + resource);
        if (input == null) {
            // this is how we load file within editor (eg eclipse)
            input = loader.getResourceAsStream(resource);
        }

        return input;
    }

    /** Convert inputStream to String. Useful for reading .txt or .json that are inside the jar file */
    public static String convertInputStreamToString(InputStream inputStream) {
        final char[] buffer = new char[8192];
        final StringBuilder result = new StringBuilder();

        // InputStream -> Reader
        try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            int charsRead;
            while ((charsRead = reader.read(buffer, 0, buffer.length)) > 0) {
                result.append(buffer, 0, charsRead);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result.toString();

    }
}
