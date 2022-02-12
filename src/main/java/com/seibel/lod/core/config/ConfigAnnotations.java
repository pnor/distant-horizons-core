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
    /** A textField, button, etc. that can be interacted with */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Entry
    {
        String name() default "";
        
        @Deprecated
        int width() default 150;

        @Deprecated
        double minValue() default Double.MIN_NORMAL;

        @Deprecated
        double maxValue() default Double.MAX_VALUE;

    }

    /** For making categories */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Category
    {

    }

    /** Makes text (looks like @Entry but dosnt save and has no button */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Comment
    {

    }

    /**
     * Adds a comment to the file,
     * This should only be used in special cases where comments from an entry cant reach
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface FileComment
    {

    }

    /** DONT USE AS IT WILL BE REMOVED IN THE REWORK OF THE CONFIG */
    @Deprecated
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface ScreenEntry
    {
        String name() default "";

        int width() default 100;
    }
}
