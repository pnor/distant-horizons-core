package com.seibel.lod.core;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.seibel.lod.core.handlers.dependencyInjection.SingletonHandler;
import com.seibel.lod.core.jar.DarkModeDetector;
import com.seibel.lod.core.jar.BaseJFrame;
import com.seibel.lod.core.jar.installer.GitlabGetter;
import com.seibel.lod.core.jar.JarDependencySetup;
import com.seibel.lod.core.jar.installer.WebDownloader;

import javax.swing.*;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;

/**
 * The main class when you run the standalone jar
 *
 * @author coolGi
 */
public class JarMain {
    public static final boolean isDarkTheme = DarkModeDetector.isDarkMode();
    public static boolean isOffline = WebDownloader.netIsAvailable();

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
        GitlabGetter.init();
        System.out.println("WARNING: The standalone jar still work in progress");

//        JOptionPane.showMessageDialog(null, "The GUI for the standalone jar isn't made yet\nIf you want to use the mod then put it in your mods folder", "Distant Horizons", JOptionPane.WARNING_MESSAGE);

        if (getOperatingSystem().equals(OperatingSystem.MACOS)) {
            System.out.println("If you want the installer then please use Linux or for the time being.\nMacOS support/testing will come later on");
        }

        // All code beyond this point is messy and will be rewritten later as I dont like it
        // ===============================================================================================================

        BaseJFrame frame = new BaseJFrame(false, false).addExtraButtons();
//        String[] optionsToChoose = {"Apple", "Orange", "Banana", "Pineapple"};
//        JComboBox<String> jTest = new JComboBox<>(optionsToChoose);
//        jTest.setBounds(400, 250, 140, 20);
//        frame.add(jTest);
//        jTest.addActionListener(e -> { System.out.println("test"); });

        JFileChooser minecraftDirPop = new JFileChooser();
        if (getOperatingSystem().equals(OperatingSystem.WINDOWS))
            minecraftDirPop.setCurrentDirectory(new File(System.getenv("APPDATA")+"/.minecraft/mods"));
        if (getOperatingSystem().equals(OperatingSystem.LINUX))
            minecraftDirPop.setCurrentDirectory(new File(System.getProperty("user.home")+"/.minecraft/mods"));
        minecraftDirPop.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        JButton minecraftDirBtn = new JButton("Click to select install path");
        minecraftDirBtn.addActionListener(e -> {
            if (minecraftDirPop.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION)
                minecraftDirBtn.setText(minecraftDirPop.getSelectedFile().toString());
        });
        minecraftDirBtn.setBounds(frame.getWidth()/2 - (200/2), 250, 200, 20);
        frame.add(minecraftDirBtn);

        JComboBox<String> modVersions = new JComboBox<>(
                Arrays.copyOf(GitlabGetter.readableReleaseNames.toArray(), GitlabGetter.readableReleaseNames.toArray().length, String[].class)
        );
        modVersions.setBounds(frame.getWidth()/2 - (200/2), 280, 200, 20);

        JComboBox<String> modMcVersion = new JComboBox<>();
        modMcVersion.setBounds(frame.getWidth()/2 - (200/2), 310, 200, 20);
        modMcVersion.setModel( new DefaultComboBoxModel(
                Arrays.copyOf(
                        GitlabGetter.getMcVersionsInRelease(GitlabGetter.releaseNames.get(modVersions.getSelectedIndex())).toArray(),
                        GitlabGetter.getMcVersionsInRelease(GitlabGetter.releaseNames.get(modVersions.getSelectedIndex())).toArray().length,
                        String[].class
                ))
        );

        modVersions.addActionListener( e -> {
            modMcVersion.setModel( new DefaultComboBoxModel(
                    Arrays.copyOf(
                            GitlabGetter.getMcVersionsInRelease(GitlabGetter.releaseNames.get(modVersions.getSelectedIndex())).toArray(),
                            GitlabGetter.getMcVersionsInRelease(GitlabGetter.releaseNames.get(modVersions.getSelectedIndex())).toArray().length,
                            String[].class
                    ))
            );
            frame.validate();
        });
        frame.add(modVersions);
        frame.add(modMcVersion);


        // Fabric installer
//        try {
//            WebDownloader.downloadAsFile(new URL("https://maven.fabricmc.net/net/fabricmc/fabric-installer/0.11.0/fabric-installer-0.11.0.jar"), new File(System.getProperty("java.io.tmpdir") + "/fabricInstaller.jar"));
//            Runtime.getRuntime().exec("java -jar " + System.getProperty("java.io.tmpdir") + "/fabricInstaller.jar");
//        } catch (Exception e) {e.printStackTrace();}

        JButton installMod = new JButton("Install "+ModInfo.READABLE_NAME);
        installMod.setBounds(frame.getWidth()/2 - (200/2), 340, 200, 20);
        installMod.addActionListener( e -> {
            if (minecraftDirPop.getSelectedFile() == null) {
                JOptionPane.showMessageDialog(frame, "Please select your install directory", ModInfo.READABLE_NAME, JOptionPane.WARNING_MESSAGE);
                return;
            }

//            JOptionPane.showMessageDialog(frame, "Installing "+ModInfo.READABLE_NAME+" version "+modVersions.getSelectedItem()+" for Minecraft version "+modMcVersion.getSelectedItem()+" \nAt "+minecraftDirPop.getSelectedFile(), ModInfo.READABLE_NAME, JOptionPane.INFORMATION_MESSAGE);

            URL downloadPath = GitlabGetter.getRelease(
                    GitlabGetter.releaseNames.get(modVersions.getSelectedIndex()),
                    (String) modMcVersion.getSelectedItem());
            try {
                if (downloadPath.toString().contains("curseforge.com"))
                    downloadPath = new URL(downloadPath.toString() + "/file");
            } catch (Exception f) { f.printStackTrace(); }

            if (!WebDownloader.downloadAsFile(
                    downloadPath,
                    minecraftDirPop.getSelectedFile().toPath().resolve(
                        ModInfo.NAME+"-"+GitlabGetter.releaseNames.get(modVersions.getSelectedIndex())+"-"+((String) modMcVersion.getSelectedItem())+".jar"
                    ).toFile()
            ))
                JOptionPane.showMessageDialog(frame, "Download failed. Check your internet connection", ModInfo.READABLE_NAME, JOptionPane.ERROR_MESSAGE);
            else
                JOptionPane.showMessageDialog(frame, "Installation done. \nYou can now close the installer", ModInfo.READABLE_NAME, JOptionPane.INFORMATION_MESSAGE);
        });
        frame.add(installMod);

        frame.addLogo();

        frame.validate(); // Update to add the widgets
        frame.setVisible(true); // Start the ui
    }




    public enum OperatingSystem {WINDOWS, MACOS, LINUX, NONE} // Easy to use enum for the 3 main os's
    public static OperatingSystem getOperatingSystem() { // Get the os and turn it into that enum
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
