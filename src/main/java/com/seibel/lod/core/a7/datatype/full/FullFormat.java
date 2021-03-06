package com.seibel.lod.core.a7.datatype.full;

// Static class for the data format:
// ID: blockState id    Y: Height(signed)    DP: Depth(signed?)
// BL: Block light     SL: Sky light
// =======Bit layout=======
// BL BL BL BL  SL SL SL SL <-- Top bits
// YY YY YY YY  YY YY YY YY
// YY YY YY YY  DP DP DP DP
// DP DP DP DP  DP DP DP DP
// ID ID ID ID  ID ID IO ID
// ID ID ID ID  ID ID IO ID
// ID ID ID ID  ID ID IO ID
// ID ID ID ID  ID ID IO ID <-- Bottom bits

import org.jetbrains.annotations.Contract;

public class FullFormat {

    public static final int ID_WIDTH = 32;
    public static final int DP_WIDTH = 12;
    public static final int Y_WIDTH = 12;
    public static final int LIGHT_WIDTH = 8;
    public static final int ID_OFFSET = 0;
    public static final int DP_OFFSET = ID_OFFSET + ID_WIDTH;
    public static final int Y_OFFSET = DP_OFFSET + DP_WIDTH;
    public static final int LIGHT_OFFSET = Y_OFFSET + Y_WIDTH;


    public static final long ID_MASK = Integer.MAX_VALUE;
    public static final long INVERSE_ID_MASK = ~ID_MASK;
    public static final int DP_MASK = (int)Math.pow(2, DP_WIDTH) - 1;
    public static final int Y_MASK = (int)Math.pow(2, Y_WIDTH) - 1;

    public static long encode(int id, int depth, int y, byte lightPair) {
        long data = 0;
        data |= id & ID_MASK;
        data |= (long) (depth & DP_MASK) << DP_OFFSET;
        data |= (long) (y & Y_MASK) << Y_OFFSET;
        data |= (long) lightPair << LIGHT_OFFSET;
        return data;
    }

    public static int getId(long data) {
        return (int) (data & ID_MASK);
    }

    public static int getDepth(long data) {
        return (int) (data << (64 - DP_OFFSET - DP_WIDTH) >> DP_OFFSET);
    }

    public static int getY(long data) {
        return (int) (data << (64 - Y_OFFSET - Y_WIDTH) >> Y_OFFSET);
    }

    public static byte getLight(long data) {
        return (byte) (data << (64 - LIGHT_OFFSET - LIGHT_WIDTH) >> LIGHT_OFFSET);
    }

    @Contract(pure = true)
    public static long remap(int[] mapping, long data) {
        return (data & INVERSE_ID_MASK) | mapping[(int)data];
    }
}
