package com.seibel.lod.core;

import com.seibel.lod.core.jar.JarDependencySetup;

import java.io.InputStream;

/**
 * The main class when you run the standalone jar
 *
 * @author coolGi
 */
public class JarMain {
    public static void main(String[] args){
        JarDependencySetup.createInitialBindings();
        System.out.println("Why are you running the jar, this isn't done yet  >:(");
    }

    /** Get a file within the mods resources */
    public static InputStream accessFile(String resource) {

        // this is the path within the jar file
        InputStream input = JarMain.class.getResourceAsStream("/resources/" + resource);
        if (input == null) {
            // this is how we load file within editor (eg eclipse)
            input = JarMain.class.getClassLoader().getResourceAsStream(resource);
        }

        return input;
    }
}
