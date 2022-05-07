package com.seibel.lod.core.objects.a7;

import com.seibel.lod.core.objects.Pos2D;
import com.seibel.lod.core.objects.a7.pos.DhBlockPos2D;
import com.seibel.lod.core.objects.a7.pos.DhLodUnit;
import com.seibel.lod.core.objects.a7.pos.DhSectionPos;
import com.seibel.lod.core.util.DetailDistanceUtil;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.util.gridList.MovableGridRingList;

// QuadTree built from several layers of 2d ring buffers
public class LodQuadTree {

    public final int maxPossibleDetailLevel;
    private final MovableGridRingList<LodSection>[] ringLists;

    public LodQuadTree(int viewDistance, int initialPlayerX, int initialPlayerZ) {
        maxPossibleDetailLevel = DetailDistanceUtil.getDetailLevelFromDistance(viewDistance*Math.sqrt(2));
        ringLists = new MovableGridRingList[maxPossibleDetailLevel];
        int size;
        for (byte detailLevel = 0; detailLevel < maxPossibleDetailLevel; detailLevel++) {
            double distance = DetailDistanceUtil.getDrawDistanceFromDetail(detailLevel);
            int sectionCount = LodUtil.ceilDiv((int) Math.ceil(distance),
                    DhSectionPos.getWidth(detailLevel).toBlock()) + 1; // +1 for the border during move
            ringLists[detailLevel] = new MovableGridRingList<LodSection>(sectionCount,
                    initialPlayerX >> detailLevel, initialPlayerZ >> detailLevel);
        }
    }

    public LodSection getSection(DhSectionPos pos) {
        return getSection(pos.detail, pos.x, pos.z);
    }

    public LodSection getSection(byte detailLevel, int x, int z) {
        return ringLists[detailLevel].get(x, z);
    }

    enum LodSectionState {
        Loaded,
        Unloaded,
        Freed,
    }

    /*
    private LodSectionState expectsState(DhBlockPos2D playerPos, DhSectionPos pos) {
        // Get state of the children
        boolean hasAnyChildren = false;
        if (pos.detail != 0) {
            hasAnyChildren = getSection(pos.getChild(0)) != null ||
                    getSection(pos.getChild(1)) != null ||
                    getSection(pos.getChild(2)) != null ||
                    getSection(pos.getChild(3)) != null; // Do this to allow short-circuit
        }
        if (hasAnyChildren) {
            return LodSectionState.Unloaded;
        }
        // All children is in the Freed state

        // Calculate the distance to the player
        long dist = pos.getCenter().getCenter().distSquared(playerPos);
        byte targetDetail = DetailDistanceUtil.getDetailLevelFromDistance(dist);
    }*/

    public void tick(DhBlockPos2D playerPos) {
        for (int detailLevel = 0; detailLevel < maxPossibleDetailLevel; detailLevel++) {
            ringLists[detailLevel].move(playerPos.x >> detailLevel, playerPos.z >> detailLevel);
        }

        // First tick pass: update all sections' distanceBasedTargetLevel amd neighborCheckedTargetLevel
        for (byte detailLevel = 0; detailLevel < maxPossibleDetailLevel; detailLevel++) {
            final MovableGridRingList<LodSection> ringList = ringLists[detailLevel];
            final MovableGridRingList<LodSection> childRingList = detailLevel == 0 ? null : ringLists[detailLevel - 1];
            final byte detail = detailLevel;
            ringList.forEachPosOrdered((r, pos) -> {
                DhSectionPos sectionPos = new DhSectionPos(detail, pos.x, pos.y);
                long dist = sectionPos.getCenter().getCenter().distSquared(playerPos);
                byte targetDetail = DetailDistanceUtil.getDetailLevelFromDistance(dist);
                if (r == null) {
                    if (targetDetail <= detail) {
                        r = ringList.setChained(pos.x, pos.y, new LodSection(sectionPos));
                    } else {
                        return;
                    }
                }
                r.distanceBasedTargetLevel = targetDetail;
                if (childRingList == null) {
                    r.childTargetLevel = (byte) (r.distanceBasedTargetLevel - 1);
                } else {
                    byte minChildLevel = Byte.MAX_VALUE;
                    /* FIXME: Todo later
                    DhSectionPos childPos0 = sectionPos.getChild(0);
                    minChildLevel = LodUtil.min(minChildLevel, childRingList.get(childPos0.x, childPos0.z).childTargetLevel);
                    DhSectionPos childPos1 = sectionPos.getChild(1);
                    minChildLevel = LodUtil.min(minChildLevel, childRingList.get(childPos1.x, childPos1.z).childTargetLevel);
                    DhSectionPos childPos2 = sectionPos.getChild(2);
                    minChildLevel = LodUtil.min(minChildLevel, childRingList.get(childPos2.x, childPos2.z).childTargetLevel);
                    DhSectionPos childPos3 = sectionPos.getChild(3);
                    minChildLevel = LodUtil.min(minChildLevel, childRingList.get(childPos3.x, childPos3.z).childTargetLevel);
                    r.childTargetLevel = minChildLevel + 1;*/
                }
            });
        }

        // Second tick pass: load, unload, and free sections
        for (byte detailLevel = 0; detailLevel < maxPossibleDetailLevel; detailLevel++) {
            final MovableGridRingList<LodSection> ringList = ringLists[detailLevel];
            final byte detail = detailLevel;

        }

        // Update the tree from the bottom detail level upwards
        for (byte detailLevel = 0; detailLevel < maxPossibleDetailLevel; detailLevel++) {
            final MovableGridRingList<LodSection> ringList = ringLists[detailLevel];
            final byte detail = detailLevel;
            ringList.forEachPosOrdered((r, pos) -> {
                DhSectionPos sectionPos = new DhSectionPos(detail, pos.x, pos.y);
            });
        }

    }



}
