/*
 *    This file is part of the Distant Horizons mod (formerly the LOD Mod),
 *    licensed under the GNU LGPL v3 License.
 *
 *    Copyright (C) 2020-2022  James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.seibel.lod.core.objects.lod;

import com.seibel.lod.core.api.internal.ClientApi;
import com.seibel.lod.core.config.Config;
import com.seibel.lod.core.enums.config.EDistanceGenerationMode;
import com.seibel.lod.core.enums.config.EDropoffQuality;
import com.seibel.lod.core.enums.config.EGenerationPriority;
import com.seibel.lod.core.enums.config.EVerticalQuality;
import com.seibel.lod.core.handlers.LodDimensionFileHandler;
import com.seibel.lod.core.handlers.dependencyInjection.SingletonHandler;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.logging.SpamReducedLogger;
import com.seibel.lod.core.objects.Pos2D;
import com.seibel.lod.core.objects.PosToGenerateContainer;
import com.seibel.lod.core.objects.DHRegionPos;
import com.seibel.lod.core.util.*;
import com.seibel.lod.core.util.gridList.MovableGridRingList;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IDimensionTypeWrapper;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


//FIXME: Race condition on lodDim move/resize!

/**
 * This object holds all loaded LOD regions
 * for a given dimension. <Br><Br>
 *
 * <strong>Coordinate Standard: </strong><br>
 * Coordinate called posX or posZ are relative LevelPos coordinates <br>
 * unless stated otherwise. <br>
 * 
 * @author Leonardo Amato
 * @author James Seibel
 * @version 2022-3-26
 */
public class LodDimension
{
	private static final IMinecraftClientWrapper MC = SingletonHandler.get(IMinecraftClientWrapper.class);
	private static final Logger LOGGER = DhLoggerBuilder.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());
	
	public final IDimensionTypeWrapper dimension;
	
	/** measured in regions */
	private volatile int width;
	/** measured in regions */
	private volatile int halfWidth;
	
	// these three variables are private to force use of the getWidth() method
	// which is a safer way to get the width then directly asking the arrays
	/** stores all the regions in this dimension */
	public MovableGridRingList<LodRegion> regions;
	//NOTE: This list pos is relative to center
	private volatile DHRegionPos[] iteratorList = null;
	
	
	private LodDimensionFileHandler fileHandler = null;
	public boolean isFileHandlerNull() { return this.fileHandler == null; }
	
	public volatile int dirtiedRegionsRoughCount = 0;
	
	private boolean isCutting = false;
	private boolean isExpanding = false;
	
	private final ExecutorService cutAndExpandThread = Executors.newSingleThreadExecutor(
			new LodThreadFactory(this.getClass().getSimpleName() + " - Cut and Expand", Thread.NORM_PRIORITY - 1));
	
	private boolean logEvents = true;
	
	
	
	/**
	 * Creates the dimension centered at (0,0), with event logging, and no file saving/loading.
	 *
	 * @param newWidth measured in regions
	 */
	public LodDimension(IDimensionTypeWrapper newDimension, int newWidth)
	{
		this(newDimension, newWidth, null, true);
	}
	
	/**
	 * Creates the dimension centered at (0,0)
	 *
	 * @param newWidth measured in regions
	 * @param saveDir can be null. If null regions will not be saved or loaded from file.
	 */
	public LodDimension(IDimensionTypeWrapper newDimension, int newWidth, File saveDir)
	{
		this(newDimension, newWidth, saveDir, true);
	}
	
	/**
	 * Creates the dimension centered at (0,0)
	 *
	 * @param newWidth measured in regions
	 * @param saveDir can be null. If null regions will not be saved or loaded from file.
	 */
	public LodDimension(IDimensionTypeWrapper newDimension, int newWidth, File saveDir, boolean newLogEvents)
	{
		this.dimension = newDimension;
		this.width = newWidth;  // FIXME any width besides 1 causes an indexOutOfBounds Exception
		this.halfWidth = width / 2;
		this.logEvents = newLogEvents;
		
		if (saveDir != null)
			this.fileHandler =  new LodDimensionFileHandler(saveDir, this);
		
		this.regions = new MovableGridRingList<LodRegion>(halfWidth, 0, 0);
		generateIteratorList();
	}
	
	
	
	private void generateIteratorList()
	{
		iteratorList = null;
		DHRegionPos[] list = new DHRegionPos[width*width];
		
		int i = 0;
		for (int ix=-halfWidth; ix<=halfWidth; ix++) {
			for (int iz=-halfWidth; iz<=halfWidth; iz++) {
				list[i] = new DHRegionPos(ix, iz);
				i++;
			}
		}
		Arrays.sort(list, (a, b) -> {
			double disSqrA = a.x* a.x+ a.z* a.z;
			double disSqrB = b.x* b.x+ b.z* b.z;
			return Double.compare(disSqrA, disSqrB);
		});
		iteratorList = list;
	}
	
	
	
	//FIXME: Race condition on this move and other reading regions!
	/**
	 * Move the center of this LodDimension and move all owned
	 * regions over by the given x and z offset. <br><br>
	 * <p>
	 * Synchronized to prevent multiple moves happening on top of each other.
	 */
	public synchronized void move(DHRegionPos regionOffset)
	{
		if (this.logEvents)
			LOGGER.info("LodDim MOVE. Offset: "+regionOffset);
		
		saveDirtyRegionsToFile(false); //async add dirty regions to be saved.
		Pos2D p = regions.getCenter();
		regions.move(p.x+regionOffset.x, p.y+regionOffset.z);
		
		if (this.logEvents)
			LOGGER.info("LodDim MOVE complete. Offset: "+regionOffset);
	}
	
	
	/**
	 * Gets the region at the given LevelPos
	 * <br>
	 * Returns null if the region doesn't exist
	 * or is outside the loaded area.
	 */
	public LodRegion getRegion(byte detailLevel, int levelPosX, int levelPosZ)
	{
		int xRegion = LevelPosUtil.getRegion(detailLevel, levelPosX);
		int zRegion = LevelPosUtil.getRegion(detailLevel, levelPosZ);
		LodRegion region = regions.get(xRegion, zRegion);
		
		if (region != null && region.getMinDetailLevel() > detailLevel)
			return null;
		//throw new InvalidParameterException("Region for level pos " + LevelPosUtil.toString(detailLevel, posX, posZ) + " currently only reach level " + regions[xIndex][zIndex].getMinDetailLevel());
		
		return region;
	}
	
	/**
	 * Gets the region at the given X and Z
	 * <br>
	 * Returns null if the region doesn't exist
	 * or is outside the loaded area.
	 */
	public LodRegion getRegion(int regionPosX, int regionPosZ)
	{
		return regions.get(regionPosX, regionPosZ);
	}
	
	/** Useful when iterating over every region. */
	@Deprecated
	public LodRegion getRegionByArrayIndex(int xIndex, int zIndex)
	{
		Pos2D p = regions.getMinInRange();
		return regions.get(p.x+xIndex, p.y+zIndex);
	}
	
	/**
	 * Overwrite the LodRegion at the location of newRegion with newRegion.
	 * @throws ArrayIndexOutOfBoundsException if newRegion is outside what can be stored in this LodDimension.
	 */
	/*public synchronized void addOrOverwriteRegion(LodRegion newRegion) throws ArrayIndexOutOfBoundsException
	{
		if (!regionIsInRange(newRegion.regionPosX, newRegion.regionPosZ))
			// out of range
			throw new ArrayIndexOutOfBoundsException("Region " + newRegion.regionPosX + ", " + newRegion.regionPosZ + " out of range");
		regions[xIndex][zIndex] = newRegion;
	}*/
	
	public interface PosConsumer {
		void run(int x, int z);
	}
	
	public void iterateWithSpiral(PosConsumer r) {
		int ox,oy,dx,dy;
	    ox = oy = dx = 0;
	    dy = -1;
	    int len = regions.getSize();
	    int maxI = len*len;
	    int halfLen = len/2;
	    for(int i =0; i < maxI; i++){
	        if ((-halfLen <= ox) && (ox <= halfLen) && (-halfLen <= oy) && (oy <= halfLen)){
	        	int x = ox+halfLen;
	        	int z = oy+halfLen;
	        	r.run(x, z);
	        }
	        if( (ox == oy) || ((ox < 0) && (ox == -oy)) || ((ox > 0) && (ox == 1-oy))){
	            int temp = dx;
	            dx = -dy;
	            dy = temp;
	        }
	        ox += dx;
	        oy += dy;
	    }
	}
	public void iterateByDistance(PosConsumer r) {
		if (iteratorList==null) return;
		for (DHRegionPos relativePos : iteratorList) {
			r.run(relativePos.x+halfWidth, relativePos.z+halfWidth);
		}
		
	}
	
	
	/**
	 * Deletes nodes that are a higher detail then necessary, freeing
	 * up memory.
	 */
	private int totalDirtiedRegions = 0;
	
	public void cutRegionNodesAsync(int playerPosX, int playerPosZ)
	{
		if (isCutting) return;
		isCutting = true;
		// don't run the tree cutter multiple times
		// for the same location
		Runnable thread = () -> {
			//ApiShared.LOGGER.info("LodDim cut Region: " + playerPosX + "," + playerPosZ);
			totalDirtiedRegions = 0;
			Pos2D minPos = regions.getMinInRange();
			// go over every region in the dimension
			iterateWithSpiral((int x, int z) -> {
				double minDistance;
				byte detail;
				
				LodRegion region = regions.get(x+minPos.x, z+minPos.y);
				if (region != null && region.needSaving) totalDirtiedRegions++;
				if (region != null && !region.needSaving && region.isWriting.get()==0) {
					// check what detail level this region should be
					// and cut it if it is higher then that
					minDistance = LevelPosUtil.minDistance(LodUtil.REGION_DETAIL_LEVEL, x+minPos.x, z+minPos.y,
							playerPosX, playerPosZ);
					detail = DetailDistanceUtil.getDetailLevelFromDistance(minDistance);
					if (region.getMinDetailLevel() < detail) {
						if (region.needSaving) return; // FIXME: A crude attempt at lowering chance of race condition!
						if (region.isWriting.get()!=0) return;
						region.cutTree(detail);
						region.needSignalToRegenBuffer = true;
					}
				}
				if (region != null && region.needSignalToRegenBuffer) {
					region.needSignalToRegenBuffer = false;
					ClientApi.lodBufferBuilderFactory.setRegionNeedRegen(x+minPos.x, z+minPos.y);
				}
				
			});
			if (totalDirtiedRegions > 8) this.saveDirtyRegionsToFile(false);
			dirtiedRegionsRoughCount = totalDirtiedRegions;
			//ApiShared.LOGGER.info("LodDim cut Region complete: " + playerPosX + "," + playerPosZ);
			isCutting = false;
			
			// See if we need to save and flush some data out.
		};
		cutAndExpandThread.execute(thread);
	}

	private boolean expandOrLoadPaused = false;
	/** Either expands or loads all regions in the rendered LOD area */
	public void expandOrLoadRegionsAsync(int playerPosX, int playerPosZ) {

		if (isExpanding) return;
		// If we have less than 20% or 128MB ram left. Don't expend.
		if (expandOrLoadPaused)
		{
			if (LodUtil.checkRamUsage(0.2, 128))
			{
				if (this.logEvents)
					LOGGER.info("Enough ram for expandOrLoadThread. Restarting...");
				
				expandOrLoadPaused = false;
			}
		}
		isExpanding = true;
		
		EVerticalQuality verticalQuality = Config.Client.Graphics.Quality.verticalQuality.get();
		EDropoffQuality dropoffQuality = Config.Client.Graphics.Quality.dropoffQuality.get();
		if (dropoffQuality == EDropoffQuality.AUTO)
			dropoffQuality = Config.Client.Graphics.Quality.lodChunkRenderDistance.get() < 128 ?
					EDropoffQuality.SMOOTH_DROPOFF : EDropoffQuality.PERFORMANCE_FOCUSED;
		int dropoffSwitch = dropoffQuality.fastModeSwitch;
		// don't run the expander multiple times
		// for the same location
		Runnable thread = () ->
		{
			//ApiShared.LOGGER.info("LodDim expend Region: " + playerPosX + "," + playerPosZ);
			Pos2D minPos = regions.getMinInRange();
			iterateWithSpiral((int x, int z) ->
			{
				if (!expandOrLoadPaused && !LodUtil.checkRamUsage(0.02, 64))
				{
					Runtime.getRuntime().gc();
					if (!LodUtil.checkRamUsage(0.2, 128))
					{
						if (this.logEvents)
							LOGGER.warn("Not enough ram for expandOrLoadThread. Pausing until Ram is freed...");
						
						// We have less than 10% or 64MB ram left. Don't expend.
						expandOrLoadPaused = true;
						saveDirtyRegionsToFile(false);
					}
				}
				
				int regionX;
				int regionZ;
				LodRegion region;
				double minDistance;
				double maxDistance;
				byte minDetail;
				byte maxDetail;
				regionX = x + minPos.x;
				regionZ = z + minPos.y;
				final DHRegionPos regionPos = new DHRegionPos(regionX, regionZ);
				region = regions.get(regionX, regionZ);
				if (region != null && region.isWriting.get()!=0) return; // FIXME: A crude attempt at lowering chance of race condition!

				minDistance = LevelPosUtil.minDistance(LodUtil.REGION_DETAIL_LEVEL, regionX, regionZ, playerPosX,
						playerPosZ);
				maxDistance = LevelPosUtil.maxDistance(LodUtil.REGION_DETAIL_LEVEL, regionX, regionZ, playerPosX,
						playerPosZ);
				{
					double debugRPosX = LevelPosUtil.convert(LodUtil.REGION_DETAIL_LEVEL, regionX, (byte) 0) + LodUtil.REGION_WIDTH/2;
					double debugRPosZ = LevelPosUtil.convert(LodUtil.REGION_DETAIL_LEVEL, regionZ, (byte) 0) + LodUtil.REGION_WIDTH/2;
					double deltaRPosX = debugRPosX - playerPosX;
					double deltaRPosZ = debugRPosZ - playerPosZ;
					double debugDistance = Math.sqrt(deltaRPosX*deltaRPosX + deltaRPosZ*deltaRPosZ);
					if (minDistance > debugDistance || maxDistance < debugDistance || minDistance > maxDistance)
					{
						if (this.logEvents)
						{
							LOGGER.error("MinDistance/MaxDistance is WRONG!!! minDist: [{}], maxDist: [{}], centerDist: [{}]\n"
											+ "At center block pos: {} {}, region pos: {}",
									minDistance, maxDistance, debugDistance, debugRPosX, debugRPosZ, regionPos);
						}
						
						return;
					}
				}
				minDetail = DetailDistanceUtil.getDetailLevelFromDistance(minDistance);
				maxDetail = DetailDistanceUtil.getDetailLevelFromDistance(maxDistance);
				boolean updated = false;
				if (region == null) {
					if ((!expandOrLoadPaused)) {
						region = getRegionFromFile(regionPos, minDetail, verticalQuality);
						regions.set(regionX, regionZ, region);
						updated = true;
					}
				} else if (region.getVerticalQuality() != verticalQuality ||
						region.getMinDetailLevel() > minDetail) {
					// The 'getRegionFromFile' will flush and save the region if it returns a new one
					if ((!expandOrLoadPaused)) {
						region = getRegionFromFile(region, minDetail, verticalQuality);
						regions.set(regionX, regionZ, region);
						updated = true;
					}
				} else if (minDetail <= dropoffSwitch && region.lastMaxDetailLevel != maxDetail) {
					region.lastMaxDetailLevel = maxDetail;
					updated = true;
				} else if (minDetail <= dropoffSwitch && region.lastMaxDetailLevel != region.getMinDetailLevel()) {
					updated = true;
				}
				if (updated) {
					region.needSignalToRegenBuffer = true;
					region.needRecheckGenPoint = true;
				}
				if (region != null && region.needSignalToRegenBuffer) {
					region.needSignalToRegenBuffer = false;
					ClientApi.lodBufferBuilderFactory.setRegionNeedRegen(x+minPos.x, z+minPos.y);
				}
			});
			//ApiShared.LOGGER.info("LodDim expend Region complete: " + playerPosX + "," + playerPosZ);
			isExpanding = false;
		};

		cutAndExpandThread.execute(thread);
	}
	
	/**
	 * Add whole column of LODs to this dimension at the coordinate
	 * stored in the LOD. If an LOD already exists at the given
	 * coordinate it will be overwritten.
	 */
	public Boolean addVerticalData(byte detailLevel, int posX, int posZ, long[] data, boolean override)
	{
		int regionPosX = LevelPosUtil.getRegion(detailLevel, posX);
		int regionPosZ = LevelPosUtil.getRegion(detailLevel, posZ);
		
		// don't continue if the region can't be saved
		LodRegion region = getRegion(regionPosX, regionPosZ);
		if (region == null)
			return false;
		
		boolean nodeAdded = region.addVerticalData(detailLevel, posX, posZ, data, override);
		return nodeAdded;
	}
	
	/**
	 * Returns every position that need to be generated based on the position of the player
	 */
	public PosToGenerateContainer getPosToGenerate(int maxDataToGenerate, int playerBlockPosX, int playerBlockPosZ,
			EGenerationPriority priority, EDistanceGenerationMode genMode)
	{
		PosToGenerateContainer posToGenerate;
		posToGenerate = new PosToGenerateContainer(maxDataToGenerate, playerBlockPosX, playerBlockPosZ);
		
		
		// This ensures that we don't spawn way too many regions without finish flushing them first.
		//if (dirtiedRegionsRoughCount > 16) return posToGenerate;
		EGenerationPriority allowedPriority = dirtiedRegionsRoughCount>12 ? EGenerationPriority.NEAR_FIRST : priority;
		Pos2D minPos = regions.getMinInRange();
		iterateByDistance((int x, int z) -> {
			boolean isCloseRange = (Math.abs(x-halfWidth)+Math.abs(z-halfWidth)<=2);
			//boolean isCloseRange = true;
			//All of this is handled directly by the region, which scan every pos from top to bottom of the quad tree
			LodRegion lodRegion = regions.get(minPos.x+x, minPos.y+z);
			
			
			if (lodRegion != null && lodRegion.needRecheckGenPoint) {
				int nearCount = posToGenerate.getNumberOfNearPos();
				int farCount = posToGenerate.getNumberOfFarPos();
				boolean checkForFlag = (nearCount < posToGenerate.getMaxNumberOfNearPos() && farCount < posToGenerate.getMaxNumberOfFarPos());
				if (checkForFlag) {
					lodRegion.needRecheckGenPoint = false;
				}
				lodRegion.getPosToGenerate(posToGenerate, playerBlockPosX, playerBlockPosZ, allowedPriority, genMode,
						isCloseRange);
				if (checkForFlag) {
					if (nearCount != posToGenerate.getNumberOfNearPos() || farCount != posToGenerate.getNumberOfFarPos()) {
						lodRegion.needRecheckGenPoint = true;
					}
				}
			}
		});
	return posToGenerate;
	}

	/**
	 * Determines how many vertical LODs could be used
	 * for the given region at the given detail level
	 */
	public int getMaxVerticalData(byte detailLevel, int posX, int posZ)
	{
		if (detailLevel > LodUtil.REGION_DETAIL_LEVEL)
			throw new IllegalArgumentException("getMaxVerticalData given a level of [" + detailLevel + "] when [" + LodUtil.REGION_DETAIL_LEVEL + "] is the max.");
		
		LodRegion region = getRegion(detailLevel, posX, posZ);
		if (region == null)
			return 0;
		
		return region.getMaxVerticalData(detailLevel);
	}
	
	/**
	 * Get the data point at the given X and Z coordinates
	 * in this dimension.
	 * <br>
	 * Returns null if the LodChunk doesn't exist or
	 * is outside the loaded area.
	 */
	public long getData(byte detailLevel, int posX, int posZ, int verticalIndex)
	{
		if (detailLevel > LodUtil.REGION_DETAIL_LEVEL)
			throw new IllegalArgumentException("getLodFromCoordinates given a level of \"" + detailLevel + "\" when \"" + LodUtil.REGION_DETAIL_LEVEL + "\" is the max.");
		
		LodRegion region = getRegion(detailLevel, posX, posZ);
		if (region == null)
			return DataPointUtil.EMPTY_DATA;
		
		return region.getData(detailLevel, posX, posZ, verticalIndex);
	}
	
	/**
	 * Get the data point at the given X and Z coordinates
	 * in this dimension.
	 * <br>
	 * Returns null if the LodChunk doesn't exist or
	 * is outside the loaded area.
	 */
	public long[] getAllData(byte detailLevel, int posX, int posZ)
	{
		if (detailLevel > LodUtil.REGION_DETAIL_LEVEL)
			throw new IllegalArgumentException("getLodFromCoordinates given a level of \"" + detailLevel + "\" when \"" + LodUtil.REGION_DETAIL_LEVEL + "\" is the max.");
		
		LodRegion region = getRegion(detailLevel, posX, posZ);
		if (region == null)
			return null;
		
		return region.getAllData(detailLevel, posX, posZ);
	}
	
	
	/**
	 * Get the data point at the given X and Z coordinates
	 * in this dimension.
	 * <br>
	 * Returns null if the LodChunk doesn't exist or
	 * is outside the loaded area.
	 */
	public long getSingleData(byte detailLevel, int posX, int posZ)
	{
		if (detailLevel > LodUtil.REGION_DETAIL_LEVEL)
			throw new IllegalArgumentException("getLodFromCoordinates given a level of \"" + detailLevel + "\" when \"" + LodUtil.REGION_DETAIL_LEVEL + "\" is the max.");
		
		LodRegion region = getRegion(detailLevel, posX, posZ);
		if (region == null)
			return DataPointUtil.EMPTY_DATA;
		
		return region.getSingleData(detailLevel, posX, posZ);
	}
	
	/**
	 * Get the data point at the given LevelPos
	 * in this dimension.
	 * <br>
	 * Returns null if the LodChunk doesn't exist or
	 * is outside the loaded area.
	 */
	public void updateData(byte detailLevel, int posX, int posZ)
	{
		if (detailLevel > LodUtil.REGION_DETAIL_LEVEL)
			throw new IllegalArgumentException("getLodFromCoordinates given a level of \"" + detailLevel + "\" when \"" + LodUtil.REGION_DETAIL_LEVEL + "\" is the max.");

		int xRegion = LevelPosUtil.getRegion(detailLevel, posX);
		int zRegion = LevelPosUtil.getRegion(detailLevel, posZ);
		LodRegion region = getRegion(xRegion, zRegion);
		if (region == null) return;
		region.updateArea(detailLevel, posX, posZ);
	}
	
	/** Returns true if a region exists at the given LevelPos */
	public boolean doesDataExist(byte detailLevel, int posX, int posZ, EDistanceGenerationMode requiredMode)
	{
		LodRegion region = getRegion(detailLevel, posX, posZ);
		return region != null && region.doesDataExist(detailLevel, posX, posZ, requiredMode);
	}
	
	/**
	 * Loads the region at the given RegionPos from file,
	 * if a file exists for that region.
	 */
	public LodRegion getRegionFromFile(DHRegionPos regionPos, byte detailLevel, EVerticalQuality verticalQuality)
	{
		return fileHandler != null ? fileHandler.loadRegionFromFile(detailLevel, regionPos, verticalQuality) : 
			new LodRegion(detailLevel, regionPos, verticalQuality);
	}
	/**
	 * Loads the region at the given region from file,
	 * if a file exists for that region.
	 */
	public LodRegion getRegionFromFile(LodRegion existingRegion, byte detailLevel, EVerticalQuality verticalQuality)
	{
		return fileHandler != null ? fileHandler.loadRegionFromFile(detailLevel, existingRegion, verticalQuality) : 
			new LodRegion(detailLevel, existingRegion.getRegionPos(), verticalQuality);
	}
	
	/** Save all dirty regions in this LodDimension to file. */
	public void saveDirtyRegionsToFile(boolean blockUntilFinished)
	{
		if (fileHandler == null) return;
		fileHandler.saveDirtyRegionsToFile(blockUntilFinished);
	}
	
	
	/** Return true if the chunk has been pregenerated in game */
	//public boolean isChunkPreGenerated(int xChunkPosWrapper, int zChunkPosWrapper)
	//{
	//
	//	LodRegion region = getRegion(LodUtil.CHUNK_DETAIL_LEVEL, xChunkPosWrapper, zChunkPosWrapper);
	//	if (region == null)
	//		return false;
	//
	//	return region.isChunkPreGenerated(xChunkPosWrapper, zChunkPosWrapper);
	//}
	
	/**
	 * Returns whether the region at the given RegionPos
	 * is within the loaded range.
	 */
	public boolean regionIsInRange(int regionX, int regionZ)
	{
		return regions.inRange(regionX, regionZ);
	}
	
	/** Returns the dimension's center region position X value */
	@Deprecated // Use getCenterRegionPos() instead
	public int getCenterRegionPosX()
	{
		return regions.getCenter().x;
	}
	
	/** Returns the dimension's center region position Z value */
	@Deprecated // Use getCenterRegionPos() instead
	public int getCenterRegionPosZ()
	{
		return regions.getCenter().y;
	}
	
	public DHRegionPos getCenterRegionPos() {
		Pos2D p = regions.getCenter();
		return new DHRegionPos(p.x, p.y);
	}
	
	/** returns the width of the dimension in regions */
	public int getWidth()
	{
		// we want to get the length directly from the
		// source to make sure it is in sync with region
		// and isRegionDirty
		return regions != null ? regions.getSize() : width;
	}
	
	/** Update the width of this dimension, in regions */
	public void setRegionWidth(int newWidth)
	{
		width = newWidth;
		halfWidth = width/ 2;
		Pos2D p = regions.getCenter();
		regions = new MovableGridRingList<LodRegion>(halfWidth, p.x, p.y);
		generateIteratorList();
	}
	
	private final SpamReducedLogger ramLogger = new SpamReducedLogger(1);
	public void dumpRamUsage()
	{
		if (!ramLogger.canMaybeLog()) return;
		int regionCount = width*width;
		ramLogger.info("Dumping Ram Usage for LodDim in {} with {} regions...", dimension.getDimensionName(), regionCount);
		int nonNullRegionCount = 0;
		int dirtiedRegionCount = 0;
		int writingRegionCount = 0;
		long totalUsage = 0;
		int[] detailCount = new int[LodUtil.DETAIL_OPTIONS];
		long[] detailUsage = new long[LodUtil.DETAIL_OPTIONS];
		for (LodRegion r : regions) {
			if (r==null) continue;
			nonNullRegionCount++;
			if (r.needSaving) dirtiedRegionCount++;
			if (r.isWriting.get() != 0) writingRegionCount++;
			LevelContainer[] container = r.debugGetDataContainers().clone();
			if (container == null || container.length != LodUtil.DETAIL_OPTIONS) {
				LOGGER.warn("DumpRamUsage encountered an invalid region!");
				continue;
			}
			for (int i = 0; i < LodUtil.DETAIL_OPTIONS; i++) {
				if (container[i] == null) continue;
				detailCount[i]++;
				long byteUsage = container[i].getRoughRamUsage();
				detailUsage[i] += byteUsage;
				totalUsage += byteUsage;
			}
		}
		ramLogger.info("================================================");
		ramLogger.info("Non Null Regions: [{}], Dirtied Regions: [{}], Writing Regions: [{}], Bytes: [{}]",
				nonNullRegionCount, dirtiedRegionCount, writingRegionCount, new UnitBytes(totalUsage));
		ramLogger.info("------------------------------------------------");
		for (int i = 0; i < LodUtil.DETAIL_OPTIONS; i++) {
			ramLogger.info("DETAIL {}: Containers: [{}], Bytes: [{}]", i, detailCount[i], new UnitBytes(detailUsage[i]));
		}
		ramLogger.info("================================================");
		ramLogger.incLogTries();
		fileHandler.dumpBufferMemoryUsage();
	}
	
	@Override
	public String toString()
	{
		return "[Dim = "+dimension.getDimensionName()+", Region = "+regions+"]";
	}
	
	public String toDetailString()
	{
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("Dimension : \n");
		stringBuilder.append(regions.toDetailString());
		return stringBuilder.toString();
	}

	public void shutdown()
	{
		cutAndExpandThread.shutdown();
		try
		{
			boolean worked = cutAndExpandThread.awaitTermination(5, TimeUnit.SECONDS);
			
			if (!worked)
				LOGGER.error("Cut And Expend threads timed out! May cause crash on game exit due to cleanup failure.");
		}
		catch (InterruptedException e)
		{
			LOGGER.error("Cut And Expend threads shutdown is interrupted! May cause crash on game exit due to cleanup failure: ", e);
		}
		
	}
}
