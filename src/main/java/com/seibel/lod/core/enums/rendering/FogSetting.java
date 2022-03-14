package com.seibel.lod.core.enums.rendering;

public class FogSetting {
    public final double start;
    public final double end;
    public final double min;
    public final double max;
    public final double density;
    public final Type type;

    public FogSetting(double start, double end, double min, double max, double density, Type type) {
        this.start = start;
        this.end = end;
        this.min = min;
        this.max = max;
        this.density = density;
        this.type = type;
    }

    public enum Type {
        LINEAR,
        EXPONENTIAL,
        EXPONENTIAL_SQUARED,
        // TEXTURE_BASED, // TODO: Impl this
    }
}
