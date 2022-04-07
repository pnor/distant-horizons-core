/*
 *    This file is part of the Distant Horizon mod (formerly the LOD Mod),
 *    licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2020  James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
 
package com.seibel.lod.core.enums.rendering;

public enum RendererType {
    DEFAULT,
    DEBUG,
    DISABLED,
    ;

    public static RendererType next(RendererType type) {
        switch (type) {
            case DEFAULT: return DEBUG;
            case DEBUG: return DISABLED;
            default: return DEFAULT;
        }
    }

    public static RendererType previous(RendererType type) {
        switch (type) {
            case DEFAULT: return DISABLED;
            case DEBUG: return DEFAULT;
            default: return DEBUG;
        }
    }
}
