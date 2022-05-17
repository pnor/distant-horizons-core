package com.seibel.lod.core.objects.a7.pos;

public class DhLodUnit {
    public final byte detail;
    public final int value;

    public DhLodUnit(byte detail, int value) {
        this.detail = detail;
        this.value = value;
    }

    public int toBlock() {
        return value >> detail;
    }

    public static DhLodUnit fromBlock(int block, byte targetDetail) {
        return new DhLodUnit(targetDetail, block << targetDetail);
    }

    public DhLodUnit convertTo(byte targetDetail) {
        if (detail == targetDetail) {
            return this;
        }
        if (detail > targetDetail) { //TODO check if this is correct
            return new DhLodUnit(targetDetail, value << (detail - targetDetail));
        }
        return new DhLodUnit(targetDetail, value >> (targetDetail - detail));
    }
}
