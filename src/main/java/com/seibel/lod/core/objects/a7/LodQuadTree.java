package com.seibel.lod.core.objects.a7;

import com.seibel.lod.core.util.DetailDistanceUtil;
import com.seibel.lod.core.util.gridList.MovableGridRingList;

// QuadTree built from several layers of 2d ring buffers
public class LodQuadTree {

    public final int maxPossibleDetailLevel;
    private final MovableGridRingList<LodSection>[] ringLists;

    public LodQuadTree(int viewDistance, int initialPlayerX, int initialPlayerZ) {
        maxPossibleDetailLevel = DetailDistanceUtil.getDetailLevelFromDistance(viewDistance*Math.sqrt(2));
        ringLists = new MovableGridRingList[maxPossibleDetailLevel];
        int size;
        for (int detailLevel = 0; detailLevel < maxPossibleDetailLevel; detailLevel++) {
            double distance = DetailDistanceUtil.getDrawDistanceFromDetail(detailLevel);
            int blockCount = ((int)Math.ceil(distance / (1 << detailLevel)));
            ringLists[detailLevel] = new MovableGridRingList<LodSection>(blockCount, initialPlayerX >> detailLevel, initialPlayerZ >> detailLevel);
            size = ringLists[detailLevel].getSize();
            for(int sectionIndexX = 0; sectionIndexX < size; sectionIndexX++)
            {
                for(int sectionIndexZ = 0; sectionIndexZ < size; sectionIndexZ++)
                {
                    ringLists[detailLevel].set(sectionIndexX, sectionIndexZ, new LodSection(sectionIndexX))
                }
            }
        }
        
    }



}
