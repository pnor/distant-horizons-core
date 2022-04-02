package com.seibel.lod.core.enums.rendering;

public enum RendererType {
    DEFAULT,
    DEBUG,
    DISABLED,
    ;

    public static RendererType next(RendererType type) {
        RendererType rendererType;
        switch (type) {
            case DEFAULT: rendererType = DEBUG;
            case DEBUG: rendererType = DISABLED;
            default: rendererType = DEFAULT;
        };
        return rendererType;
    }

    public static RendererType previous(RendererType type) {
        RendererType rendererType;
        switch (type) {
            case DEFAULT: rendererType = DISABLED;
            case DEBUG: rendererType = DEFAULT;
            default: rendererType = DEBUG;
        };
        return rendererType;
    }
}
