package com.seibel.lod.core.objects.a7.pos;

import com.seibel.lod.core.enums.LodDirection;

import java.util.function.Consumer;

public class DhSectionPos {
    public static final byte SECTION_DETAIL_LEVEL_OFFSET = 6;
    public static final int DATA_WIDTH_PER_SECTION = 1 << SECTION_DETAIL_LEVEL_OFFSET;

    public final byte detail;
    public final int x;
    public final int z;
    public final int yOffset;

    public DhSectionPos(byte detail, int x, int z) {
        this.detail = detail;
        this.x = x;
        this.z = z;
        this.yOffset = 0;
    }
    public DhSectionPos(byte detail, int x, int z, int yOffset) {
        this.detail = detail;
        this.x = x;
        this.z = z;
        this.yOffset = yOffset;
    }

    public DhSectionPos withOffset(int yOffset) {
        return new DhSectionPos(detail, x, z, yOffset);
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
        return new DhSectionPos((byte) (detail - 1), x * 2 + (child0to3 & 1), z * 2 + (child0to3 & 2) / 2, yOffset);
    }

    public void forEachChild(Consumer<DhSectionPos> callback){
        for (int i = 0; i < 4; i++) {
            callback.accept(getChild(i));
        }
    }

    public DhSectionPos getParent(){
        return new DhSectionPos((byte) (detail + 1), x / 2, z / 2, yOffset);
    }

    public DhSectionPos getAdjacent(LodDirection dir) {
        return new DhSectionPos(detail, x + dir.getNormal().x, z + dir.getNormal().z, yOffset);
    }

    public DhSectionPos convertUpwardsTo(byte newDetail){
        if (detail == newDetail) return this;
        if (detail > newDetail) return new DhSectionPos(newDetail, x >> (detail - newDetail), z >> (detail - newDetail), yOffset);
        throw new IllegalArgumentException("newDetail must be greater than detail");
    }

    /**
     *  NOTE: This equals() does not consider yOffset!
     */

    public boolean equals(Object o){
        if (o == this) return true;
        if (!(o instanceof DhSectionPos)) return false;
        DhSectionPos other = (DhSectionPos) o;
        return detail == other.detail && x == other.x && z == other.z;
    }

    /**
     *  NOTE: This does not consider yOffset!
     */
    public boolean overlaps(DhSectionPos other){
        if (this.equals(other))
            return true;
        else if (detail < other.detail)
            return other.equals(this.convertUpwardsTo(other.detail));
        else
            return this.equals(other.convertUpwardsTo(detail));
    }
}
