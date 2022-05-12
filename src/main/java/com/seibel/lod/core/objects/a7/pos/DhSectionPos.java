package com.seibel.lod.core.objects.a7.pos;

import com.seibel.lod.core.enums.LodDirection;

import java.util.function.Consumer;

public class DhSectionPos {
    public final byte detail;
    public final int x;
    public final int z;
    public final int yOffset;
    public final byte dataDetailOffset;

    public DhSectionPos(byte detail, int x, int z, int yOffset, byte dataDetailOffset) {
        this.detail = detail;
        this.x = x;
        this.z = z;
        this.yOffset = yOffset;
        this.dataDetailOffset = dataDetailOffset;
    }

    public DhSectionPos withYOffset(int yOffset) {
        return new DhSectionPos(detail, x, z, yOffset, dataDetailOffset);
    }
    public DhSectionPos withDataOffset(byte dataDetailOffset) {
        return new DhSectionPos(detail, x, z, yOffset, dataDetailOffset);
    }

    public DhLodPos getCenter() {
        if (dataDetailOffset == 0) return new DhLodPos(detail, x, z);
        return new DhLodPos(detail, (x << dataDetailOffset)+(1 << (dataDetailOffset-1)), (z << dataDetailOffset)+(1 << (dataDetailOffset-1)));
    }

    public DhLodPos getCorner() {
        return new DhLodPos(detail, x << dataDetailOffset, z << dataDetailOffset);
    }

    public DhLodUnit getWidth() {
        return new DhLodUnit(detail, 1 << dataDetailOffset);
    }

    public static DhLodUnit getWidth(byte detail, byte dataDetailOffset){
        return new DhLodUnit(detail, 1 << dataDetailOffset);
    }

    public DhSectionPos getChild(int child0to3){
        if (child0to3 < 0 || child0to3 > 3) throw new IllegalArgumentException("child0to3 must be between 0 and 3");
        if (detail-dataDetailOffset <= 0) throw new IllegalStateException("detail or data detail must be greater than 0");
        return new DhSectionPos((byte) (detail - 1), x * 2 + (child0to3 & 1), z * 2 + (child0to3 & 2) / 2, yOffset, dataDetailOffset);
    }

    public void forEachChild(Consumer<DhSectionPos> callback){
        for (int i = 0; i < 4; i++) {
            callback.accept(getChild(i));
        }
    }

    public DhSectionPos getParent(){
        return new DhSectionPos((byte) (detail + 1), x / 2, z / 2, yOffset, dataDetailOffset);
    }

    public DhSectionPos getAdjacent(LodDirection dir) {
        return new DhSectionPos(detail, x + dir.getNormal().x, z + dir.getNormal().z, yOffset, dataDetailOffset);
    }

    public DhSectionPos convertUpwardsTo(byte newDetail){
        if (detail == newDetail) return this;
        if (detail > newDetail) return
                new DhSectionPos(newDetail, x >> (detail - newDetail), z >> (detail - newDetail), yOffset, dataDetailOffset);
        throw new IllegalArgumentException("newDetail must be greater than detail");
    }

    /**
     *  NOTE: This equals() does not consider yOffset or dataDetailOffset!
     */

    public boolean equals(Object o){
        if (o == this) return true;
        if (!(o instanceof DhSectionPos)) return false;
        DhSectionPos other = (DhSectionPos) o;
        return detail == other.detail && x == other.x && z == other.z;
    }

    /**
     *  NOTE: This does not consider yOffset! (dataDetailOffset is also ignored since, well, it doesn't effect the outcome)
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
