package com.seibel.lod.core.objects.a7.pos;

import com.seibel.lod.core.objects.a7.DHLevel;
import org.lwjgl.system.CallbackI;

import java.util.function.Consumer;

public class DhSectionPos {
    public static final int DATA_WIDTH_PER_SECTION = 64;

    public final byte detail;
    public final int x;
    public final int z;

    public DhSectionPos(byte detail, int x, int z) {
        this.detail = detail;
        this.x = x;
        this.z = z;
    }

    public DhLodPos getCenter() {
        return new DhLodPos(detail, x * DATA_WIDTH_PER_SECTION + DATA_WIDTH_PER_SECTION / 2, z * DATA_WIDTH_PER_SECTION + DATA_WIDTH_PER_SECTION / 2);
    }

    public DhLodPos getCorner() {
        return new DhLodPos(detail, x * DATA_WIDTH_PER_SECTION, z * DATA_WIDTH_PER_SECTION);
    }

    public DhLodUnit getWidth() {
        return new DhLodUnit(detail, DATA_WIDTH_PER_SECTION);
    }

    public static DhLodUnit getWidth(byte detail) {
        return new DhLodUnit(detail, DATA_WIDTH_PER_SECTION);
    }

    public DhSectionPos getChild(int child0to3){
        if (child0to3 < 0 || child0to3 > 3) throw new IllegalArgumentException("child0to3 must be between 0 and 3");
        if (detail == 0) throw new IllegalStateException("detail must be greater than 0");
        return new DhSectionPos((byte) (detail - 1), x * 2 + (child0to3 & 1), z * 2 + (child0to3 & 2) / 2);
    }

    public void forEachChild(Consumer<DhSectionPos> callback){
        for (int i = 0; i < 4; i++) {
            callback.accept(getChild(i));
        }
    }

    public DhSectionPos getParent(){
        return new DhSectionPos((byte) (detail + 1), x / 2, z / 2);
    }
}
