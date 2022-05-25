package com.seibel.lod.core.objects.a7;

import com.seibel.lod.core.objects.a7.datatype.column.ColumnDatatype;
import com.seibel.lod.core.objects.a7.datatype.full.FullDatatype;
import com.seibel.lod.core.objects.a7.pos.DhBlockPos2D;
import com.seibel.lod.core.objects.a7.pos.DhSectionPos;
import com.seibel.lod.core.objects.a7.render.RenderDataSource;
import com.seibel.lod.core.objects.a7.render.RenderDataSourceLoader;
import com.seibel.lod.core.util.DetailDistanceUtil;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.util.gridList.MovableGridRingList;

import java.util.ArrayList;
import java.util.Collections;

// QuadTree built from several layers of 2d ring buffers

/**
 * This quadTree structure is the core of the DH mod.
 * This class represent a circular quadTree of lodSection
 *
 * Each section at level n is populated in one (sometimes more than one) ways:
 *      -by constructing it from the data of all the children sections (lower levels)
 *      -by loading from file
 *      -by adding data with the lodBuilder
 */
public abstract class LodQuadTree {


    /**
     * //TODO add static configs here
     * //These configs are updated someway
     * Comment: all config value should be via the class that extends this class, and
     *          by implementing different abstract methods - LeeTom
     */
    
    
    public final byte numbersOfSectionLevels;
    public final byte startingSectionLevel;
    private final MovableGridRingList<LodSection>[] ringLists;

    static final ArrayList<RenderDataSourceLoader> layerLoaderConfig = new ArrayList<>();

    public static void registerLayerLoader(RenderDataSourceLoader loader, byte sectionLevel) {
        while (layerLoaderConfig.size() <= sectionLevel) {
            layerLoaderConfig.add(null);
        }
        if (layerLoaderConfig.set(sectionLevel, loader) != null) {
            throw new RuntimeException("Layer loader for level " + sectionLevel + " has a registry conflict!");
        }
    }

//    static {
//        //TODO: Make this dynamic
//        Collections.addAll(layerLoaderConfig,
//                null,
//                null, //1
//                null, //2
//                null, //3
//                new ContainerTypeConfigEntry(FullDatatype.class, (byte) 4), //4 -> 0
//                null, //5 force breaks down to 4 -> 0
//                null, //6 force breaks down to 4 -> 0
//                new ContainerTypeConfigEntry(ColumnDatatype.class, ColumnDatatype.SECTION_SIZE_OFFSET), //7 -> 1
//                new ContainerTypeConfigEntry(ColumnDatatype.class, ColumnDatatype.SECTION_SIZE_OFFSET) //8 -> 2
//                // ... And same onwards
//        );
//    }

    static class SectionDetailLayer {
        final byte targetDataDetail;
        final RenderDataSourceLoader containerType;
        public SectionDetailLayer(byte targetDataDetail, RenderDataSourceLoader containerType) {
            this.targetDataDetail = targetDataDetail;
            this.containerType = containerType;
        }
    }

    static void assertContainerTypeConfigCorrect() {
        boolean isInFront = true;
        for (int i = 0; i < layerLoaderConfig.size(); i++) {
            if (layerLoaderConfig.get(i) == null) continue;
            isInFront = false;
            RenderDataSourceLoader entry = layerLoaderConfig.get(i);
            if (i - entry.detailOffset < 0) {
                throw new RuntimeException("ContainerTypeConfigEntry " + i + " has a levelOffset of "
                        + entry.detailOffset + " which makes the dataDetail be " + (i - entry.detailOffset) + "," +
                        " which is less than 0!");
            }
            if (entry.detailOffset < 0) {
                throw new RuntimeException("ContainerTypeConfigEntry " + i + " has a levelOffset of "
                        + entry.detailOffset + " which is less than 0!");
            }
        }
        if (layerLoaderConfig.get(layerLoaderConfig.size()-1) == null) {
            throw new RuntimeException("The last ContainerTypeConfigEntry is null, which is invalid!");
        }
    }

    final SectionDetailLayer[] sectionDetailLayers;
    
    /**
     * Constructor of the quadTree
     * @param viewDistance View distance in blocks
     * @param initialPlayerX player x coordinate
     * @param initialPlayerZ player z coordinate
     */
    public LodQuadTree(int viewDistance, int initialPlayerX, int initialPlayerZ) {
        assertContainerTypeConfigCorrect();

        { // Calculate the max section detail
            byte maxDetailLevel = getMaxDetailInRange(viewDistance * Math.sqrt(2));
            RenderDataSourceLoader finalEntry = null;
            byte topSectionLevel = 0;
            byte firstLevel = -1;
            for (; topSectionLevel < layerLoaderConfig.size(); topSectionLevel++) {
                if (layerLoaderConfig.get(topSectionLevel) == null) continue;
                finalEntry = layerLoaderConfig.get(topSectionLevel);
                if (firstLevel == -1) firstLevel = topSectionLevel;
                if (topSectionLevel - finalEntry.detailOffset >= maxDetailLevel) break;
            }
            if (finalEntry == null) throw new RuntimeException("No container type found!");
            if (topSectionLevel == layerLoaderConfig.size())
                topSectionLevel = (byte) (maxDetailLevel - finalEntry.detailOffset);
            numbersOfSectionLevels = (byte) (topSectionLevel + 1);
            startingSectionLevel = firstLevel;
            sectionDetailLayers = new SectionDetailLayer[numbersOfSectionLevels - startingSectionLevel];
            ringLists = new MovableGridRingList[numbersOfSectionLevels - startingSectionLevel];
        }

        { // Fill in the sectionDetailLayers info and construct the ringLists
            byte lastNonNullEntry = -1;
            for (byte i = startingSectionLevel; i < numbersOfSectionLevels; i++) {
                byte targetDataDetail;
                RenderDataSourceLoader containerType;

                if (i < layerLoaderConfig.size()) {
                    if (layerLoaderConfig.get(i) == null) {
                        if (lastNonNullEntry == -1) continue;
                        targetDataDetail = sectionDetailLayers[lastNonNullEntry].targetDataDetail;
                        containerType = null;
                    } else {
                        lastNonNullEntry = i;
                        RenderDataSourceLoader entry = layerLoaderConfig.get(i);
                        targetDataDetail = (byte) (i - entry.detailOffset);
                        containerType = entry;
                    }
                } else {
                    LodUtil.assertTrue(layerLoaderConfig.get(layerLoaderConfig.size() - 1) != null,
                            "The last entry must not be null!");
                    RenderDataSourceLoader entry = layerLoaderConfig.get(layerLoaderConfig.size() - 1);
                    targetDataDetail = (byte) (i - entry.detailOffset);
                    containerType = entry;
                }

                LodUtil.assertTrue(targetDataDetail >= 0, "dataDetail must be >= 0!");
                int maxDist = getFurthestDistance(targetDataDetail);
                int halfSize = LodUtil.ceilDiv(maxDist, (1 << i) + 2);
                sectionDetailLayers[i - startingSectionLevel] = new SectionDetailLayer(targetDataDetail, containerType);
                ringLists[i - startingSectionLevel] = new MovableGridRingList<LodSection>(halfSize,
                        initialPlayerX >> i, initialPlayerZ >> i);
            }
        }
    }


    /**
     * This method return the LodSection given the Section Pos
     * @param pos the section positon.
     * @return the LodSection
     */
    public LodSection getSection(DhSectionPos pos) {
        return getSection(pos.sectionDetail, pos.sectionX, pos.sectionZ);
    }

    public byte getFirstSectionDetailFromDataDetail(byte dataDetail) {
        if (dataDetail <= startingSectionLevel) return startingSectionLevel;
        for (byte i = 0; i < sectionDetailLayers.length; i++) {
            if (sectionDetailLayers[i].targetDataDetail >= dataDetail) return (byte) (i + startingSectionLevel);
        }
        throw new RuntimeException("No section detail for dataDetail " + dataDetail+ " found!");
    }
    public byte getDataDetail(byte sectionDetail) {
        return sectionDetailLayers[sectionDetail - startingSectionLevel].targetDataDetail;
    }

    /**
     * This method returns the RingList of a given detail level
     * @apiNote The returned ringList should not be modified!
     * @param detailLevel the detail level
     * @return the RingList
     */
    public MovableGridRingList<LodSection> getRingList(byte detailLevel) {
        return ringLists[detailLevel - startingSectionLevel];
    }

    /**
     * This method returns the number of detail levels in the quadTree
     * @return the number of detail levels
     */
    public byte getNumbersOfSectionLevels() {
        return numbersOfSectionLevels;
    }

    public byte getStartingSectionLevel() {
        return startingSectionLevel;
    }

    /**
     * This method return the LodSection at the given detail level and level coordinate x and z
     * @param detailLevel detail level of the section
     * @param x x coordinate of the section
     * @param z z coordinate of the section
     * @return the LodSection
     */
    public LodSection getSection(byte detailLevel, int x, int z) {
        return ringLists[detailLevel - startingSectionLevel].get(x, z);
    }

    
    /**
     * This method will compute the detail level based on player position and section pos
     * Override this method if you want to use a different algorithm
     * @param playerPos player position as a reference for calculating the detail level
     * @param sectionPos section position
     * @return detail level of this section pos
     */
    public byte calculateExpectedDetailLevel(DhBlockPos2D playerPos, DhSectionPos sectionPos) {
        return DetailDistanceUtil.getDetailLevelFromDistance(
                playerPos.dist(sectionPos.getCenter().getCenter()));
    }

    /**
     * The method will return the highest detail level in a circle around the center
     * Override this method if you want to use a different algorithm
     * Note: the returned distance should always be the ceiling estimation of the distance
     * //TODO: Make this input a bbox or a circle or something....
     * @param distance the circle radius
     * @return the highest detail level in the circle
     */
    public byte getMaxDetailInRange(double distance) {
        return DetailDistanceUtil.getDetailLevelFromDistance(distance);
    }

    /**
     * The method will return the furthest distance to the center for the given detail level
     * Override this method if you want to use a different algorithm
     * Note: the returned distance should always be the ceiling estimation of the distance
     * //TODO: Make this return a bbox instead of a distance in circle
     * @param detailLevel detail level
     * @return the furthest distance to the center, in blocks
     */
    public int getFurthestDistance(byte detailLevel) {
        return (int)Math.ceil(DetailDistanceUtil.getDrawDistanceFromDetail(detailLevel));
    }

    public abstract RenderDataProvider getRenderDataProvider();

    
    /**
     * Given a section pos at level n this method returns the parent section at level n+1
     * @param pos the section positon
     * @return the parent LodSection
     */
    public LodSection getParentSection(DhSectionPos pos) {
        return getSection(pos.getParent());
    }
    
    /**
     * Given a section pos at level n and a child index this method return the
     * child section at level n-1
     * @param pos
     * @param child0to3 since there are 4 possible children this index identify which one we are getting
     * @return one of the child LodSection
     */
    public LodSection getChildSection(DhSectionPos pos, int child0to3) {
        return getSection(pos.getChild(child0to3));
    }
    
    /**
     * This function update the quadTree based on the playerPos and the current game configs (static and global)
     * @param playerPos the reference position for the player
     */
    public void tick(DhBlockPos2D playerPos) {
        for (int sectLevel = startingSectionLevel; sectLevel < numbersOfSectionLevels; sectLevel++) {
            ringLists[sectLevel - startingSectionLevel]
                    .move(playerPos.x >> sectLevel, playerPos.z >> sectLevel,
                    LodSection::dispose);
        }





        // First tick pass: update all sections' childCount from bottom level to top level. Step:
        //   If sectLevel is bottom && section != null:
        //     - set childCount to 0
        //   If section != null && child != 0: //TODO: Should I move this createChild steps to Second tick pass?
        //     - // Section will be in the unloaded state.
        //     - create parent if it doesn't exist, with childCount = 1
        //     - for each child:
        //       - if null, create new with childCount = 0 (force load due to neighboring issues)
        //       - else if childCount == -1, set childCount = 0 (rescue it)
        //     - set childCount to 4
        //   Else:
        //     - Calculate targetLevel at that section
        //     - If sectLevel == numberOfSectionLevels - 1:
        //       - // Section is the top level.
        //       - If targetLevel > dataLevel@sectLevel && section != null:
        //         - set childCount to -1 (Signal that section is to be freed) (this prob not be rescued as it is the top level)
        //       - If targetLevel <= dataLevel@sectLevel && section == null: (direct use the current sectLevel's dataLevel)
        //         - create new section with childCount = 0
        //     - Else:
        //       - // Section is not the top level. So we also need to consider the parent.
        //       - If targetLevel >= dataLevel@(sectLevel+1) && section != null: (use the next level's dataLevel)
        //         - Parent's childCount-- (Assert parent != null && childCount > 0 before decrementing)
        //         - // Note that this doesn't necessarily mean this section will be freed as it may be rescued later
        //              due to neighboring quadrants not able to be freed (they pass targetLevel checks or has children)
        //              or due to parent's layer is in the Always Cascade mode. (containerType == null)
        //         - set childCount to -1 (Signal that this section will be freed if not rescued)
        //       - If targetLevel < dataLevel@(sectLevel+1) && section == null: (use the next level's dataLevel)
        //         - create new section with childCount = 0
        //         - Parent's childCount++ (Create parent if needed)
        for (byte sectLevel = startingSectionLevel; sectLevel < numbersOfSectionLevels; sectLevel++) {
            final MovableGridRingList<LodSection> ringList = ringLists[sectLevel - startingSectionLevel];
            final MovableGridRingList<LodSection> childRingList =
                    sectLevel == startingSectionLevel ? null : ringLists[sectLevel - startingSectionLevel - 1];
            final MovableGridRingList<LodSection> parentRingList =
                    sectLevel == numbersOfSectionLevels - 1 ? null : ringLists[sectLevel - startingSectionLevel + 1];
            final byte f_sectLevel = sectLevel;
            RenderDataSourceLoader containerType = sectionDetailLayers[sectLevel - startingSectionLevel].containerType;
            ringList.forEachPosOrdered((section, pos) -> {
                if (f_sectLevel == 0 && section != null) {
                    section.childCount = 0;
                }
                if (section != null && section.childCount != 0) {
                    // Section will be in the unloaded state.
                    LodUtil.assertTrue(parentRingList != null);
                    LodSection parent = parentRingList.get(pos.x >> 1, pos.y >> 1);
                    if (parent == null) {
                        parent = parentRingList.setChained(pos.x >> 1, pos.y >> 1,
                                new LodSection(section.pos.getParent()));
                        parent.childCount++;
                    }
                    LodUtil.assertTrue(parent.childCount <= 4 && parent.childCount > 0);
                    for (byte i = 0; i < 4; i++) {
                        DhSectionPos childPos = section.pos.getChild(i);
                        LodSection child = childRingList.get(childPos.sectionX, childPos.sectionZ);
                        if (child == null) {
                            child = childRingList.setChained(childPos.sectionX, childPos.sectionZ,
                                    new LodSection(childPos));
                            child.childCount = 0;
                        } else if (child.childCount == -1) {
                            child.childCount = 0;
                        }
                    }
                    section.childCount = 4;
                } else {
                    DhSectionPos sectPos = section != null ? section.pos : new DhSectionPos(f_sectLevel, pos.x, pos.y);
                    byte targetLevel = calculateExpectedDetailLevel(playerPos, sectPos);
                    if (f_sectLevel == numbersOfSectionLevels -1) {
                        // Section is in the top level.
                        if (targetLevel > getDataDetail(f_sectLevel) && section != null) {
                            section.childCount = -1;
                        }
                        if (targetLevel <= getDataDetail(f_sectLevel) && section == null) {
                            section = ringList.setChained(pos.x, pos.y,
                                    new LodSection(sectPos));
                        }
                    } else {
                        // Section is not the top level. So we also need to consider the parent.
                        if (targetLevel >= getDataDetail((byte) (f_sectLevel+1)) && section != null) {
                            LodUtil.assertTrue(parentRingList != null);
                            LodSection parent = parentRingList.get(pos.x >> 1, pos.y >> 1);
                            LodUtil.assertTrue(parent != null);
                            LodUtil.assertTrue(parent.childCount <= 4 && parent.childCount > 0);
                            parent.childCount--;
                            section.childCount = -1;
                        }
                        if (targetLevel < getDataDetail((byte) (f_sectLevel+1)) && section == null) {
                            section = ringList.setChained(pos.x, pos.y,
                                    new LodSection(sectPos));
                            LodUtil.assertTrue(parentRingList != null);
                            LodSection parent = parentRingList.get(pos.x >> 1, pos.y >> 1);
                            if (parent == null) {
                                parent = parentRingList.setChained(pos.x >> 1, pos.y >> 1,
                                        new LodSection(sectPos.getParent()));
                            }
                            parent.childCount++;
                        }
                    }
                }
                // Final quick assert to insure section pos is correct.
                if (section != null) {
                    LodUtil.assertTrue(section.pos.sectionDetail == f_sectLevel);
                    LodUtil.assertTrue(section.pos.sectionX == pos.x);
                    LodUtil.assertTrue(section.pos.sectionZ == pos.y);
                }
            });
        }

        // Second tick pass:
        // Cascade the layers that is in Always Cascade Mode from top to bottom. (layer's containerType == null)
        // At the same time, load and unload sections (and can also be used to assert everything is working). Step:
        // ===Assertion steps===
        //   assert childCount == 4 || childCount == 0 || childCount == -1
        //   if childCount == 4 assert all children exist
        //   if childCount == 0 assert all children are null
        //   if childCount == -1 assert parent childCount is 0
        //   // ======================
        //   if childCount == 4 && section is loaded:
        //     - unload section
        //   if childCount == 0 && section is unloaded:
        //     - load section
        //   if childCount == -1: // (section can be loaded or unloaded, due to fast movement)
        //     - set this section to null (TODO: Is this needed to be first or last or don't matter for concurrency?)
        //     - If loaded unload section
        for (byte sectLevel = (byte) (numbersOfSectionLevels - 1); sectLevel >= startingSectionLevel; sectLevel--) {
            final MovableGridRingList<LodSection> ringList = ringLists[sectLevel - startingSectionLevel];
            final MovableGridRingList<LodSection> childRingList =
                    sectLevel == startingSectionLevel ? null : ringLists[sectLevel - startingSectionLevel - 1];
            final boolean doCacsade = sectionDetailLayers[sectLevel - startingSectionLevel].containerType == null;
            RenderDataSourceLoader containerType = sectionDetailLayers[sectLevel - startingSectionLevel].containerType;

            ringList.forEachPosOrdered((section, pos) -> {
                if (section == null) return;

                // Cascade layers
                if (doCacsade && section.childCount == 0) {
                    LodUtil.assertTrue(childRingList != null);
                    // Create childs to cascade the layer.
                    for (byte i = 0; i < 4; i++) {
                        DhSectionPos childPos = section.pos.getChild(i);
                        LodSection child = childRingList.get(childPos.sectionX, childPos.sectionZ);
                        if (child == null) {
                            child = childRingList.setChained(childPos.sectionX, childPos.sectionZ,
                                    new LodSection(childPos));
                            child.childCount = 0;
                        } else {
                            LodUtil.assertTrue(child.childCount == -1,
                                    "Self has child count 0 but an existing child's child count != -1!");
                            child.childCount = 0;
                        }
                    }
                    section.childCount = 4;
                }

                // Assertion steps
                LodUtil.assertTrue(section.childCount == 4 || section.childCount == 0 || section.childCount == -1);
                if (section.childCount == 4) LodUtil.assertTrue(
                        getChildSection(section.pos, 0) != null &&
                                getChildSection(section.pos, 1) != null &&
                                getChildSection(section.pos, 2) != null &&
                                getChildSection(section.pos, 3) != null);
                if (section.childCount == 0) LodUtil.assertTrue(
                        getChildSection(section.pos, 0) == null &&
                                getChildSection(section.pos, 1) == null &&
                                getChildSection(section.pos, 2) == null &&
                                getChildSection(section.pos, 3) == null);
                if (section.childCount == -1) LodUtil.assertTrue(
                        getParentSection(section.pos).childCount == 0);

                // Call load on new sections, and tick on existing ones, and dispose old sections
                if (section.childCount == -1) {
                    ringList.set(pos.x, pos.y, null);
                    section.dispose();
                } else {
                    if (!section.isLoaded() && !section.isLoading()) {
                        section.load(getRenderDataProvider(), containerType);
                    }
                    if (section.childCount == 4) section.enableRender(this);
                    if (section.childCount == 0) section.disableRender();
                    section.tick(this);
                }
            });
        }
    }
}
