package com.seibel.lod.core.objects.a7.pos;

import com.seibel.lod.core.objects.DHBlockPos;

public class DhLodPos {
    public final byte detail;
    public final int x;
    public final int z;

    public DhLodPos(byte detail, int x, int z) {
        this.detail = detail;
        this.x = x;
        this.z = z;
    }

    public String toString() {
        return "DhLodPos(" + detail + ", " + x + ", " + z + ")";
    }

    public DhLodUnit getX() {
        return new DhLodUnit(detail, x);
    }

    public DhLodUnit getZ() {
        return new DhLodUnit(detail, z);
    }

    public int getWidth() {
        return 1 << detail;
    }
    public static int getWidth(byte detail) {
        return 1 << detail;
    }

    public DhBlockPos2D getCenter() {
        return new DhBlockPos2D(getX().toBlock() + (getWidth() >> 1), getZ().toBlock() + (getWidth() >> 1));
    }
    public DhBlockPos2D getCorner() {
        return new DhBlockPos2D(getX().toBlock(), getZ().toBlock());
    }
}
