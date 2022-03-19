package com.seibel.lod.core.enums.rendering;

import java.util.Objects;

public class FogSetting {
    public final double start;
    public final double end;
    public final double min;
    public final double max;
    public final double density;
    public final FogType fogType;

    public FogSetting(double start, double end, double min, double max, double density, FogType fogType) {
        this.start = start;
        this.end = end;
        this.min = min;
        this.max = max;
        this.density = density;
        this.fogType = fogType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FogSetting that = (FogSetting) o;
        return Double.compare(that.start, start) == 0 && Double.compare(that.end, end) == 0 && Double.compare(that.min, min) == 0 && Double.compare(that.max, max) == 0 && Double.compare(that.density, density) == 0 && fogType == that.fogType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end, min, max, density, fogType);
    }

    public enum FogType {
        LINEAR,
        EXPONENTIAL,
        EXPONENTIAL_SQUARED,
        // TEXTURE_BASED, // TODO: Impl this
    }
    

}
