/*
 *    This file is part of the Distant Horizons mod (formerly the LOD Mod),
 *    licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2020-2022  James Seibel
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

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import com.seibel.lod.core.api.ClientApi;
import com.seibel.lod.core.enums.config.DistanceGenerationMode;
import com.seibel.lod.core.enums.config.DropoffQuality;
import com.seibel.lod.core.enums.config.GenerationPriority;
import com.seibel.lod.core.enums.config.VerticalQuality;
import com.seibel.lod.core.handlers.dependencyInjection.SingletonHandler;
import com.seibel.lod.core.objects.PosToGenerateContainer;
import com.seibel.lod.core.objects.PosToRenderContainer;
import com.seibel.lod.core.util.DataPointUtil;
import com.seibel.lod.core.util.DetailDistanceUtil;
import com.seibel.lod.core.util.LevelPosUtil;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton;

// NOTE: me <3
// import com.seibel.lod.core.api.ClientApi;
import org.apache.logging.log4j.Level;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;

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
	private static final ILodConfigWrapperSingleton CONFIG = SingletonHandler.get(ILodConfigWrapperSingleton.class);
	/** Number of detail level supported by a region */
	private static final byte POSSIBLE_LOD = LodUtil.DETAIL_OPTIONS;

	/** Holds the lowest (least detailed) detail level in this region */
	private volatile byte minDetailLevel;
	public byte lastMaxDetailLevel = LodUtil.REGION_DETAIL_LEVEL;

	/**
	 * This holds all data for this region
	 */
	private final LevelContainer[] dataContainer;

	/** This chunk Pos has been generated */
	// private final boolean[] preGeneratedChunkPos;

	/** the vertical quality of this region */
	private final VerticalQuality verticalQuality;

	/** this region's x RegionPos */
	public final int regionPosX;
	/** this region's z RegionPos */
	public final int regionPosZ;

	public volatile boolean needRecheckGenPoint = true;
	// public volatile int needRegenBuffer = 2; MOVED TO RENDERREGION!
	public volatile boolean needSignalToRegenBuffer = true;
	public volatile boolean needSaving = false;
	public final AtomicInteger isWriting = new AtomicInteger(0);

	public static byte calculateFarModeSwitch(byte targetLevel) {
		if (targetLevel == 0)
			return 0; // Always use detail 0 if it's way too close
		double part = targetLevel / (double) LodUtil.REGION_DETAIL_LEVEL;
		byte farModeLevel = LodUtil.DETAIL_OPTIONS - (LodUtil.CHUNK_DETAIL_LEVEL + 1);
		farModeLevel *= part;
		farModeLevel += (LodUtil.CHUNK_DETAIL_LEVEL + 1);
		return (byte) LodUtil.clamp(LodUtil.CHUNK_DETAIL_LEVEL + 1, farModeLevel, LodUtil.DETAIL_OPTIONS - 1);
	}

	public LodRegion(byte minDetailLevel, RegionPos regionPos, VerticalQuality verticalQuality) {
		this.minDetailLevel = minDetailLevel;
		this.regionPosX = regionPos.x;
		this.regionPosZ = regionPos.z;
		this.verticalQuality = verticalQuality;
		dataContainer = new LevelContainer[POSSIBLE_LOD];

		// Initialize all the different matrices
		for (byte lod = minDetailLevel; lod <= LodUtil.REGION_DETAIL_LEVEL; lod++) {
			dataContainer[lod] = new VerticalLevelContainer(lod);
		}
	}

	/**
	 * Inserts the data point into the region.
	 * <p>
	 * 
	 * @return true if the data was added successfully
	 */
	public boolean addData(byte detailLevel, int posX, int posZ, int verticalIndex, long data) {
		if (isWriting.get() <= 0)
			throw new ConcurrentModificationException("isWriting counter lock should have been aquired!");
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
	 * 
	 * @return true if the data was added successfully
	 */
	public boolean addVerticalData(byte detailLevel, int posX, int posZ, long[] data, boolean override) {
		if (isWriting.get() <= 0)
			throw new ConcurrentModificationException("isWriting counter lock should have been aquired!");
		posX = LevelPosUtil.getRegionModule(detailLevel, posX);
		posZ = LevelPosUtil.getRegionModule(detailLevel, posZ);

		// The dataContainer could have null entries if the
		// detailLevel changes.
		if (this.dataContainer[detailLevel] == null)
			return false;// this.dataContainer[detailLevel] = new VerticalLevelContainer(detailLevel);

		boolean updated = this.dataContainer[detailLevel].addVerticalData(data, posX, posZ, override);
		if (updated) {
			needSignalToRegenBuffer = true;
			needSaving = true;
		}
		return updated;
	}

	/**
	 * Inserts the vertical data into the region.
	 * <p>
	 * 
	 * @return true if the data was added successfully
	 */
	public boolean addChunkOfData(byte detailLevel, int posX, int posZ, int widthX, int widthZ, long[] data,
			int verticalSize, boolean override) {
		if (isWriting.get() <= 0)
			throw new ConcurrentModificationException("isWriting counter lock should have been aquired!");
		posX = LevelPosUtil.getRegionModule(detailLevel, posX);
		posZ = LevelPosUtil.getRegionModule(detailLevel, posZ);

		// The dataContainer could have null entries if the
		// detailLevel changes.
		if (this.dataContainer[detailLevel] == null)
			return false;// this.dataContainer[detailLevel] = new VerticalLevelContainer(detailLevel);
		if (this.dataContainer[detailLevel].getVerticalSize() != verticalSize) {
			throw new RuntimeException(String.format(
					"Provided data's verticalSize [%d]"
							+ " is different from current storage's verticalSize [%d] at detail [%d]",
					verticalSize, this.dataContainer[detailLevel].getVerticalSize(), detailLevel));
		}
		boolean updated = this.dataContainer[detailLevel].addChunkOfData(data, posX, posZ, widthX, widthZ, override);
		// ApiShared.LOGGER.info("addChunkOfData(region:{}, level:{}, x:{}, z:{}, wx:{},
		// wz:{}, override:{}, updated:{})",
		// getRegionPos(), detailLevel, posX, posZ, widthX, widthZ, override, updated);
		if (updated) {
			needSignalToRegenBuffer = true;
			needSaving = true;
		} else {
			/*
			 * ApiShared.LOGGER.
			 * info("addChunkOfData nothing changed. Datapoint: {}\n Upper Datapoint: {}",
			 * DataPointUtil.toString(this.dataContainer[detailLevel].getSingleData(posX,
			 * posZ)), DataPointUtil.toString(this.dataContainer[9].getSingleData(0, 0)) );
			 */

		}
		if (!doesDataExist(detailLevel, posX, posZ,
				DistanceGenerationMode.values()[DataPointUtil.getGenerationMode(data[0]) - 1])) { // FIXME: -1 casue
																									// NONE has value of
																									// 1 but slot of 0
			throw new RuntimeException("Data still doesn't exist after addChunkOfData!");
		}

		return updated;
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
	public long[] getAllData(byte detailLevel, int posX, int posZ) {
		posX = LevelPosUtil.getRegionModule(detailLevel, posX);
		posZ = LevelPosUtil.getRegionModule(detailLevel, posZ);
		return dataContainer[detailLevel].getAllData(posX, posZ);
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
	 * This method will fill the posToGenerate array with all levelPos that are
	 * render-able.
	 * <p>
	 * TODO why don't we return the posToGenerate, it would make this easier to
	 * understand
	 */
	public void getPosToGenerate(PosToGenerateContainer posToGenerate, int playerBlockPosX, int playerBlockPosZ,
			GenerationPriority priority, DistanceGenerationMode genMode, boolean shouldSort) {
		getPosToGenerate(posToGenerate, LodUtil.REGION_DETAIL_LEVEL, 0, 0, playerBlockPosX, playerBlockPosZ, priority,
				genMode, shouldSort, true);

	}

	/**
	 * A recursive method that fills the posToGenerate array with all levelPos that
	 * need to be generated.
	 * <p>
	 * TODO why don't we return the posToGenerate, it would make this easier to
	 * understand FIXME This is.... absolute hell currently. Needs clean up.
	 */
	private void getPosToGenerate(PosToGenerateContainer posToGenerate, byte detailLevel, int offsetPosX,
			int offsetPosZ, int playerPosX, int playerPosZ, GenerationPriority priority, DistanceGenerationMode genMode,
			boolean shouldSort, boolean needFarPos) {
		// equivalent to 2^(...)
		int size = 1 << (LodUtil.REGION_DETAIL_LEVEL - detailLevel);

		// calculate what LevelPos are in range to generate
		double minDistance = LevelPosUtil.minDistance(detailLevel, offsetPosX + regionPosX * size,
				offsetPosZ + regionPosZ * size, playerPosX, playerPosZ);

		// determine this child's levelPos
		byte childDetailLevel = (byte) (detailLevel - 1);
		int childOffsetPosX = offsetPosX * 2;
		int childOffsetPosZ = offsetPosZ * 2;
		DistanceGenerationMode testerGenMode = genMode;// detailLevel >= LodUtil.CHUNK_DETAIL_LEVEL ?
														// DistanceGenerationMode.NONE : genMode;

		byte targetDetailLevel = DetailDistanceUtil.getDetailLevelFromDistance(minDistance);
		int farModeSwitchLevel = (priority == GenerationPriority.NEAR_FIRST) ? -1
				: calculateFarModeSwitch(targetDetailLevel);
		if (priority == GenerationPriority.FAR_FIRST)
			farModeSwitchLevel = 8;
		boolean doesDataExist = doesDataExist(detailLevel, offsetPosX + regionPosX * size,
				offsetPosZ + regionPosZ * size, testerGenMode);

		boolean isFarModeSwitchEdge = needFarPos && detailLevel <= farModeSwitchLevel;
		if (isFarModeSwitchEdge)
			needFarPos = false;

		if (targetDetailLevel >= detailLevel) {
			if (!doesDataExist) {
				if (isFarModeSwitchEdge)
					posToGenerate.addFarPosToGenerate(detailLevel, offsetPosX + regionPosX * size,
							offsetPosZ + regionPosZ * size, shouldSort);
				else
					posToGenerate.addNearPosToGenerate(detailLevel, offsetPosX + regionPosX * size,
							offsetPosZ + regionPosZ * size, shouldSort);
			}
		} else if (!doesDataExist && isFarModeSwitchEdge) {
			posToGenerate.addFarPosToGenerate(detailLevel, offsetPosX + regionPosX * size,
					offsetPosZ + regionPosZ * size, shouldSort);
		} else if (detailLevel > LodUtil.CHUNK_DETAIL_LEVEL) {
			for (int x = 0; x <= 1; x++)
				for (int z = 0; z <= 1; z++)
					getPosToGenerate(posToGenerate, childDetailLevel, childOffsetPosX + x, childOffsetPosZ + z,
							playerPosX, playerPosZ, priority, genMode, shouldSort, needFarPos);
		} else {
			getPosToGenerate(posToGenerate, childDetailLevel, childOffsetPosX, childOffsetPosZ, playerPosX, playerPosZ,
					priority, genMode, shouldSort, needFarPos);
		}
	}

	public byte getRenderDetailLevelAt(int playerPosX, int playerPosZ, byte detailLevel, int offsetX, int offsetZ) {
		GenerationPriority generationPriority = CONFIG.client().worldGenerator().getResolvedGenerationPriority();
		DropoffQuality dropoffQuality = CONFIG.client().graphics().quality().getResolvedDropoffQuality();

		double minDistance = LevelPosUtil.minDistance(LodUtil.REGION_DETAIL_LEVEL, regionPosX, regionPosZ, playerPosX,
				playerPosZ);
		byte targetLevel = DetailDistanceUtil.getDetailLevelFromDistance(minDistance);
		byte renderLevel;
		if (targetLevel > dropoffQuality.fastModeSwitch) {
			double centerDistance = LevelPosUtil.centerDistance(LodUtil.REGION_DETAIL_LEVEL, regionPosX, regionPosZ,
					playerPosX, playerPosZ);
			renderLevel = DetailDistanceUtil.getDetailLevelFromDistance(centerDistance);
		} else {
			int size = 1 << (LodUtil.REGION_DETAIL_LEVEL - detailLevel);
			double posMinDistance = LevelPosUtil.minDistance(detailLevel,
					LevelPosUtil.getRegionModule(detailLevel, offsetX) + regionPosX * size,
					LevelPosUtil.getRegionModule(detailLevel, offsetZ) + regionPosZ * size, playerPosX, playerPosZ);
			renderLevel = DetailDistanceUtil.getDetailLevelFromDistance(posMinDistance);
		}
		return (byte) Math.max(getMinDetailLevel(), renderLevel);
	}

	public void getPosToRender(PosToRenderContainer posToRender, int playerPosX, int playerPosZ) {
		// use FAR_FIRST on local worlds and NEAR_FIRST on servers
		GenerationPriority generationPriority = CONFIG.client().worldGenerator().getResolvedGenerationPriority();

		DropoffQuality dropoffQuality = CONFIG.client().graphics().quality().getResolvedDropoffQuality();

		getPosToRender(posToRender, playerPosX, playerPosZ, generationPriority, dropoffQuality);
	}

	/**
	 * This method will fill the posToRender array with all levelPos that are
	 * render-able.
	 * <p>
	 * TODO why don't we return the posToRender, it would make this easier to
	 * understand
	 */
	private void getPosToRender(PosToRenderContainer posToRender, int playerPosX, int playerPosZ,
			GenerationPriority priority, DropoffQuality dropoffQuality) {
		double minDistance = LevelPosUtil.minDistance(LodUtil.REGION_DETAIL_LEVEL, regionPosX, regionPosZ, playerPosX,
				playerPosZ);
		byte targetLevel = DetailDistanceUtil.getDetailLevelFromDistance(minDistance);
		if (targetLevel <= dropoffQuality.fastModeSwitch) {
			getPosToRender(posToRender, LodUtil.REGION_DETAIL_LEVEL, 0, 0, playerPosX, playerPosZ, priority);
		} else {
			// FarModeSwitchLevel or above is the level where a giant block of lod is not
			// acceptable even if not all child data exist.
			double centerDistance = LevelPosUtil.centerDistance(LodUtil.REGION_DETAIL_LEVEL, regionPosX, regionPosZ,
					playerPosX, playerPosZ);
			targetLevel = DetailDistanceUtil.getDetailLevelFromDistance(centerDistance);
			byte farModeSwitchLevel = (priority == GenerationPriority.NEAR_FIRST) ? 0
					: calculateFarModeSwitch(targetLevel);
			if (priority == GenerationPriority.FAR_FIRST)
				farModeSwitchLevel = 8;
			getPosToRenderFlat(posToRender, LodUtil.REGION_DETAIL_LEVEL, 0, 0, targetLevel, farModeSwitchLevel);
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
	private void getPosToRender(PosToRenderContainer posToRender, byte detailLevel, int offsetPosX, int offsetPosZ,
			int playerPosX, int playerPosZ, GenerationPriority priority) {
		// equivalent to 2^(...)
		int size = 1 << (LodUtil.REGION_DETAIL_LEVEL - detailLevel);

		// calculate the LevelPos that are in range
		double minDistance = LevelPosUtil.minDistance(detailLevel, offsetPosX + regionPosX * size,
				offsetPosZ + regionPosZ * size, playerPosX, playerPosZ);
		byte minLevel = DetailDistanceUtil.getDetailLevelFromDistance(minDistance);
		// FarModeSwitchLevel or above is the level where a giant block of lod is not
		// acceptable even if not all child data exist.
		byte farModeSwitchLevel = (priority == GenerationPriority.NEAR_FIRST) ? 0 : calculateFarModeSwitch(minLevel);
		if (priority == GenerationPriority.FAR_FIRST)
			farModeSwitchLevel = 8;

		if (detailLevel <= minLevel) {
			posToRender.addPosToRender(detailLevel, offsetPosX + regionPosX * size, offsetPosZ + regionPosZ * size);
		} else // case where (detailLevel > desiredLevel)
		{
			int childPosX = (offsetPosX + regionPosX * size) * 2;
			int childPosZ = (offsetPosZ + regionPosZ * size) * 2;
			byte childDetailLevel = (byte) (detailLevel - 1);

			if (detailLevel > farModeSwitchLevel) {
				// Giant block is not acceptable. So leave empty void if data doesn't exist.
				for (int x = 0; x <= 1; x++) {
					for (int z = 0; z <= 1; z++) {
						if (doesDataExist(childDetailLevel, childPosX + x, childPosZ + z,
								DistanceGenerationMode.NONE)) {
							getPosToRender(posToRender, childDetailLevel, offsetPosX * 2 + x, offsetPosZ * 2 + z,
									playerPosX, playerPosZ, priority);
						}
					}
				}
			} else {
				// Giant block is acceptable. So use this level lod if not all child data exist.
				int childrenCount = 0;
				for (int x = 0; x <= 1; x++) {
					for (int z = 0; z <= 1; z++) {
						if (doesDataExist(childDetailLevel, childPosX + x, childPosZ + z,
								DistanceGenerationMode.NONE)) {
							childrenCount++;
						}
					}
				}
				// If all the four children exist go deeper
				if (childrenCount == 4) {
					for (int x = 0; x <= 1; x++)
						for (int z = 0; z <= 1; z++)
							getPosToRender(posToRender, childDetailLevel, offsetPosX * 2 + x, offsetPosZ * 2 + z,
									playerPosX, playerPosZ, priority);
				} else {
					posToRender.addPosToRender(detailLevel, offsetPosX + regionPosX * size,
							offsetPosZ + regionPosZ * size);
				}
			}
		}
	}

	/**
	 * This method will fill the posToRender array with all levelPos that are
	 * render-able. But the entire region try use the same detail level.
	 */
	private void getPosToRenderFlat(PosToRenderContainer posToRender, byte detailLevel, int offsetPosX, int offsetPosZ,
			byte targetLevel, byte farModeSwitchLevel) {
		// equivalent to 2^(...)
		int size = 1 << (LodUtil.REGION_DETAIL_LEVEL - detailLevel);
		if (detailLevel == targetLevel) {
			posToRender.addPosToRender(detailLevel, offsetPosX + regionPosX * size, offsetPosZ + regionPosZ * size);
		} else // case where (detailLevel > desiredLevel)
		{
			int childPosX = (offsetPosX + regionPosX * size) * 2;
			int childPosZ = (offsetPosZ + regionPosZ * size) * 2;
			byte childDetailLevel = (byte) (detailLevel - 1);

			if (detailLevel > farModeSwitchLevel) {
				// Giant block is not acceptable. So leave empty void if data doesn't exist.
				for (int x = 0; x <= 1; x++) {
					for (int z = 0; z <= 1; z++) {
						if (doesDataExist(childDetailLevel, childPosX + x, childPosZ + z,
								DistanceGenerationMode.NONE)) {
							getPosToRenderFlat(posToRender, childDetailLevel, offsetPosX * 2 + x, offsetPosZ * 2 + z,
									targetLevel, farModeSwitchLevel);
						}
					}
				}
			} else {
				// Giant block is acceptable. So use this level lod if not all child data exist.
				int childrenCount = 0;
				for (int x = 0; x <= 1; x++) {
					for (int z = 0; z <= 1; z++) {
						if (doesDataExist(childDetailLevel, childPosX + x, childPosZ + z,
								DistanceGenerationMode.RENDERABLE)) {
							childrenCount++;
						}
					}
				}
				// If all the four children exist go deeper
				if (childrenCount == 4) {
					for (int x = 0; x <= 1; x++)
						for (int z = 0; z <= 1; z++)
							getPosToRenderFlat(posToRender, childDetailLevel, offsetPosX * 2 + x, offsetPosZ * 2 + z,
									targetLevel, farModeSwitchLevel);
				} else {
					posToRender.addPosToRender(detailLevel, offsetPosX + regionPosX * size,
							offsetPosZ + regionPosZ * size);
				}
			}
		}
	}

	public static final class LevelPos {
		public final byte detail;
		public final int posX;
		public final int posZ;

		LevelPos(byte d, int x, int z) {
			detail = d;
			posX = x;
			posZ = z;
		}
	}

	public Iterator<LevelPos> posToRenderIterator() {
		return new Iterator<LevelPos>() {
			final byte minDetail = minDetailLevel;
			int offsetX = 0;
			int offsetZ = 0;
			final int size = 1 << (LodUtil.REGION_DETAIL_LEVEL - minDetail);

			private void advance() {

			}

			@Override
			public boolean hasNext() {

				return (offsetZ >= size);
			}

			@Override
			public LevelPos next() {

				// TODO Auto-generated method stub
				return null;
			}

		};

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
		needSignalToRegenBuffer = true;
	}

	public boolean regenerateLodFromArea(byte detailLevel, int posX, int posZ, int widthX, int widthZ) {
		if (detailLevel >= LodUtil.REGION_DETAIL_LEVEL)
			return false;
		int modPosX = LevelPosUtil.getRegionModule(detailLevel, posX);
		int modPosZ = LevelPosUtil.getRegionModule(detailLevel, posZ);
		// ApiShared.LOGGER.info("RegenerateLodFromArea(region:{} level:{}, x:{}, z:{},
		// wx:{}, wz:{})",
		// getRegionPos(), detailLevel, modPosX, modPosZ, widthX, widthZ);
		if (detailLevel < minDetailLevel) {
			byte startLevel = minDetailLevel;
			int maxPosX = Math.floorDiv(modPosX + widthX - 1, (1 << (minDetailLevel - startLevel))) + 1;
			int maxPosZ = Math.floorDiv(modPosZ + widthZ - 1, (1 << (minDetailLevel - startLevel))) + 1;
			modPosX = Math.floorDiv(modPosX, (1 << (minDetailLevel - startLevel)));
			modPosZ = Math.floorDiv(modPosZ, (1 << (minDetailLevel - startLevel)));
			widthX = maxPosX - modPosX;
			widthZ = maxPosZ - modPosZ;
			detailLevel = minDetailLevel;
		}
		do {
			int maxPosX = Math.floorDiv(modPosX + widthX - 1, 2) + 1;
			int maxPosZ = Math.floorDiv(modPosZ + widthZ - 1, 2) + 1;
			modPosX = Math.floorDiv(modPosX, 2);
			modPosZ = Math.floorDiv(modPosZ, 2);
			widthX = maxPosX - modPosX;
			widthZ = maxPosZ - modPosZ;
			detailLevel++;
			// ApiShared.LOGGER.info(" - Shink: (level:{}, x:{}, z:{}, wx:{}, wz:{})",
			// detailLevel, modPosX, modPosZ, widthX, widthZ);
			chunkUpdate(detailLevel, modPosX, modPosZ, widthX, widthZ);
		} while (detailLevel < LodUtil.REGION_DETAIL_LEVEL);

		needSignalToRegenBuffer = true;
		return true;
	}

	/**
	 * Update the child at the given relative Pos
	 * <p>
	 * TODO could this be renamed mergeChildData? TODO make this return whether any
	 * value has changed
	 */

	private void update(byte detailLevel, int modPosX, int modPosZ) {
		// ApiShared.LOGGER.info(" - Update: (level:{}, subLevel:{}, mx:{}, mz:{})",
		// detailLevel, detailLevel-1, modPosX, modPosZ);
		dataContainer[detailLevel].updateData(dataContainer[detailLevel - 1], modPosX, modPosZ);
	}

	private void chunkUpdate(byte detailLevel, int modPosX, int modPosZ, int widthX, int widthZ) {
		for (int ox = 0; ox < widthX; ox++) {
			for (int oz = 0; oz < widthZ; oz++) {
				update(detailLevel, modPosX + ox, modPosZ + oz);
			}
		}
	}

	/**
	 * Returns if data exists at the given relative Pos.
	 */
	public boolean doesDataExist(byte detailLevel, int posX, int posZ, DistanceGenerationMode requiredMode) {
		if (detailLevel < minDetailLevel || dataContainer[detailLevel] == null)
			return false;

		int modPosX = LevelPosUtil.getRegionModule(detailLevel, posX);
		int modPosZ = LevelPosUtil.getRegionModule(detailLevel, posZ);
		if (!dataContainer[detailLevel].doesItExist(modPosX, modPosZ))
			return false;
		if (requiredMode == DistanceGenerationMode.NONE)
			return true;
		byte mode = getGenerationMode(detailLevel, posX, posZ);
		return (mode >= requiredMode.complexity);
	}

	/**
	 * Gets the generation mode for the data point at the given relative pos.
	 */
	public byte getGenerationMode(byte detailLevel, int posX, int posZ) {

		int modPosX = LevelPosUtil.getRegionModule(detailLevel, posX);
		int modPosZ = LevelPosUtil.getRegionModule(detailLevel, posZ);
		if (dataContainer[detailLevel] != null && dataContainer[detailLevel].doesItExist(modPosX, modPosZ))
			// We take the bottom information always
			// TODO what does that mean? bottom of what?
			return DataPointUtil.getGenerationMode(dataContainer[detailLevel].getSingleData(modPosX, modPosZ));
		else
			throw new RuntimeException("Data does not exist!");
	}

	/**
	 * Returns the lowest (least detailed) detail level in this region
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

		dataContainer[levelContainer.getDetailLevel()] = levelContainer;
		if (levelContainer.getDetailLevel() == minDetailLevel - 1)
			minDetailLevel = levelContainer.getDetailLevel();

		needRecheckGenPoint = true;
	}

	// TODO James thinks cutTree and growTree (which he renamed to match cutTree)
	// should have more descriptive names, to make sure the "Tree" portion isn't
	// confused with Minecraft trees (the plant).

	/**
	 * Removes any dataContainers that are higher than the given detailLevel
	 */
	public void cutTree(byte detailLevel) {
		if (detailLevel > minDetailLevel) {
			minDetailLevel = detailLevel;
			for (byte detailLevelIndex = 0; detailLevelIndex < detailLevel; detailLevelIndex++)
				dataContainer[detailLevelIndex] = null;
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
			needRecheckGenPoint = true;
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

	public int getMaxVerticalData(byte detailLevel) {
		return dataContainer[detailLevel].getVerticalSize();
	}

	public LevelContainer[] debugGetDataContainers() {
		return dataContainer;
	}

	@Override
	public String toString() {
		return getLevel(LodUtil.REGION_DETAIL_LEVEL).toString();
	}
}
