package com.seibel.lod.core.enums.rendering;

public enum RendererType {
    DEFAULT,
    DEBUG,
    DISABLED,
    ;

    public static RendererType next(RendererType type) {
        return switch (type) {
            case DEFAULT -> DEBUG;
            case DEBUG -> DISABLED;
            default -> DEFAULT;
        };
    }

    public static RendererType previous(RendererType type) {
        return switch (type) {
            case DEFAULT -> DISABLED;
            case DEBUG -> DEFAULT;
            default -> DEBUG;
        };
    }
}
