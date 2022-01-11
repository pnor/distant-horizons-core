/*
 *    This file is part of the Distant Horizon mod (formerly the LOD Mod),
 *    licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2020  James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.seibel.lod.core.objects.lod;

import com.seibel.lod.core.enums.config.DistanceGenerationMode;
import com.seibel.lod.core.enums.config.DropoffQuality;
import com.seibel.lod.core.enums.config.GenerationPriority;
import com.seibel.lod.core.enums.config.VerticalQuality;
import com.seibel.lod.core.objects.PosToGenerateContainer;
import com.seibel.lod.core.objects.PosToRenderContainer;
import com.seibel.lod.core.util.DataPointUtil;
import com.seibel.lod.core.util.DetailDistanceUtil;
import com.seibel.lod.core.util.LevelPosUtil;
import com.seibel.lod.core.util.LodUtil;

/**
 * This object holds all loaded LevelContainers acting as a quad tree for a
 * given region. <Br>
 * <Br>
 *
 * <strong>Coordinate Standard: </strong><br>
 * Coordinate called posX or posZ are relative LevelPos coordinates <br>
 * unless stated otherwise. <br>
 * 
 * @author Leonardo Amato
 * @version 10-10-2021
 */
public class LodRegion {
	/** Number of detail level supported by a region */
	private static final byte POSSIBLE_LOD = 10;

	/** Holds the lowest (least detailed) detail level in this region */
	private byte minDetailLevel;
	public byte lastMaxDetailLevel = LodUtil.REGION_DETAIL_LEVEL;

	/**
	 * This holds all data for this region
	 */
	private final LevelContainer[] dataContainer;

	/** This chunk Pos has been generated */
	// private final boolean[] preGeneratedChunkPos;

	/** the generation mode for this region */
	private final DistanceGenerationMode generationMode;
	/** the vertical quality of this region */
	private final VerticalQuality verticalQuality;

	/** this region's x RegionPos */
	public final int regionPosX;
	/** this region's z RegionPos */
	public final int regionPosZ;

	public volatile int needRegenBuffer = 2;
	public volatile boolean needSaving = false;

	public LodRegion(byte minDetailLevel, RegionPos regionPos, DistanceGenerationMode generationMode,
			VerticalQuality verticalQuality) {
		this.minDetailLevel = minDetailLevel;
		this.regionPosX = regionPos.x;
		this.regionPosZ = regionPos.z;
		this.verticalQuality = verticalQuality;
		this.generationMode = generationMode;
		dataContainer = new LevelContainer[POSSIBLE_LOD];

		// Initialize all the different matrices
		for (byte lod = minDetailLevel; lod <= LodUtil.REGION_DETAIL_LEVEL; lod++) {
			dataContainer[lod] = new VerticalLevelContainer(lod);
		}
	}

	/**
	 * Inserts the data point into the region.
	 * <p>
	 * TODO this will always return true unless it has
	 * 
	 * @return true if the data was added successfully
	 */
	public boolean addData(byte detailLevel, int posX, int posZ, int verticalIndex, long data) {
		posX = LevelPosUtil.getRegionModule(detailLevel, posX);
		posZ = LevelPosUtil.getRegionModule(detailLevel, posZ);

		// The dataContainer could have null entries if the
		// detailLevel changes.
		if (this.dataContainer[detailLevel] == null)
			return false;// this.dataContainer[detailLevel] = new VerticalLevelContainer(detailLevel);

		this.dataContainer[detailLevel].addData(data, posX, posZ, verticalIndex);

		return true;
	}

	/**
	 * Inserts the vertical data into the region.
	 * <p>
	 * TODO this will always return true unless it has
	 * 
	 * @return true if the data was added successfully
	 */
	public boolean addVerticalData(byte detailLevel, int posX, int posZ, long[] data) {
		posX = LevelPosUtil.getRegionModule(detailLevel, posX);
		posZ = LevelPosUtil.getRegionModule(detailLevel, posZ);

		// The dataContainer could have null entries if the
		// detailLevel changes.
		if (this.dataContainer[detailLevel] == null)
			return false;// this.dataContainer[detailLevel] = new VerticalLevelContainer(detailLevel);

		return this.dataContainer[detailLevel].addVerticalData(data, posX, posZ);
	}

	/**
	 * Get the dataPoint at the given relative position.
	 * 
	 * @return the data at the relative pos and detail level, 0 if the data doesn't
	 *         exist.
	 */
	public long getData(byte detailLevel, int posX, int posZ, int verticalIndex) {
		posX = LevelPosUtil.getRegionModule(detailLevel, posX);
		posZ = LevelPosUtil.getRegionModule(detailLevel, posZ);
		return dataContainer[detailLevel].getData(posX, posZ, verticalIndex);
	}

	/**
	 * Get the dataPoint at the given relative position.
	 * 
	 * @return the data at the relative pos and detail level, 0 if the data doesn't
	 *         exist.
	 */
	public long getSingleData(byte detailLevel, int posX, int posZ) {
		posX = LevelPosUtil.getRegionModule(detailLevel, posX);
		posZ = LevelPosUtil.getRegionModule(detailLevel, posZ);
		return dataContainer[detailLevel].getSingleData(posX, posZ);
	}

	/**
	 * Clears the datapoint at the given relative position
	 */
	public void clear(byte detailLevel, int posX, int posZ) {
		posX = LevelPosUtil.getRegionModule(detailLevel, posX);
		posZ = LevelPosUtil.getRegionModule(detailLevel, posZ);
		dataContainer[detailLevel].clear(posX, posZ);
	}

	/**
	 * This method will fill the posToGenerate array with all levelPos that are
	 * render-able.
	 * <p>
	 * TODO why don't we return the posToGenerate, it would make this easier to
	 * understand
	 */
	public void getPosToGenerate(PosToGenerateContainer posToGenerate, int playerBlockPosX, int playerBlockPosZ,
			GenerationPriority priority) {
		getPosToGenerate(posToGenerate, LodUtil.REGION_DETAIL_LEVEL, 0, 0, playerBlockPosX, playerBlockPosZ, priority);

	}

	/**
	 * A recursive method that fills the posToGenerate array with all levelPos that
	 * need to be generated.
	 * <p>
	 * TODO why don't we return the posToGenerate, it would make this easier to
	 * understand
	 */
	private void getPosToGenerate(PosToGenerateContainer posToGenerate, byte detailLevel, int childOffsetPosX,
			int childOffsetPosZ, int playerPosX, int playerPosZ, GenerationPriority priority) {
		// equivalent to 2^(...)
		int size = 1 << (LodUtil.REGION_DETAIL_LEVEL - detailLevel);

		// calculate what LevelPos are in range to generate
		int minDistance = LevelPosUtil.minDistance(LodUtil.REGION_DETAIL_LEVEL, regionPosX, regionPosZ, playerPosX,
				playerPosZ);

		// determine this child's levelPos
		byte childDetailLevel = (byte) (detailLevel - 1);
		int childPosX = childOffsetPosX * 2;
		int childPosZ = childOffsetPosZ * 2;


		byte targetDetailLevel = DetailDistanceUtil.getGenerationDetailFromDistance(minDistance);
		if (targetDetailLevel <= detailLevel) {
			if (targetDetailLevel == detailLevel) {
				if (!doesDataExist(detailLevel, childOffsetPosX, childOffsetPosZ))
					posToGenerate.addPosToGenerate(detailLevel, childOffsetPosX + regionPosX * size,
							childOffsetPosZ + regionPosZ * size);
			} else {
				if (priority == GenerationPriority.FAR_FIRST && detailLevel >= posToGenerate.farMinDetail
						&& !doesDataExist(detailLevel, childOffsetPosX, childOffsetPosZ)) {
					posToGenerate.addPosToGenerate(detailLevel, childOffsetPosX + regionPosX * size,
							childOffsetPosZ + regionPosZ * size);
				} else if (detailLevel > LodUtil.CHUNK_DETAIL_LEVEL) {
					for (int x = 0; x <= 1; x++)
						for (int z = 0; z <= 1; z++)
							getPosToGenerate(posToGenerate, childDetailLevel, childPosX + x, childPosZ + z, playerPosX,
									playerPosZ, priority);
				} else {
					// we want at max one request per chunk (since the world generator creates
					// chunks).
					// So for lod smaller than a chunk, only recurse down
					// the top right child
					getPosToGenerate(posToGenerate, childDetailLevel, childPosX, childPosZ, playerPosX, playerPosZ,
							priority);
				}
			}
		}
		// we have gone beyond the target Detail level
		// we can stop generating

	}

	/**
	 * This method will fill the posToRender array with all levelPos that are
	 * render-able.
	 * <p>
	 * TODO why don't we return the posToRender, it would make this easier to
	 * understand
	 */
	public void getPosToRender(PosToRenderContainer posToRender, int playerPosX, int playerPosZ,
			boolean requireCorrectDetailLevel, DropoffQuality dropoffQuality) {
		int minDistance = LevelPosUtil.minDistance(LodUtil.REGION_DETAIL_LEVEL, 0, 0, playerPosX, playerPosZ, regionPosX, regionPosZ);
		byte targetLevel = DetailDistanceUtil.getDrawDetailFromDistance(minDistance);
		if (targetLevel <= dropoffQuality.fastModeSwitch) {
			getPosToRender(posToRender, LodUtil.REGION_DETAIL_LEVEL, 0, 0, playerPosX, playerPosZ,
					requireCorrectDetailLevel);
		} else {
			getPosToRenderFlat(posToRender, LodUtil.REGION_DETAIL_LEVEL, 0, 0, targetLevel, requireCorrectDetailLevel);
		}
	}

	/**
	 * This method will fill the posToRender array with all levelPos that are
	 * render-able.
	 * <p>
	 * TODO why don't we return the posToRender, it would make this easier to
	 * understand TODO this needs some more comments, James was only able to figure
	 * out part of it
	 */
	private void getPosToRender(PosToRenderContainer posToRender, byte detailLevel, int posX, int posZ, int playerPosX,
			int playerPosZ, boolean requireCorrectDetailLevel) {
		// equivalent to 2^(...)
		int size = 1 << (LodUtil.REGION_DETAIL_LEVEL - detailLevel);

		byte desiredLevel;
		int maxDistance;
		int minDistance;
		int childLevel;

		// calculate the LevelPos that are in range
		maxDistance = LevelPosUtil.maxDistance(detailLevel, posX, posZ, playerPosX, playerPosZ, regionPosX, regionPosZ);
		desiredLevel = DetailDistanceUtil.getLodDrawDetail(DetailDistanceUtil.getDrawDetailFromDistance(maxDistance));
		minDistance = LevelPosUtil.minDistance(detailLevel, posX, posZ, playerPosX, playerPosZ, regionPosX, regionPosZ);
		childLevel = DetailDistanceUtil.getLodDrawDetail(DetailDistanceUtil.getDrawDetailFromDistance(minDistance));

		if (detailLevel == childLevel - 1) {
			posToRender.addPosToRender(detailLevel, posX + regionPosX * size, posZ + regionPosZ * size);
		} else
		// if (desiredLevel > detailLevel)
		// {
		// we have gone beyond the target Detail level
		// we can stop generating
		// } else
		if (desiredLevel == detailLevel) {
			posToRender.addPosToRender(detailLevel, posX + regionPosX * size, posZ + regionPosZ * size);
		} else // case where (detailLevel > desiredLevel)
		{
			int childPosX = posX * 2;
			int childPosZ = posZ * 2;
			byte childDetailLevel = (byte) (detailLevel - 1);
			int childrenCount = 0;

			for (int x = 0; x <= 1; x++) {
				for (int z = 0; z <= 1; z++) {
					if (doesDataExist(childDetailLevel, childPosX + x, childPosZ + z)) {
						if (!requireCorrectDetailLevel)
							childrenCount++;
						else
							getPosToRender(posToRender, childDetailLevel, childPosX + x, childPosZ + z, playerPosX,
									playerPosZ, requireCorrectDetailLevel);
					}
				}
			}

			if (!requireCorrectDetailLevel) {
				// If all the four children exist go deeper
				if (childrenCount == 4) {
					for (int x = 0; x <= 1; x++)
						for (int z = 0; z <= 1; z++)
							getPosToRender(posToRender, childDetailLevel, childPosX + x, childPosZ + z, playerPosX,
									playerPosZ, requireCorrectDetailLevel);
				} else {
					posToRender.addPosToRender(detailLevel, posX + regionPosX * size, posZ + regionPosZ * size);
				}
			}
		}
	}

	/**
	 * This method will fill the posToRender array with all levelPos that are
	 * render-able. But the entire region try use the same detail level.
	 */
	private void getPosToRenderFlat(PosToRenderContainer posToRender, byte detailLevel, int posX, int posZ, byte targetLevel, boolean requireCorrectDetailLevel) {
		// equivalent to 2^(...)
		int size = 1 << (LodUtil.REGION_DETAIL_LEVEL - detailLevel);

		if (detailLevel == targetLevel) {
			posToRender.addPosToRender(detailLevel, posX + regionPosX * size, posZ + regionPosZ * size);
		} else // case where (detailLevel > desiredLevel)
		{
			int childPosX = posX * 2;
			int childPosZ = posZ * 2;
			byte childDetailLevel = (byte) (detailLevel - 1);
			int childrenCount = 0;

			for (int x = 0; x <= 1; x++) {
				for (int z = 0; z <= 1; z++) {
					if (doesDataExist(childDetailLevel, childPosX + x, childPosZ + z)) {
						if (!requireCorrectDetailLevel)
							childrenCount++;
						else
							getPosToRenderFlat(posToRender, childDetailLevel, childPosX + x, childPosZ + z, targetLevel, requireCorrectDetailLevel);
					}
				}
			}

			if (!requireCorrectDetailLevel) {
				// If all the four children exist go deeper
				if (childrenCount == 4) {
					for (int x = 0; x <= 1; x++)
						for (int z = 0; z <= 1; z++)
							getPosToRenderFlat(posToRender, childDetailLevel, childPosX + x, childPosZ + z, targetLevel, requireCorrectDetailLevel);
				} else {
					posToRender.addPosToRender(detailLevel, posX + regionPosX * size, posZ + regionPosZ * size);
				}
			}
		}
	}

	/**
	 * Updates all children.
	 * <p>
	 * TODO could this be renamed mergeArea?
	 */
	public void updateArea(byte detailLevel, int posX, int posZ) {
		int width;
		int startX;
		int startZ;

		// Update the level lower or equal to the detail level
		for (byte down = (byte) (minDetailLevel + 1); down <= detailLevel; down++) {
			startX = LevelPosUtil.convert(detailLevel, posX, down);
			startZ = LevelPosUtil.convert(detailLevel, posZ, down);
			width = 1 << (detailLevel - down);

			for (int x = 0; x < width; x++)
				for (int z = 0; z < width; z++)
					update(down, startX + x, startZ + z);
		}

		// Update the level higher than the detail level
		for (byte up = (byte) (Math.max(detailLevel, minDetailLevel) + 1); up <= LodUtil.REGION_DETAIL_LEVEL; up++) {
			update(up, LevelPosUtil.convert(detailLevel, posX, up), LevelPosUtil.convert(detailLevel, posZ, up));
		}
	}

	/**
	 * Update the child at the given relative Pos
	 * <p>
	 * TODO could this be renamed mergeChildData?
	 */
	private void update(byte detailLevel, int posX, int posZ) {
		posX = LevelPosUtil.getRegionModule(detailLevel, posX);
		posZ = LevelPosUtil.getRegionModule(detailLevel, posZ);
		dataContainer[detailLevel].updateData(dataContainer[detailLevel - 1], posX, posZ);
	}

	/**
	 * Returns if data exists at the given relative Pos.
	 */
	public boolean doesDataExist(byte detailLevel, int posX, int posZ) {
		if (detailLevel < minDetailLevel || dataContainer[detailLevel] == null)
			return false;

		posX = LevelPosUtil.getRegionModule(detailLevel, posX);
		posZ = LevelPosUtil.getRegionModule(detailLevel, posZ);

		return dataContainer[detailLevel].doesItExist(posX, posZ);
	}

	/**
	 * Gets the generation mode for the data point at the given relative pos.
	 */
	public byte getGenerationMode(byte detailLevel, int posX, int posZ) {
		if (dataContainer[detailLevel].doesItExist(posX, posZ))
			// We take the bottom information always
			// TODO what does that mean? bottom of what?
			return DataPointUtil.getGenerationMode(dataContainer[detailLevel].getSingleData(posX, posZ));
		else
			return DistanceGenerationMode.NONE.complexity;
	}

	/**
	 * Returns the lowest (least detailed) detail level in this region TODO is that
	 * right?
	 */
	public byte getMinDetailLevel() {
		return minDetailLevel;
	}

	/**
	 * Returns the LevelContainer for the detailLevel
	 * 
	 * @throws IllegalArgumentException if the detailLevel is less than
	 *                                  minDetailLevel
	 */
	public LevelContainer getLevel(byte detailLevel) {
		if (detailLevel < minDetailLevel)
			throw new IllegalArgumentException("getLevel asked for a detail level that does not exist: minimum: ["
					+ minDetailLevel + "] level requested: [" + detailLevel + "]");

		return dataContainer[detailLevel];
	}

	/**
	 * Add the levelContainer to this Region, updating the minDetailLevel if
	 * necessary.
	 * 
	 * @throws IllegalArgumentException if the LevelContainer's detailLevel is 2 or
	 *                                  more detail levels lower than the
	 *                                  minDetailLevel of this region.
	 */
	public void addLevelContainer(LevelContainer levelContainer) {
		if (levelContainer.getDetailLevel() < minDetailLevel - 1) {
			throw new IllegalArgumentException("the LevelContainer's detailLevel was " + "["
					+ levelContainer.getDetailLevel() + "] but this region "
					+ "only allows adding LevelContainers with a " + "detail level of [" + (minDetailLevel - 1) + "]");
		}

		if (levelContainer.getDetailLevel() == minDetailLevel - 1)
			minDetailLevel = levelContainer.getDetailLevel();

		dataContainer[levelContainer.getDetailLevel()] = levelContainer;
	}

	// TODO James thinks cutTree and growTree (which he renamed to match cutTree)
	// should have more descriptive names, to make sure the "Tree" portion isn't
	// confused with Minecraft trees (the plant).

	/**
	 * Removes any dataContainers that are higher than the given detailLevel
	 */
	public void cutTree(byte detailLevel) {
		if (detailLevel > minDetailLevel) {
			for (byte detailLevelIndex = 0; detailLevelIndex < detailLevel; detailLevelIndex++)
				dataContainer[detailLevelIndex] = null;

			minDetailLevel = detailLevel;
		}
	}

	/**
	 * Make this region more detailed to the detailLevel given. TODO is that
	 * correct?
	 */
	public void growTree(byte detailLevel) {
		if (detailLevel < minDetailLevel) {
			for (byte detailLevelIndex = (byte) (minDetailLevel
					- 1); detailLevelIndex >= detailLevel; detailLevelIndex--) {
				if (dataContainer[detailLevelIndex + 1] == null)
					dataContainer[detailLevelIndex + 1] = new VerticalLevelContainer((byte) (detailLevelIndex + 1));

				dataContainer[detailLevelIndex] = dataContainer[detailLevelIndex + 1].expand();
			}
			minDetailLevel = detailLevel;
		}
	}

	/**
	 * return RegionPos of this lod region
	 */
	public RegionPos getRegionPos() {
		return new RegionPos(regionPosX, regionPosZ);
	}

	/**
	 * Returns how many LODs are in this region
	 */
	public int getNumberOfLods() {
		int count = 0;
		for (LevelContainer container : dataContainer)
			count += container.getMaxNumberOfLods();

		return count;
	}

	public VerticalQuality getVerticalQuality() {
		return verticalQuality;
	}

	public DistanceGenerationMode getGenerationMode() {
		return generationMode;
	}

	public int getMaxVerticalData(byte detailLevel) {
		return dataContainer[detailLevel].getVerticalSize();
	}

	@Override
	public String toString() {
		return getLevel(LodUtil.REGION_DETAIL_LEVEL).toString();
	}
}
