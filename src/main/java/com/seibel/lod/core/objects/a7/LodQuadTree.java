package com.seibel.lod.core.objects.a7;

import com.seibel.lod.core.objects.a7.datatype.column.ColumnDatatype;
import com.seibel.lod.core.objects.a7.pos.DhBlockPos2D;
import com.seibel.lod.core.objects.a7.pos.DhSectionPos;
import com.seibel.lod.core.util.DetailDistanceUtil;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.util.gridList.MovableGridRingList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    
    
    public final int numbersOfDetailLevels;
    private final MovableGridRingList<LodSection>[] ringLists;

    static class ContainerTypeConfigEntry {
        final Class<?> containerType;
        final int levelOffset;
        public ContainerTypeConfigEntry(Class<?> containerType, int levelOffset) {
            this.containerType = containerType;
            this.levelOffset = levelOffset;
        }
    }

    static final ArrayList<ContainerTypeConfigEntry> containerTypeConfig = new ArrayList<>();
    static {
        Collections.addAll(containerTypeConfig,
                null,
                null, //1
                null, //2
                null, //3
                new ContainerTypeConfigEntry(FullDatatype.class, 4), //4 -> 0
                null, //5 breaks down to 4
                null, //6 breaks down to 4
                new ContainerTypeConfigEntry(ColumnDatatype.class, ColumnDatatype.SECTION_SIZE_OFFSET), //7 -> 1
                new ContainerTypeConfigEntry(ColumnDatatype.class, ColumnDatatype.SECTION_SIZE_OFFSET), //8 -> 2
                new ContainerTypeConfigEntry(ColumnDatatype.class, ColumnDatatype.SECTION_SIZE_OFFSET), //9 -> 3
                new ContainerTypeConfigEntry(ColumnDatatype.class, ColumnDatatype.SECTION_SIZE_OFFSET), //10 -> 4
                new ContainerTypeConfigEntry(ColumnDatatype.class, ColumnDatatype.SECTION_SIZE_OFFSET) //11 -> 5...
        );
    }

    final ContainerTypeConfigEntry[] containerTypeConfigs;

    //public static final
    
    /**
     * Constructor of the quadTree
     * @param viewDistance View distance in blocks
     * @param initialPlayerX player x coordinate
     * @param initialPlayerZ player z coordinate
     */
    public LodQuadTree(int viewDistance, int initialPlayerX, int initialPlayerZ) {
        byte maxDetailLevel = DetailDistanceUtil.getDetailLevelFromDistance(viewDistance*Math.sqrt(2));
        ContainerTypeConfigEntry finalEntry = null;
        byte topSectionLevel = 0;
        for (; topSectionLevel < containerTypeConfig.size(); topSectionLevel++) {
            if (containerTypeConfig.get(topSectionLevel) == null) continue;
            finalEntry = containerTypeConfig.get(topSectionLevel);
            if (topSectionLevel - finalEntry.levelOffset >= maxDetailLevel) break;
        }
        if (finalEntry == null) throw new RuntimeException("No container type found!");
        if (topSectionLevel == containerTypeConfig.size())
            topSectionLevel = (byte) (maxDetailLevel - finalEntry.levelOffset);
        numbersOfDetailLevels = topSectionLevel + 1;
        containerTypeConfigs = new ContainerTypeConfigEntry[numbersOfDetailLevels];
        finalEntry = null;
        for (byte i = 0; i < numbersOfDetailLevels; i++) {
            if (containerTypeConfig.get(i) == null) continue; //TODO: Next here

        }

        ringLists = new MovableGridRingList[numbersOfDetailLevels];
        int size;
        for (byte detailLevel = 0; detailLevel < numbersOfDetailLevels; detailLevel++) {
            int distance = getFurthestPoint(detailLevel);
            ContainerTypeConfigEntry configEntry = containerTypeConfig.get(detailLevel);
            if (configEntry == null) {
                continue;
            }

            int sectionCount = LodUtil.ceilDiv(distance, DhSectionPos.getWidth(detailLevel).toBlock()) + 1; // +1 for the border during move
            ringLists[detailLevel] = new MovableGridRingList<LodSection>(sectionCount,
                    initialPlayerX >> detailLevel, initialPlayerZ >> detailLevel);
        }
    }
    
    /**
     * This method return the LodSection given the Section Pos
     * @param pos the section positon
     * @return the LodSection
     */
    public LodSection getSection(DhSectionPos pos) {
        return getSection(pos.detail, pos.x, pos.z);
    }

    /**
     * This method returns the RingList of a given detail level
     * @apiNote The returned ringList should not be modified!
     * @param detailLevel the detail level
     * @return the RingList
     */
    public MovableGridRingList<LodSection> getRingList(byte detailLevel) {
        return ringLists[detailLevel];
    }

    /**
     * This method returns the number of detail levels in the quadTree
     * @return the number of detail levels
     */
    public int getNumbersOfDetailLevels() {
        return numbersOfDetailLevels;
    }

    /**
     * This method return the LodSection at the given detail level and level coordinate x and z
     * @param detailLevel detail level of the section
     * @param x x coordinate of the section
     * @param z z coordinate of the section
     * @return the LodSection
     */
    public LodSection getSection(byte detailLevel, int x, int z) {
        return ringLists[detailLevel].get(x, z);
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
     * The method will return the furthest distance to the center for the given detail level
     * Override this method if you want to use a different algorithm
     * Note: the returned distance should always be the ceiling estimation of the distance
     * @param detailLevel detail level
     * @return the furthest distance to the center, in blocks
     */
    public int getFurthestPoint(byte detailLevel) {
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
        for (int detailLevel = 0; detailLevel < numbersOfDetailLevels; detailLevel++) {
            ringLists[detailLevel].move(playerPos.x >> detailLevel, playerPos.z >> detailLevel,
                    LodSection::dispose);
        }

        // First tick pass: update all sections' childCount from bottom level to top level. Step:
        //   If as detail 0 && section != null:
        //     - set childCount to 0
        //   If section != null && child != 0:
        //     - // Section will be in the unloaded state.
        //     - create parent if it doesn't exist, with childCount = 1
        //     - for each child:
        //       - if null, create new with childCount = 0
        //       - else if childCount == -1, set childCount = 0 (rescue it)
        //     - set childCount to 4
        //   Else:
        //     - Calculate targetLevel at that section
        //     - If targetLevel > detail && section != null:
        //       - Parent's childCount-- (Assert parent != null && childCount > 0 before decrementing)
        //       - // Note that this doesn't necessarily mean this section will be freed as it may be rescued later
        //            due to neighboring quadrants not able to be freed (they pass targetLevel checks or has children)
        //       - set childCount to -1 (Signal that this section will be freed if not rescued)
        //     - If targetLevel <= detail && section == null:
        //       - Parent's childCount++ (Create parent if needed)
        for (byte detailLevel = 0; detailLevel < numbersOfDetailLevels; detailLevel++) {
            final MovableGridRingList<LodSection> ringList = ringLists[detailLevel];
            final MovableGridRingList<LodSection> childRingList =
                    detailLevel == 0 ? null : ringLists[detailLevel - 1];
            final MovableGridRingList<LodSection> parentRingList =
                    detailLevel == numbersOfDetailLevels - 1 ? null : ringLists[detailLevel + 1];
            final byte detail = detailLevel;
            ringList.forEachPosOrdered((section, pos) -> {
                if (detail == 0 && section != null) {
                    section.childCount = 0;
                }
                if (section != null && section.childCount != 0) {
                    // Section will be in the unloaded state.
                    LodUtil.assertTrue(parentRingList != null);
                    LodSection parent = parentRingList.get(pos.x >> 1, pos.y >> 1);
                    if (parent == null) {
                        parent = parentRingList.setChained(pos.x >> 1, pos.y >> 1,
                                new LodSection(section.pos.getParent(), getRenderDataProvider()));
                        parent.childCount++;
                    }
                    LodUtil.assertTrue(parent.childCount <= 4 && parent.childCount > 0);
                    for (byte i = 0; i < 4; i++) {
                        DhSectionPos childPos = section.pos.getChild(i);
                        LodSection child = ringList.get(childPos.x, childPos.z);
                        if (child == null) {
                            child = ringList.setChained(childPos.x, childPos.z,
                                    new LodSection(childPos, getRenderDataProvider()));
                            child.childCount = 0;
                        } else if (child.childCount == -1) {
                            child.childCount = 0;
                        }
                    }
                    section.childCount = 4;
                } else {
                    DhSectionPos sectPos = section != null ? section.pos : new DhSectionPos(detail, pos.x, pos.y);
                    byte targetLevel = calculateExpectedDetailLevel(playerPos, sectPos);
                    if (targetLevel > detail && section != null) {
                        LodUtil.assertTrue(parentRingList != null);
                        LodSection parent = parentRingList.get(pos.x >> 1, pos.y >> 1);
                        LodUtil.assertTrue(parent != null);
                        LodUtil.assertTrue(parent.childCount <= 4 && parent.childCount > 0);
                        parent.childCount--;
                        section.childCount = -1;
                    } else if (targetLevel <= detail && section == null) {
                        LodUtil.assertTrue(parentRingList != null);
                        LodSection parent = parentRingList.get(pos.x >> 1, pos.y >> 1);
                        if (parent == null) {
                            parent = parentRingList.setChained(pos.x >> 1, pos.y >> 1,
                                    new LodSection(sectPos.getParent(), getRenderDataProvider()));
                        }
                        parent.childCount++;
                    }
                }
                // Final quick assert to insure section pos is correct.
                if (section != null) {
                    LodUtil.assertTrue(section.pos.detail == detail);
                    LodUtil.assertTrue(section.pos.x == pos.x);
                    LodUtil.assertTrue(section.pos.z == pos.y);
                }
            });
        }

        // Second tick pass: load and unload sections (and can also be used to assert everything is working). Step:
        //   // ===Assertion steps===
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
        for (byte detailLevel = 0; detailLevel < numbersOfDetailLevels; detailLevel++) {
            final MovableGridRingList<LodSection> ringList = ringLists[detailLevel];
            final MovableGridRingList<LodSection> childRingList =
                    detailLevel == 0 ? null : ringLists[detailLevel - 1];
            final MovableGridRingList<LodSection> parentRingList =
                    detailLevel == numbersOfDetailLevels - 1 ? null : ringLists[detailLevel + 1];
            ringList.forEachPosOrdered((section, pos) -> {
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
                if (section.childCount == 4 && section.isLoaded()) {
                    section.unload();
                } else if (section.childCount == 0 && !section.isLoaded()) {
                    section.load();
                } else if (section.childCount == -1) {
                    ringList.set(pos.x, pos.y, null);
                    section.dispose();
                }
            });
        }
    }
}
