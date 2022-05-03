package com.seibel.lod.core.objects.a7;

import com.seibel.lod.core.util.DetailDistanceUtil;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.util.gridList.MovableGridRingList;

// QuadTree built from several layers of 2d ring buffers
public class LodQuadTree {

    public final int maxPossibleDetailLevel;
    private final MovableGridRingList[] ringLists;

    public LodQuadTree(int viewDistance, int initialPlayerX, int initialPlayerZ) {
        maxPossibleDetailLevel = DetailDistanceUtil.getDetailLevelFromDistance(viewDistance*Math.sqrt(2));
        ringLists = new MovableGridRingList[maxPossibleDetailLevel];
        for (int i = 0; i < maxPossibleDetailLevel; i++) {
            double distance = DetailDistanceUtil.getDrawDistanceFromDetail(i);
            int blockCount = ((int)Math.ceil(distance / (1 << i)));
            ringLists[i] = new MovableGridRingList<LodSubRegion>(
                    blockCount, initialPlayerX >> i, initialPlayerZ >> i);
        }
    }



}
