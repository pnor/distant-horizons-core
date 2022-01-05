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

package com.seibel.lod.core.handlers;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ConcurrentModificationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream;

import com.seibel.lod.core.api.ClientApi;
import com.seibel.lod.core.enums.config.DistanceGenerationMode;
import com.seibel.lod.core.enums.config.VerticalQuality;
import com.seibel.lod.core.objects.lod.LodDimension;
import com.seibel.lod.core.objects.lod.LodRegion;
import com.seibel.lod.core.objects.lod.RegionPos;
import com.seibel.lod.core.objects.lod.VerticalLevelContainer;
import com.seibel.lod.core.util.LodThreadFactory;
import com.seibel.lod.core.util.LodUtil;


/**
 * This object handles creating LodRegions
 * from files and saving LodRegion objects
 * to file.
 * 
 * @author James Seibel
 * @author Cola
 * @version 9-25-2021
 */
public class LodDimensionFileHandler
{
	/** This is the dimension that owns this file handler */
	private LodDimension lodDimension;
	
	private final File dimensionDataSaveFolder;
	
	/** lod */
	private static final String FILE_NAME_PREFIX = "lod";
	/** .txt */
	private static final String FILE_EXTENSION = ".xz";
	/** detail- */
	private static final String DETAIL_FOLDER_NAME_PREFIX = "detail-";
	
	/**
	 * .tmp <br>
	 * Added to the end of the file path when saving to prevent
	 * nulling a currently existing file. <br>
	 * After the file finishes saving it will end with
	 * FILE_EXTENSION.
	 */
	private static final String TMP_FILE_EXTENSION = ".tmp";
	
	/**
	 * This is the file version currently accepted by this
	 * file handler, older versions (smaller numbers) will be deleted and overwritten,
	 * newer versions (larger numbers) will be ignored and won't be read.
	 */
	public static final int LOD_SAVE_FILE_VERSION = 8;
	
	/**
	 * Allow saving asynchronously, but never try to save multiple regions
	 * at a time
	 */
	private AtomicBoolean isFileWritingThreadRunning = new AtomicBoolean(false);
	private ExecutorService fileWritingThreadPool = Executors.newSingleThreadExecutor(new LodThreadFactory(this.getClass().getSimpleName()));
	
	private ConcurrentHashMap<RegionPos, LodRegion> regionToSave = new ConcurrentHashMap<RegionPos, LodRegion>();
	
	
	public LodDimensionFileHandler(File newSaveFolder, LodDimension newLodDimension)
	{
		if (newSaveFolder == null)
			throw new IllegalArgumentException("LodDimensionFileHandler requires a valid File location to read and write to.");
		
		dimensionDataSaveFolder = newSaveFolder;
		lodDimension = newLodDimension;
	}
	
	
	
	
	
	//================//
	// read from file //
	//================//

	/**
	 * Returns a new LodRegion at the given coordinates.
	 * Returns an empty region if the file doesn't exist.
	 */
	public LodRegion loadRegionFromFile(byte detailLevel, RegionPos regionPos, DistanceGenerationMode generationMode, VerticalQuality verticalQuality)
	{
		// Get one from the region hot cache
		LodRegion region = regionToSave.get(regionPos);
		if (region!=null && region.getMinDetailLevel()<=detailLevel &&
			region.getGenerationMode().compareTo(generationMode)>=0 &&
			region.getVerticalQuality().compareTo(verticalQuality)>=0)
			return region; // The current hot cache to-be-saved region match our requirement.
		region = new LodRegion((byte) (LodUtil.REGION_DETAIL_LEVEL+1), regionPos, generationMode, verticalQuality);
		return loadRegionFromFile(detailLevel, region, generationMode, verticalQuality);
	}
	
	/**
	 * Returns the LodRegion that is filled at the given coordinates.
	 * Returns an empty region if the file doesn't exist.
	 */
	public LodRegion loadRegionFromFile(byte detailLevel, LodRegion region, DistanceGenerationMode generationMode, VerticalQuality verticalQuality)
	{
		if (region.getGenerationMode().compareTo(generationMode)<0 || region.getVerticalQuality().compareTo(verticalQuality)<0) {
			regionToSave.put(region.getRegionPos(), region); //FIXME: The hashMap key should prob be a {regionPos,VertQual} pair. 
			region = new LodRegion((byte) (LodUtil.REGION_DETAIL_LEVEL+1), region.getRegionPos(), generationMode, verticalQuality);
		}
		int regionX = region.regionPosX;
		int regionZ = region.regionPosZ;
		
		for (byte tempDetailLevel = (byte) (region.getMinDetailLevel()-1); tempDetailLevel >= detailLevel; tempDetailLevel--)
		{
			
			File file = getBestMatchingRegionFile(tempDetailLevel, regionX, regionZ, generationMode, verticalQuality);
			if (file == null) {
				region.addLevelContainer(new VerticalLevelContainer(tempDetailLevel));
				continue; // Failed to find the file for this detail level. continue and try next one
			}
			
			long fileSize = file.length();
			if (fileSize == 0) {
				region.addLevelContainer(new VerticalLevelContainer(tempDetailLevel));
				continue; // file is empty. Let's not try parsing empty files
			}
			try (FileInputStream fileInStream = new FileInputStream(file))
			{
				XZCompressorInputStream inputStream = new XZCompressorInputStream(fileInStream);
				int fileVersion;
				fileVersion = inputStream.read();
				
				// check if this file can be read by this file handler
				if (fileVersion < 6)
				{
					// the file we are reading is an older version,
					// close the reader and delete the file.
					inputStream.close();
					file.delete();
					ClientApi.LOGGER.info("Outdated LOD region file for region: (" + regionX + "," + regionZ + ")"
							+ " version found: " + fileVersion
							+ ", version requested: " + LOD_SAVE_FILE_VERSION
							+ ". File has been deleted.");
					// This should not break, but be continue to see whether other detail levels can be loaded or updated
					region.addLevelContainer(new VerticalLevelContainer(tempDetailLevel));
					continue;
				}
				else if (fileVersion > LOD_SAVE_FILE_VERSION)
				{
					// the file we are reading is a newer version,
					// close the reader and ignore the file, we don't
					// want to accidentally delete anything the user may want.
					inputStream.close();
					ClientApi.LOGGER.info("Newer LOD region file for region: (" + regionX + "," + regionZ + ")"
							+ " version found: " + fileVersion
							+ ", version requested: " + LOD_SAVE_FILE_VERSION
							+ " this region will not be written to in order to protect the newer file.");
					// This should not break, but be continue to see whether other detail levels can be loaded or updated
					region.addLevelContainer(new VerticalLevelContainer(tempDetailLevel));
					continue;
				}
				else if (fileVersion < LOD_SAVE_FILE_VERSION)
				{
					ClientApi.LOGGER.debug("Old LOD region file for region: (" + regionX + "," + regionZ + ")"
							+ " version found: " + fileVersion
							+ ", version requested: " + LOD_SAVE_FILE_VERSION
							+ ". File will be loaded and updated to new format in next save.");
					// this is old, but readable version
					// read and add the data to our region
					region.addLevelContainer(new VerticalLevelContainer(new DataInputStream(inputStream), fileVersion, tempDetailLevel));
					inputStream.close();
				} else
				{
					// this file is a readable version,
					// read and add the data to our region
					region.addLevelContainer(new VerticalLevelContainer(new DataInputStream(inputStream), LOD_SAVE_FILE_VERSION, tempDetailLevel));
					inputStream.close();
				}
			}
			catch (IOException ioEx)
			{
				ClientApi.LOGGER.error("LOD file read error. Unable to read xz compressed file [" + file + "] error [" + ioEx.getMessage() + "]: ");
				ioEx.printStackTrace();
				region.addLevelContainer(new VerticalLevelContainer(tempDetailLevel));
			}
		}// for each detail level
		
		return region;
	}
	
	
	//==============//
	// Save to File //
	//==============//
	
	public void addRegionsToSave(LodRegion r) {
		regionToSave.put(r.getRegionPos(), r);
	}
	
	/** Save all dirty regions in this LodDimension to file */
	public void saveDirtyRegionsToFile(boolean blockUntilFinished)
	{
		for (int i = 0; i < lodDimension.getWidth(); i++)
		{
			for (int j = 0; j < lodDimension.getWidth(); j++)
			{
				LodRegion r = lodDimension.getRegionByArrayIndex(i, j);
				
				if (r != null && r.needSaving)
				{
					r.needSaving = false;
					regionToSave.put(r.getRegionPos(), r);
				}
			}
		}
		trySaveRegionsToBeSaved();
		if (blockUntilFinished) {
			ClientApi.LOGGER.info("Blocking until lod file save finishes!");
			try {
				fileWritingThreadPool.shutdown();
				boolean worked = fileWritingThreadPool.awaitTermination(30, TimeUnit.SECONDS);
				if (!worked)
					ClientApi.LOGGER.error("File writing timed out! File data may not be saved correctly and may cause corruptions!!!");
			} catch (InterruptedException e) {
				ClientApi.LOGGER.error("File writing wait is interrupted! File data may not be saved correctly and may cause corruptions!!!");
				e.printStackTrace();
			} finally {
				fileWritingThreadPool = Executors.newSingleThreadExecutor(new LodThreadFactory(this.getClass().getSimpleName()));
			}
		}
	}
	
	public void trySaveRegionsToBeSaved() {
		if (regionToSave.isEmpty()) return;
		// Use Memory order Acquire to acquire any memory changes on getting this boolean
		// (Corresponding call is the this::writerMain(...)::...setRelease(false);)
		boolean haventStarted = !isFileWritingThreadRunning.compareAndExchangeAcquire(false, true);
		if (haventStarted) {
			// We acquired the atomic lock.
			fileWritingThreadPool.execute(this::writerMain);
		}
	}

	private void writerMain() {
		// Use Memory order Relaxed as no additional memory changes needed to be visible.
		// (This is just a safety checks)
		boolean isStarted = isFileWritingThreadRunning.getPlain();
		if (!isStarted) throw new ConcurrentModificationException("WriterMain Triggered but the thead state is not started!?");
		ClientApi.LOGGER.info("Lod File Writer started. To-be-written-regions: "+regionToSave.size());
		Instant start = Instant.now();
		// Note: Since regionToSave is a ConcurrentHashMap, and the .values() return one that support concurrency,
		//       this for loop should be safe and loop until all values are gone.
		while (!regionToSave.isEmpty()) {
			for (LodRegion r : regionToSave.values()) {
				//Check if the data has been swapped out right under me. Otherwise remove it from the entry
				if (!regionToSave.remove(r.getRegionPos(), r)) continue;
				try
				{
					Instant i = Instant.now();
					ClientApi.LOGGER.info("Lod: Saving Region "+r.getRegionPos());
					saveRegionToFile(r);
					Instant j = Instant.now();
					Duration d = Duration.between(i, j);
					ClientApi.LOGGER.info("Lod: Region "+r.getRegionPos()+" save finish. Took "+d);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		}
		Instant end = Instant.now();
		ClientApi.LOGGER.info("Lod File Writer completed. Took "+Duration.between(start, end));
		// Use Memory order Release to release any memory changes on setting this boolean
		// (Corresponding call is the this::saveRegions(...)::...compareAndExchangeAcquire(false, true);)
		isFileWritingThreadRunning.setRelease(false);
	}
	
	/**
	 * Save a specific region to disk.<br>
	 * Note: <br>
	 * 1. If a file already exists for a newer version
	 * the file won't be written.<br>
	 * 2. This will save to the LodDimension that this
	 * handler is associated with.
	 */
	private void saveRegionToFile(LodRegion region)
	{
		for (byte detailLevel = region.getMinDetailLevel(); detailLevel <= LodUtil.REGION_DETAIL_LEVEL; detailLevel++)
		{
			// Get the old file
			File oldFile = getRegionFile(region.regionPosX, region.regionPosZ, region.getGenerationMode(), detailLevel, region.getVerticalQuality());
			ClientApi.LOGGER.debug("saving region [" + region.regionPosX + ", " + region.regionPosZ + "] detail "+detailLevel+" to file.");
			
			boolean isFileFullyGened = false;
			// make sure the file and folder exists
			if (!oldFile.exists())
			{
				// the file doesn't exist,
				// create it and the folder if need be
				if (!oldFile.getParentFile().exists())
					oldFile.getParentFile().mkdirs();
				try {
					oldFile.createNewFile();
				} catch (IOException e) {
					ClientApi.LOGGER.error("LOD file write error. Unable to create parent directory for [" + oldFile + "] error [" + e.getMessage() + "]: ");
					e.printStackTrace();
					continue;
				}
			}
			else
			{
				// the file exists, make sure it
				// is the correct version.
				// (to make sure we don't overwrite a newer
				// version file if it exists)
				int fileVersion = LOD_SAVE_FILE_VERSION;
				try (FileInputStream fileInStream = new FileInputStream(oldFile))
				{
					XZCompressorInputStream inputStream = new XZCompressorInputStream(fileInStream);
					fileVersion = inputStream.read();
					inputStream.skip(1);
					isFileFullyGened = (inputStream.read() & 0b10000000) != 0;
					inputStream.close();
				}
				catch (IOException e)
				{
					ClientApi.LOGGER.warn("LOD file write warning. Unable to read existing file [" + oldFile + "] version. Treating it as latest version. [" + e.getMessage() + "]: ");
					e.printStackTrace();
				}
				
				// check if this file can be written to by the file handler
				if (fileVersion > LOD_SAVE_FILE_VERSION)
				{
					// the file we are reading is a newer version,
					// don't write anything, we don't want to accidentally
					// delete anything the user may want.
					continue;
				}
				// if we got this far then we are good
				// to overwrite the old file
			}
			
			// Now create a new temporary save file
			File tempFile = new File(oldFile.getPath() + TMP_FILE_EXTENSION);
			try (FileOutputStream fileOutStream = new FileOutputStream(tempFile))
			{
				XZCompressorOutputStream outputStream = new XZCompressorOutputStream(fileOutStream, 3);
				// add the version of this file
				outputStream.write(LOD_SAVE_FILE_VERSION);
				// add each LodChunk to the file
				boolean isNewDataFullyGened = region.getLevel(detailLevel).writeData(new DataOutputStream(outputStream));
				outputStream.close();
				
				if (!isNewDataFullyGened && isFileFullyGened)
				{
					// existing file is complete while new one is only partially generate
					// this can happen is for some reason loading failed
					// this doesn't fix the bug, but at least protects old data
					ClientApi.LOGGER.error("LOD file write error. Attempted to overwrite complete region with incomplete one [" + oldFile + "]");
					try {
						tempFile.delete();
					} catch (SecurityException e) {
						// Failed to delete temp file... just continue.
					}
					continue;
				}
			}
			catch (IOException e)
			{
				ClientApi.LOGGER.error("LOD file write error. Unable to write to temp file [" + tempFile + "] error [" + e.getMessage() + "]: ");
				e.printStackTrace();
				continue;
			}
			
			// overwrite the old file with the new one
			try {
				Files.move(tempFile.toPath(), oldFile.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e) {
				ClientApi.LOGGER.error("LOD file write error. Unable to update file [" + oldFile + "] error [" + e.getMessage() + "]: ");
				e.printStackTrace();
			}
		}
	}
	
	//================//
	// helper methods //
	//================//
	
	/**
	 * Return the name of the file that should contain the
	 * region at the given x and z. <br>
	 * Returns null if this object isn't available to read and write. <br><br>
	 * <p>
	 * example: "lod.0.0.txt" <br>
	 * <p>
	 * Returns null if there is an IO or security Exception.
	 */
	
	private String getFileBasePath() {
		try {
			return dimensionDataSaveFolder.getCanonicalPath() + File.separatorChar;
		} catch (IOException e) {
			ClientApi.LOGGER.warn("Unable to get the base save file path. One possible cause is that"
					+ " the process failed to read the current path location due to security configs.");
			throw new RuntimeException("DistantHorizons Get Save File Path Failure");
		}
	}
	
	private File getRegionFile(int regionX, int regionZ, DistanceGenerationMode genMode, byte detail, VerticalQuality vertQuality) {
		return new File(getFileBasePath() + vertQuality + File.separatorChar +
				genMode + File.separatorChar +
				DETAIL_FOLDER_NAME_PREFIX + detail + File.separatorChar +
				FILE_NAME_PREFIX + "." + regionX + "." + regionZ + FILE_EXTENSION);
	}
	
	// Return null if no file found
	private File getBestMatchingRegionFile(byte detailLevel, int regionX, int regionZ, DistanceGenerationMode targetGenMode, VerticalQuality targetVertQuality) {
		DistanceGenerationMode genMode = DistanceGenerationMode.FULL;
		// Search from least GenMode to max GenMode, than least vertQuality to max vertQuality
		do {
			File file = getRegionFile(regionX, regionZ, genMode, detailLevel, targetVertQuality);
			if (file.exists()) return file; // Found target file.
			genMode = DistanceGenerationMode.previous(genMode);
			if (genMode==null || genMode==DistanceGenerationMode.previous(targetGenMode)) { // Failed to find any files for this vertQuality. Try next one up.
				genMode = DistanceGenerationMode.FULL;
				targetVertQuality = VerticalQuality.next(targetVertQuality);
			}
		} while (targetVertQuality != null);
		return null;
	}
	
	
}
