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
 * @version 12-28-2021
 */
public class ConfigAnnotations {
    /** a textField, button, etc. that can be interacted with */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Entry
    {
        String name() default "";

        @Deprecated
        int width() default 150;

        double minValue() default Double.MIN_NORMAL;

        double maxValue() default Double.MAX_VALUE;
    }

    /** Makes text (looks like @Entry but dosnt save and has no button */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Category
    {
        String name() default "";
    }

    /** Makes text (looks like @Entry but dosnt save and has no button */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Comment
    {

    }








    @Deprecated
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface ScreenEntry
    {
        String name() default "";

        int width() default 100;
    }

    /**
     * Adds a comment to the file.
     *
     * DONT USE AS IT WILL BE REMOVED IN THE REWORK OF THE CONFIG
     */
    @Deprecated
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface FileComment
    {

    }
}
