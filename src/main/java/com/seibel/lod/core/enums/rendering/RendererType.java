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
