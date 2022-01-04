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

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.seibel.lod.core.api.ClientApi;
import com.seibel.lod.core.enums.config.DistanceGenerationMode;
import com.seibel.lod.core.enums.config.GenerationPriority;
import com.seibel.lod.core.enums.config.VerticalQuality;
import com.seibel.lod.core.handlers.LodDimensionFileHandler;
import com.seibel.lod.core.objects.PosToGenerateContainer;
import com.seibel.lod.core.objects.PosToRenderContainer;
import com.seibel.lod.core.util.DataPointUtil;
import com.seibel.lod.core.util.DetailDistanceUtil;
import com.seibel.lod.core.util.LevelPosUtil;
import com.seibel.lod.core.util.LodThreadFactory;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.util.SingletonHandler;
import com.seibel.lod.core.wrapperInterfaces.IWrapperFactory;
import com.seibel.lod.core.wrapperInterfaces.chunk.AbstractChunkPosWrapper;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IDimensionTypeWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IWorldWrapper;



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
 * @version 11-12-2021
 */
public class LodDimension
{
	private static final ILodConfigWrapperSingleton CONFIG = SingletonHandler.get(ILodConfigWrapperSingleton.class);
	private static final IMinecraftWrapper MC = SingletonHandler.get(IMinecraftWrapper.class);
	private static final IWrapperFactory FACTORY = SingletonHandler.get(IWrapperFactory.class);
	
	public final IDimensionTypeWrapper dimension;
	
	/** measured in regions */
	private volatile int width;
	/** measured in regions */
	private volatile int halfWidth;
	
	// these three variables are private to force use of the getWidth() method
	// which is a safer way to get the width then directly asking the arrays
	/** stores all the regions in this dimension */
	public volatile LodRegion[][] regions;
	
	/** stores if the region at the given x and z index needs to be saved to disk */
	private volatile boolean[][] isRegionDirty;
	/** stores if the region at the given x and z index needs to be regenerated */
	// Use int because I need Tri state:
	// 0: both buffer good. 1: the displaying buffer good. 2: both buffer bad.
	private volatile int[][] regenRegionBuffer;
	
	/**
	 * if true that means there are regions in this dimension
	 * that need to have their buffers rebuilt.
	 */
	public volatile boolean regenDimensionBuffers = false;
	
	private LodDimensionFileHandler fileHandler;
	
	private final RegionPos center;
	
	private boolean isCutting = false;
	private boolean isExpanding = false;
	
	private final ExecutorService cutAndExpandThread = Executors.newSingleThreadExecutor(new LodThreadFactory(this.getClass().getSimpleName() + " - Cut and Expand"));
	
	/**
	 * Creates the dimension centered at (0,0)
	 * @param newWidth in regions
	 */
	public LodDimension(IDimensionTypeWrapper newDimension, LodWorld lodWorld, int newWidth)
	{
		dimension = newDimension;
		width = newWidth;
		halfWidth = width / 2;
		
		if (newDimension != null && lodWorld != null)
		{
			try
			{
				// determine the save folder
				File saveDir;
				if (MC.hasSinglePlayerServer())
				{
					// local world
					
					IWorldWrapper serverWorld = LodUtil.getServerWorldFromDimension(newDimension);
					saveDir = new File(serverWorld.getSaveFolder().getCanonicalFile().getPath() + File.separatorChar + "lod");
				}
				else
				{
					// connected to server
					
					saveDir = new File(MC.getGameDirectory().getCanonicalFile().getPath() +
											   File.separatorChar + "Distant_Horizons_server_data" + File.separatorChar + MC.getCurrentDimensionId());
				}
				
				fileHandler = new LodDimensionFileHandler(saveDir, this);
			}
			catch (IOException e)
			{
				// the file handler wasn't able to be created
				// we won't be able to read or write any files
			}
		}
		
		
		regions = new LodRegion[width][width];
		isRegionDirty = new boolean[width][width];
		regenRegionBuffer = new int[width][width];
		
		center = new RegionPos(0, 0);
	}
	
	
	
	//FIXME: Race condition on this move and other reading regions!
	/**
	 * Move the center of this LodDimension and move all owned
	 * regions over by the given x and z offset. <br><br>
	 * <p>
	 * Synchronized to prevent multiple moves happening on top of each other.
	 */
	public synchronized void move(RegionPos regionOffset)
	{
		ClientApi.LOGGER.info("LodDim MOVE. Offset: "+regionOffset);
		int xOffset = regionOffset.x;
		int zOffset = regionOffset.z;
		
		// if the x or z offset is equal to or greater than
		// the total width, just delete the current data
		// and update the centerX and/or centerZ
		if (Math.abs(xOffset) >= width || Math.abs(zOffset) >= width)
		{
			for (int x = 0; x < width; x++)
				for (int z = 0; z < width; z++) {
					regions[x][z] = null;
					regenRegionBuffer[x][z] = 0;
				}
			// update the new center
			center.x += xOffset;
			center.z += zOffset;
			
			return;
		}
		
		
		// X
		if (xOffset > 0)
		{
			// move everything over to the left (as the center moves to the right)
			for (int x = 0; x < width; x++)
			{
				for (int z = 0; z < width; z++)
				{
					if (x + xOffset < width) {
						regions[x][z] = regions[x + xOffset][z];
						regenRegionBuffer[x][z] = regenRegionBuffer[x + xOffset][z];
					}
					else {
						regions[x][z] = null;
						regenRegionBuffer[x][z] = 0;
					}
				}
			}
		}
		else
		{
			// move everything over to the right (as the center moves to the left)
			for (int x = width - 1; x >= 0; x--)
			{
				for (int z = 0; z < width; z++)
				{
					if (x + xOffset >= 0) {
						regions[x][z] = regions[x + xOffset][z];
						regenRegionBuffer[x][z] = regenRegionBuffer[x + xOffset][z];
					}
					else {
						regions[x][z] = null;
						regenRegionBuffer[x][z] = 0;
					}
			}
		}
		
		
		// Z
		if (zOffset > 0)
		{
			// move everything up (as the center moves down)
			for (int x = 0; x < width; x++)
			{
				for (int z = 0; z < width; z++)
				{
					if (z + zOffset < width) {
						regions[x][z] = regions[x][z + zOffset];
						regenRegionBuffer[x][z] = regenRegionBuffer[x][z + zOffset];
					}
					else {
						regions[x][z] = null;
						regenRegionBuffer[x][z] = 0;
					}
				}
			}
		}
		else
		{
			// move everything down (as the center moves up)
			for (int x = 0; x < width; x++)
			{
				for (int z = width - 1; z >= 0; z--)
				{
					if (z + zOffset >= 0) {
						regions[x][z] = regions[x][z + zOffset];
						regenRegionBuffer[x][z] = regenRegionBuffer[x][z + zOffset];
					}
					else {
						regions[x][z] = null;
						regenRegionBuffer[x][z] = 0;
					}
				}
				}
			}
		}
		
		
		// update the new center
		center.x += xOffset;
		center.z += zOffset;
		ClientApi.LOGGER.info("LodDim MOVE complete. Offset: "+regionOffset);
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
		int xIndex = (xRegion - center.x) + halfWidth;
		int zIndex = (zRegion - center.z) + halfWidth;
		
		if (!regionIsInRange(xRegion, zRegion))
			return null;
			// throw new ArrayIndexOutOfBoundsException("Region for level pos " + LevelPosUtil.toString(detailLevel, posX, posZ) + " out of range");
		else if (regions[xIndex][zIndex] == null)
			return null;
		else if (regions[xIndex][zIndex].getMinDetailLevel() > detailLevel)
			return null;
		//throw new InvalidParameterException("Region for level pos " + LevelPosUtil.toString(detailLevel, posX, posZ) + " currently only reach level " + regions[xIndex][zIndex].getMinDetailLevel());
		
		return regions[xIndex][zIndex];
	}
	
	/**
	 * Gets the region at the given X and Z
	 * <br>
	 * Returns null if the region doesn't exist
	 * or is outside the loaded area.
	 */
	public LodRegion getRegion(int regionPosX, int regionPosZ)
	{
		int xIndex = (regionPosX - center.x) + halfWidth;
		int zIndex = (regionPosZ - center.z) + halfWidth;
		
		if (!regionIsInRange(regionPosX, regionPosZ))
			return null;
		//throw new ArrayIndexOutOfBoundsException("Region " + regionPosX + " " + regionPosZ + " out of range");
		
		return regions[xIndex][zIndex];
	}
	
	/** Useful when iterating over every region. */
	public LodRegion getRegionByArrayIndex(int xIndex, int zIndex)
	{
		return regions[xIndex][zIndex];
	}
	
	/**
	 * Overwrite the LodRegion at the location of newRegion with newRegion.
	 * @throws ArrayIndexOutOfBoundsException if newRegion is outside what can be stored in this LodDimension.
	 */
	public synchronized void addOrOverwriteRegion(LodRegion newRegion) throws ArrayIndexOutOfBoundsException
	{
		int xIndex = (newRegion.regionPosX - center.x) + halfWidth;
		int zIndex = (newRegion.regionPosZ - center.z) + halfWidth;
		
		if (!regionIsInRange(newRegion.regionPosX, newRegion.regionPosZ))
			// out of range
			throw new ArrayIndexOutOfBoundsException("Region " + newRegion.regionPosX + ", " + newRegion.regionPosZ + " out of range");
		
		regions[xIndex][zIndex] = newRegion;
	}
	public interface PosComsumer {
		void run(int x, int z);
	}
	
	public void iterateWithSpiral(PosComsumer r) {
		int ox,oy,dx,dy;
	    ox = oy = dx = 0;
	    dy = -1;
	    int len = regions.length;
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
	
	
	/**
	 * Deletes nodes that are a higher detail then necessary, freeing
	 * up memory.
	 */
	public void cutRegionNodesAsync(int playerPosX, int playerPosZ)
	{
		if (isCutting) return;
		isCutting = true;
		// don't run the tree cutter multiple times
		// for the same location
		Runnable thread = () -> {
			//ClientApi.LOGGER.info("LodDim cut Region: " + playerPosX + "," + playerPosZ);

			// go over every region in the dimension
			iterateWithSpiral((int x, int z) -> {
				int regionX;
				int regionZ;
				int minDistance;
				byte detail;
				byte minAllowedDetailLevel;
				regionX = (x + center.x) - halfWidth;
				regionZ = (z + center.z) - halfWidth;

				if (regions[x][z] != null) {
					// check what detail level this region should be
					// and cut it if it is higher then that
					minDistance = LevelPosUtil.minDistance(LodUtil.REGION_DETAIL_LEVEL, regionX, regionZ,
							playerPosX, playerPosZ);
					detail = DetailDistanceUtil.getTreeCutDetailFromDistance(minDistance);
					minAllowedDetailLevel = DetailDistanceUtil.getCutLodDetail(detail);

					if (regions[x][z].getMinDetailLevel() < minAllowedDetailLevel) {
						regions[x][z].cutTree(minAllowedDetailLevel);
						regenRegionBuffer[x][z] = 2;
						regenDimensionBuffers = true;
					}
				}
			});
			//ClientApi.LOGGER.info("LodDim cut Region complete: " + playerPosX + "," + playerPosZ);
			isCutting = false;
		};
		cutAndExpandThread.execute(thread);
	}
	
	/** Either expands or loads all regions in the rendered LOD area */
	public void expandOrLoadRegionsAsync(int playerPosX, int playerPosZ) {

		if (isExpanding) return;
		isExpanding = true;
		
		DistanceGenerationMode generationMode = CONFIG.client().worldGenerator().getDistanceGenerationMode();
		VerticalQuality verticalQuality = CONFIG.client().graphics().quality().getVerticalQuality();

		// don't run the expander multiple times
		// for the same location
		Runnable thread = () -> {
			//ClientApi.LOGGER.info("LodDim expend Region: " + playerPosX + "," + playerPosZ);

			iterateWithSpiral((int x, int z) -> {
				int regionX;
				int regionZ;
				LodRegion region;
				int minDistance;
				int maxDistance;
				byte minDetail;
				byte maxDetail;
				regionX = (x + center.x) - halfWidth;
				regionZ = (z + center.z) - halfWidth;
				final RegionPos regionPos = new RegionPos(regionX, regionZ);
				region = regions[x][z];

				minDistance = LevelPosUtil.minDistance(LodUtil.REGION_DETAIL_LEVEL, regionX, regionZ, playerPosX,
						playerPosZ);
				maxDistance = LevelPosUtil.maxDistance(LodUtil.REGION_DETAIL_LEVEL, regionX, regionZ, playerPosX,
						playerPosZ);
				minDetail = DetailDistanceUtil.getTreeGenDetailFromDistance(minDistance);
				maxDetail = DetailDistanceUtil.getTreeGenDetailFromDistance(maxDistance);
				
				boolean updated = false;
				if (region == null) {
					regions[x][z] = getRegionFromFile(regionPos, minDetail, generationMode, verticalQuality);
					updated = true;
				} else if (region.getGenerationMode().compareTo(generationMode) < 0 ||
						region.getVerticalQuality() != verticalQuality ||
						region.getMinDetailLevel() > minDetail) {
					regions[x][z] = getRegionFromFile(regions[x][z], minDetail, generationMode, verticalQuality);
					updated = true;
				}
				if (updated) {
					regenRegionBuffer[x][z] = 2;
					regenDimensionBuffers = true;
				}
			});
			//ClientApi.LOGGER.info("LodDim expend Region complete: " + playerPosX + "," + playerPosZ);
			isExpanding = false;
		};

		cutAndExpandThread.execute(thread);
	}
	
	/**
	 * Use addVerticalData when possible.
	 * Add the given LOD to this dimension at the coordinate
	 * stored in the LOD. If an LOD already exists at the given
	 * coordinate it will be overwritten.
	 */
	public Boolean addData(byte detailLevel, int posX, int posZ, int verticalIndex, long data, boolean dontSave)
	{
		int regionPosX = LevelPosUtil.getRegion(detailLevel, posX);
		int regionPosZ = LevelPosUtil.getRegion(detailLevel, posZ);
		
		// don't continue if the region can't be saved
		LodRegion region = getRegion(regionPosX, regionPosZ);
		if (region == null)
			return false;
		
		boolean nodeAdded = region.addData(detailLevel, posX, posZ, verticalIndex, data);
		
		// only save valid LODs to disk
		if (!dontSave && fileHandler != null)
		{
			try
			{
				// mark the region as dirty, so it will be saved to disk
				int xIndex = (regionPosX - center.x) + halfWidth;
				int zIndex = (regionPosZ - center.z) + halfWidth;
				
				isRegionDirty[xIndex][zIndex] = true;
				regenRegionBuffer[xIndex][zIndex] = 2;
				regenDimensionBuffers = true;
			}
			catch (ArrayIndexOutOfBoundsException e)
			{
				e.printStackTrace();
				// If this happens, the method was probably 
				// called when the dimension was changing size.
				// Hopefully this shouldn't be an issue.
			}
		}
		
		return nodeAdded;
	}
	
	/**
	 * Add whole column of LODs to this dimension at the coordinate
	 * stored in the LOD. If an LOD already exists at the given
	 * coordinate it will be overwritten.
	 */
	public Boolean addVerticalData(byte detailLevel, int posX, int posZ, long[] data, boolean dontSave)
	{
		int regionPosX = LevelPosUtil.getRegion(detailLevel, posX);
		int regionPosZ = LevelPosUtil.getRegion(detailLevel, posZ);
		
		// don't continue if the region can't be saved
		LodRegion region = getRegion(regionPosX, regionPosZ);
		if (region == null)
			return false;
		
		boolean nodeAdded = region.addVerticalData(detailLevel, posX, posZ, data);
		
		// only save valid LODs to disk
		if (!dontSave && fileHandler != null)
		{
			try
			{
				// mark the region as dirty, so it will be saved to disk
				int xIndex = (regionPosX - center.x) + halfWidth;
				int zIndex = (regionPosZ - center.z) + halfWidth;
				
				isRegionDirty[xIndex][zIndex] = true;
				regenRegionBuffer[xIndex][zIndex] = 2;
				regenDimensionBuffers = true;
			}
			catch (ArrayIndexOutOfBoundsException e)
			{
				e.printStackTrace();
				// If this happens, the method was probably
				// called when the dimension was changing size.
				// Hopefully this shouldn't be an issue.
			}
		}
		
		return nodeAdded;
	}
	
	/** marks the region at the given region position to have its buffer rebuilt */
	public void markRegionBufferToRegen(int xRegion, int zRegion)
	{
		int xIndex = (xRegion - center.x) + halfWidth;
		int zIndex = (zRegion - center.z) + halfWidth;
		regenRegionBuffer[xIndex][zIndex] = 2;
	}
	
	/**
	 * Returns every position that need to be generated based on the position of the player
	 */
	public PosToGenerateContainer getPosToGenerate(int maxDataToGenerate, int playerBlockPosX, int playerBlockPosZ)
	{
		PosToGenerateContainer posToGenerate;
		LodRegion lodRegion;
		// all the following values are used for the spiral matrix visit
		// x and z are the matrix coord
		// dx and dz is the next move on the coordinate in the range -1 0 +1
		int x, z, dx, dz, t;
		x = 0;
		z = 0;
		dx = 0;
		dz = -1;
		
		//in the FAR_FIRST generation we dedicate part of the generation process to the far region with really
		//low detail quality.
		
		posToGenerate = new PosToGenerateContainer((byte) 8, maxDataToGenerate, playerBlockPosX, playerBlockPosZ);
		
		int xRegion;
		int zRegion;
		
		for (int i = 0; i < width * width; i++)
		{
			xRegion = x + center.x;
			zRegion = z + center.z;
			
			//All of this is handled directly by the region, which scan every pos from top to bottom of the quad tree
			lodRegion = getRegion(xRegion, zRegion);
			if (lodRegion != null)
				lodRegion.getPosToGenerate(posToGenerate, playerBlockPosX, playerBlockPosZ);
			
			
			//with this code section we find the next chunk to check
			if ((x == z) || ((x < 0) && (x == -z)) || ((x > 0) && (x == 1 - z)))
			{
				t = dx;
				dx = -dz;
				dz = t;
			}
			x += dx;
			z += dz;
		}
	return posToGenerate;
	}
	
	/**
	 * Fills the posToRender with the position to render for the regionPos given in input
	 */
	public void getPosToRender(PosToRenderContainer posToRender, RegionPos regionPos, int playerPosX,
			int playerPosZ)
	{
		LodRegion region = getRegion(regionPos.x, regionPos.z);
		
		// use FAR_FIRST on local worlds and NEAR_FIRST on servers
		GenerationPriority generationPriority = CONFIG.client().worldGenerator().getGenerationPriority();
		if (generationPriority == GenerationPriority.AUTO)
			generationPriority = MC.hasSinglePlayerServer() ? GenerationPriority.FAR_FIRST : GenerationPriority.NEAR_FIRST;
		
		boolean requireCorrectDetailLevel = generationPriority == GenerationPriority.NEAR_FIRST;
		
		if (region != null)
			region.getPosToRender(posToRender, playerPosX, playerPosZ, requireCorrectDetailLevel);
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
	public long getSingleData(byte detailLevel, int posX, int posZ)
	{
		if (detailLevel > LodUtil.REGION_DETAIL_LEVEL)
			throw new IllegalArgumentException("getLodFromCoordinates given a level of \"" + detailLevel + "\" when \"" + LodUtil.REGION_DETAIL_LEVEL + "\" is the max.");
		
		LodRegion region = getRegion(detailLevel, posX, posZ);
		if (region == null)
			return DataPointUtil.EMPTY_DATA;
		
		return region.getSingleData(detailLevel, posX, posZ);
	}
	
	/** Clears the given region */
	public void clear(byte detailLevel, int posX, int posZ)
	{
		if (detailLevel > LodUtil.REGION_DETAIL_LEVEL)
			throw new IllegalArgumentException("getLodFromCoordinates given a level of \"" + detailLevel + "\" when \"" + LodUtil.REGION_DETAIL_LEVEL + "\" is the max.");

		int xRegion = LevelPosUtil.getRegion(detailLevel, posX);
		int zRegion = LevelPosUtil.getRegion(detailLevel, posZ);
		LodRegion region = getRegion(xRegion, zRegion);
		if (region == null)
			return;
		markRegionBufferToRegen(xRegion, zRegion);
		region.clear(detailLevel, posX, posZ);
	}
	
	/**
	 * Returns if the buffer at the given array index needs
	 * to have its buffer regenerated. Also decrease the state by 1
	 */
	public boolean getAndClearRegionNeedBufferRegen(int regionX, int regionZ)
	{
		//FIXME: Use actual atomics on regenRegionBuffer
		//FIXME: Race condition on lodDim move/resize!
		int xIndex = (regionX - center.x) + halfWidth;
		int zIndex = (regionZ - center.z) + halfWidth;
		
		if (xIndex < 0 || xIndex >= width || zIndex < 0 || zIndex >= width)
			return false;
		int i = regenRegionBuffer[xIndex][zIndex];
		if (i > 0) {
			regenRegionBuffer[xIndex][zIndex]--;
			return true;
		}
		return false;
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
		if (region == null)
			return;
		markRegionBufferToRegen(xRegion, zRegion);
		
		region.updateArea(detailLevel, posX, posZ);
	}
	
	/** Returns true if a region exists at the given LevelPos */
	public boolean doesDataExist(byte detailLevel, int posX, int posZ)
	{
		LodRegion region = getRegion(detailLevel, posX, posZ);
		return region != null && region.doesDataExist(detailLevel, posX, posZ);
	}
	
	/**
	 * Loads the region at the given RegionPos from file,
	 * if a file exists for that region.
	 */
	public LodRegion getRegionFromFile(RegionPos regionPos, byte detailLevel,
			DistanceGenerationMode generationMode, VerticalQuality verticalQuality)
	{
		return fileHandler != null ? fileHandler.loadRegionFromFile(detailLevel, regionPos, generationMode, verticalQuality) : 
			new LodRegion(detailLevel, regionPos, generationMode, verticalQuality);
	}
	/**
	 * Loads the region at the given region from file,
	 * if a file exists for that region.
	 */
	public LodRegion getRegionFromFile(LodRegion existingRegion, byte detailLevel,
			DistanceGenerationMode generationMode, VerticalQuality verticalQuality)
	{
		return fileHandler != null ? fileHandler.loadRegionFromFile(detailLevel, existingRegion, generationMode, verticalQuality) : 
			new LodRegion(detailLevel, existingRegion.getRegionPos(), generationMode, verticalQuality);
	}
	
	/** Save all dirty regions in this LodDimension to file. */
	public void saveDirtyRegionsToFileAsync()
	{
		fileHandler.saveDirtyRegionsToFileAsync();
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
		int xIndex = (regionX - center.x) + halfWidth;
		int zIndex = (regionZ - center.z) + halfWidth;
		
		return xIndex >= 0 && xIndex < width && zIndex >= 0 && zIndex < width;
	}
	
	/** Returns the dimension's center region position X value */
	public int getCenterRegionPosX()
	{
		return center.x;
	}
	
	/** Returns the dimension's center region position Z value */
	public int getCenterRegionPosZ()
	{
		return center.z;
	}
	
	/** returns the width of the dimension in regions */
	public int getWidth()
	{
		// we want to get the length directly from the
		// source to make sure it is in sync with region
		// and isRegionDirty
		return regions != null ? regions.length : width;
	}
	
	/** Update the width of this dimension, in regions */
	public void setRegionWidth(int newWidth)
	{
		width = newWidth;
		halfWidth = width/ 2;
		
		regions = new LodRegion[width][width];
		isRegionDirty = new boolean[width][width];
		regenRegionBuffer = new int[width][width];
		
		// populate isRegionDirty
		for (int i = 0; i < width; i++)
			for (int j = 0; j < width; j++)
				isRegionDirty[i][j] = false;
	}
	
	
	@Override
	public String toString()
	{
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("Dimension : \n");
		for (LodRegion[] lodRegions : regions)
		{
			for (LodRegion region : lodRegions)
			{
				if (region == null)
					stringBuilder.append("n");
				else
					stringBuilder.append(region.getMinDetailLevel());
				stringBuilder.append("\t");
			}
			stringBuilder.append("\n");
		}
		return stringBuilder.toString();
	}
	
	public boolean GetIsRegionDirty(int i, int j)
	{
		return isRegionDirty[i][j];
	}
	
	public void SetIsRegionDirty(int i, int j, boolean val)
	{
		isRegionDirty[i][j] = val;
	}
}
