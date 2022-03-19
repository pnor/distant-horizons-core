package com.seibel.lod.core.enums.rendering;

public enum HeightFogMode {
    ABOVE_CAMERA(true, true, false),
    BELOW_CAMERA(true, false, true),
    ABOVE_AND_BELOW_CAMERA(true, true, true),
    ABOVE_SET_HEIGHT(false, true, false),
    BELOW_SET_HEIGHT(false, false, true),
    ABOVE_AND_BELOW_SET_HEIGHT(false, true, true);

    public final boolean basedOnCamera;
    public final boolean above;
    public final boolean below;

    HeightFogMode(boolean basedOnCamera, boolean above, boolean below) {
        this.basedOnCamera = basedOnCamera;
        this.above = above;
        this.below = below;
    }
}
