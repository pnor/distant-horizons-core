package com.seibel.lod.core.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Where the annotations for the config are defined
 * If there is no annotation then the config will not touch it
 *
 * @author coolGi2007
 * @version 02-07-2022
 */
public class ConfigAnnotations {
    /** For making categories */
    @Deprecated
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    /** Use com.seibel.lod.core.config.types.ConfigCategory instead of this */
    public @interface Category {}


    /**
     * Makes text (looks like normal entry but doesn't save and has no button)
     *
     * Accepts string and the text is the value
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Comment {}

    /**
     * Adds a comment to the file,
     * This should only be used in special cases where comments from an entry cant reach
     *
     * Accepts string and the text is the value
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface FileComment {}
}
