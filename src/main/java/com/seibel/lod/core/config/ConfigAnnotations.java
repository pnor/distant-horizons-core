/*
 *    This file is part of the Distant Horizons mod (formerly the LOD Mod),
 *    licensed under the GNU LGPL v3 License.
 *
 *    Copyright (C) 2020-2022  James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
 
package com.seibel.lod.core.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Will be removed soon so please keep this out
 *
 * @author coolGi
 * @version 02-07-2022
 */
@Deprecated
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
    @Deprecated
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Comment {}

    /**
     * Adds a comment to the file,
     * This should only be used in special cases where comments from an entry cant reach
     *
     * Accepts string and the text is the value
     */
    @Deprecated
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface FileComment {}
}
