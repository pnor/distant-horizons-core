package com.seibel.lod.core.objects.a7.data;

// Static class for the data format:
// ID: blockState id    Y: Height(signed)    DP: Depth(signed?)
// =======Bit layout=======
// __ __ __ __  __ __ __ __ <-- Top bits
// YY YY YY YY  YY YY YY YY
// YY YY YY YY  DP DP DP DP
// DP DP DP DP  DP DP DP DP
// ID ID ID ID  ID ID IO ID
// ID ID ID ID  ID ID IO ID
// ID ID ID ID  ID ID IO ID
// ID ID ID ID  ID ID IO ID <-- Bottom bits


public class Data {

    public static final int ID_WIDTH = 32;
    public static final int DP_WIDTH = 12;
    public static final int Y_WIDTH = 12;
    public static final int ID_OFFSET = 0;
    public static final int DP_OFFSET = ID_OFFSET + ID_WIDTH;
    public static final int Y_OFFSET = DP_OFFSET + DP_WIDTH;

    public static final int ID_MASK = (int)Math.pow(2, ID_WIDTH) - 1;
    public static final int DP_MASK = (int)Math.pow(2, DP_WIDTH) - 1;
    public static final int Y_MASK = (int)Math.pow(2, Y_WIDTH) - 1;

    public static long encode(int id, int depth, int y) {
        long data = 0;
        data |= id & ID_MASK;
        data |= (long) (depth & DP_MASK) << DP_OFFSET;
        data |= (long) (y & Y_MASK) << Y_OFFSET;
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

}
