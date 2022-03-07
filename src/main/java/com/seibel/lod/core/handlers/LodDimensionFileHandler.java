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
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import com.seibel.lod.core.api.ApiShared;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream;

import com.seibel.lod.core.api.ClientApi;
import com.seibel.lod.core.enums.config.DistanceGenerationMode;
import com.seibel.lod.core.enums.config.VerticalQuality;
import com.seibel.lod.core.objects.lod.LevelContainer;
import com.seibel.lod.core.objects.lod.LodDimension;
import com.seibel.lod.core.objects.lod.LodRegion;
import com.seibel.lod.core.objects.lod.RegionPos;
import com.seibel.lod.core.objects.lod.VerticalLevelContainer;
import com.seibel.lod.core.util.LodThreadFactory;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.util.SpamReducedLogger;
import com.seibel.lod.core.util.UnitBytes;


/**
 * This object handles creating LodRegions
 * from files and saving LodRegion objects
 * to file.
 * 
 * @author James Seibel
 * @author Cola
 * @version 3-7-2022
 */
public class LodDimensionFileHandler
{
	public static final boolean ENABLE_SAVE_THREAD_LOGGING = true;
	public static final boolean ENABLE_SAVE_REGION_LOGGING = false;
	
	/** This is the dimension that owns this file handler */
	private final LodDimension lodDimension;
	
	public final File dimensionDataSaveFolder;
	
	/** lod */
	private static final String FILE_NAME_PREFIX = "lod";
	/** .xz */
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
	public static final int LOD_SAVE_FILE_VERSION = 9;
	
	/**
	 * Allow saving asynchronously, but never try to save multiple regions
	 * at a time
	 */
	private final AtomicBoolean isFileWritingThreadRunning = new AtomicBoolean(false);
	private ExecutorService fileWritingThreadPool = Executors.newSingleThreadExecutor(
			new LodThreadFactory(this.getClass().getSimpleName(), Thread.NORM_PRIORITY + 1));
	
	private final ConcurrentHashMap<RegionPos, LodRegion> regionToSave = new ConcurrentHashMap<RegionPos, LodRegion>();
	
	
	public LodDimensionFileHandler(File newSaveFolder, LodDimension newLodDimension)
	{
		if (newSaveFolder == null)
			throw new IllegalArgumentException("LodDimensionFileHandler requires a valid File location to read and write to.");
		
		dimensionDataSaveFolder = newSaveFolder;
		lodDimension = newLodDimension;
		
		checkForOldSaveStructure();
	}
	
	private ReentrantLock mergeOldFileLock = new ReentrantLock();
	
	private void checkForOldSaveStructure()
	{
		File file = new File(getFileBasePath());
		if (!file.exists())
			return;
		
		File[] vertQualFiles = file.listFiles();
		for (File vertQualFile : vertQualFiles)
		{
			if (!vertQualFile.isDirectory())
				continue;
			
			if (vertQualFile.getName().equals(VerticalQuality.HIGH.toString()) ||
					vertQualFile.getName().equals(VerticalQuality.MEDIUM.toString()) ||
					vertQualFile.getName().equals(VerticalQuality.LOW.toString()))
			{
				File[] subFiles = vertQualFile.listFiles();
				for (File subFile : subFiles)
				{
					if (!subFile.isDirectory())
						continue;
					
					if (subFile.getName().equals(DistanceGenerationMode.FULL.toString()) ||
							subFile.getName().equals(DistanceGenerationMode.FEATURES.toString()) ||
							subFile.getName().equals(DistanceGenerationMode.SURFACE.toString()) ||
							subFile.getName().equals(DistanceGenerationMode.BIOME_ONLY_SIMULATE_HEIGHT.toString()) ||
							subFile.getName().equals(DistanceGenerationMode.BIOME_ONLY.toString()) ||
							subFile.getName().equals(DistanceGenerationMode.NONE.toString()))
					{
						ApiShared.LOGGER.info("Noticed old save structure files. Starting merge process...");
						LodDimensionOldFileStructureHandler oldFileStructHandler = new LodDimensionOldFileStructureHandler(this);
						if (mergeOldFileLock.tryLock())
						{
							// I got the lock to merge file.
							ApiShared.LOGGER.info("Updating VerticalQuality LOW...");
							oldFileStructHandler.mergeOldFileStructureForVertQuality(VerticalQuality.LOW);
							ApiShared.LOGGER.info("Updating VerticalQuality MEDIUM...");
							oldFileStructHandler.mergeOldFileStructureForVertQuality(VerticalQuality.MEDIUM);
							ApiShared.LOGGER.info("Updating VerticalQuality HIGH...");
							oldFileStructHandler.mergeOldFileStructureForVertQuality(VerticalQuality.HIGH);
							ApiShared.LOGGER.info("Update completed.");
						}
						else
						{
							// Someone is already doing it. I just need to wait until he is done.
							mergeOldFileLock.lock();
							mergeOldFileLock.unlock();
						}
						ApiShared.LOGGER.info("Merge process completed.");
						return;
					}
				}
			}
		}
	}
	
	
	
	//================//
	// read from file //
	//================//
	
	/**
	 * Returns a new LodRegion at the given coordinates.
	 * Returns an empty region if the file doesn't exist.
	 */
	public LodRegion loadRegionFromFile(byte detailLevel, RegionPos regionPos, VerticalQuality verticalQuality)
	{
		// Get one from the region hot cache
		LodRegion region = regionToSave.get(regionPos);
		if (region != null && region.getMinDetailLevel() <= detailLevel &&
				region.getVerticalQuality().compareTo(verticalQuality) >= 0)
			return region; // The current hot cache to-be-saved region match our requirement.
		region = new LodRegion((byte) (LodUtil.REGION_DETAIL_LEVEL + 1), regionPos, verticalQuality);
		return loadRegionFromFile(detailLevel, region, verticalQuality);
	}
	
	/**
	 * Returns the LodRegion that is filled at the given coordinates.
	 * Returns an empty region if the file doesn't exist.
	 */
	public LodRegion loadRegionFromFile(byte detailLevel, LodRegion region, VerticalQuality verticalQuality)
	{
		if (region.getVerticalQuality().compareTo(verticalQuality) < 0)
		{
			regionToSave.put(region.getRegionPos(), region); //FIXME: The hashMap key should prob be a {regionPos,VertQual} pair. 
			region = new LodRegion((byte) (LodUtil.REGION_DETAIL_LEVEL + 1), region.getRegionPos(), verticalQuality);
		}
		int regionX = region.regionPosX;
		int regionZ = region.regionPosZ;
		
		for (byte tempDetailLevel = (byte) (region.getMinDetailLevel() - 1); tempDetailLevel >= detailLevel; tempDetailLevel--)
		{
			
			File file = getBestMatchingRegionFile(tempDetailLevel, regionX, regionZ, verticalQuality);
			if (file == null)
			{
				region.addLevelContainer(new VerticalLevelContainer(tempDetailLevel));
				continue; // Failed to find the file for this detail level. continue and try next one
			}
			
			long fileSize = file.length();
			if (fileSize == 0)
			{
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
					ApiShared.LOGGER.info("Outdated LOD region file for region: (" + regionX + "," + regionZ + ")"
							+ " version found: " + fileVersion
							+ ", version requested: " + LOD_SAVE_FILE_VERSION
							+ ". File has been deleted.");
					// This should not break, but be continue to see whether other detail levels can be loaded or updated
					region.addLevelContainer(new VerticalLevelContainer(tempDetailLevel));
				}
				else if (fileVersion > LOD_SAVE_FILE_VERSION)
				{
					// the file we are reading is a newer version,
					// close the reader and ignore the file, we don't
					// want to accidentally delete anything the user may want.
					inputStream.close();
					ApiShared.LOGGER.info("Newer LOD region file for region: (" + regionX + "," + regionZ + ")"
							+ " version found: " + fileVersion
							+ ", version requested: " + LOD_SAVE_FILE_VERSION
							+ " this region will not be written to in order to protect the newer file.");
					// This should not break, but be continue to see whether other detail levels can be loaded or updated
					region.addLevelContainer(new VerticalLevelContainer(tempDetailLevel));
				}
				else if (fileVersion < LOD_SAVE_FILE_VERSION)
				{
					ApiShared.LOGGER.debug("Old LOD region file for region: (" + regionX + "," + regionZ + ")"
							+ " version found: " + fileVersion
							+ ", version requested: " + LOD_SAVE_FILE_VERSION
							+ ". File will be loaded and updated to new format in next save.");
					// this is old, but readable version
					// read and add the data to our region
					DataInputStream dataStream = new DataInputStream(inputStream);
					region.addLevelContainer(new VerticalLevelContainer(dataStream, fileVersion, tempDetailLevel));
					dataStream.close();
					inputStream.close();
				}
				else
				{
					// this file is a readable version,
					// read and add the data to our region
					DataInputStream dataStream = new DataInputStream(inputStream);
					region.addLevelContainer(new VerticalLevelContainer(dataStream, LOD_SAVE_FILE_VERSION, tempDetailLevel));
					dataStream.close();
					inputStream.close();
				}
			}
			catch (IOException ioEx)
			{
				ApiShared.LOGGER.error("LOD file read error. Unable to read xz compressed file [" + file + "]: ", ioEx);
				region.addLevelContainer(new VerticalLevelContainer(tempDetailLevel));
			}
		} // for each detail level
		
		return region;
	}
	
	
	//==============//
	// Save to File //
	//==============//
	
	public void saveDirect(int posX, int posZ, VerticalQuality vertQual, VerticalLevelContainer dataContainer)
	{
		File file = new File(getFileBasePath() + vertQual + File.separatorChar +
				DETAIL_FOLDER_NAME_PREFIX + dataContainer.detailLevel + File.separatorChar +
				FILE_NAME_PREFIX + "." + posX + "." + posZ + FILE_EXTENSION);
		if (file.exists())
		{
			ApiShared.LOGGER.warn("LOD file write warn. Unable to write [" + file + "] because the newer version file already exist! Skipping this position...");
			return;
		}
		if (!file.getParentFile().exists())
			file.getParentFile().mkdirs();
		try
		{
			file.createNewFile();
		}
		catch (IOException e)
		{
			ApiShared.LOGGER.error("LOD file write error. Unable to create parent directory for [" + file + "]: ", e);
			return;
		}
		try (FileOutputStream fileOutStream = new FileOutputStream(file))
		{
			XZCompressorOutputStream outputStream = new XZCompressorOutputStream(fileOutStream, 3);
			// add the version of this file
			outputStream.write(LOD_SAVE_FILE_VERSION);
			// add each LodChunk to the file
			DataOutputStream dataStream = new DataOutputStream(outputStream);
			dataContainer.writeData(dataStream);
			dataStream.close();
			outputStream.close();
		}
		catch (IOException e)
		{
			ApiShared.LOGGER.error("LOD file write error. Unable to write to temp file [" + file + "]: ", e);
		}
	}
	
	
	public void addRegionsToSave(LodRegion r)
	{
		regionToSave.put(r.getRegionPos(), r);
	}
	
	private final SpamReducedLogger ramLogger = new SpamReducedLogger(1);
	
	public void dumpBufferMemoryUsage()
	{
		if (!ramLogger.canMaybeLog())
			return;
		ArrayList<LodRegion> regions = new ArrayList<LodRegion>(regionToSave.values());
		ramLogger.info("Dumping Ram Usage for file writer for {} with {} regions...",
				lodDimension.dimension.getDimensionName(), regions.size());
		int nonNullRegionCount = 0;
		int nonDirtiedRegionCount = 0;
		int writingRegionCount = 0;
		long totalUsage = 0;
		int[] detailCount = new int[LodUtil.DETAIL_OPTIONS];
		long[] detailUsage = new long[LodUtil.DETAIL_OPTIONS];
		for (LodRegion r : regions)
		{
			if (r == null)
				continue;
			nonNullRegionCount++;
			if (!r.needSaving)
				nonDirtiedRegionCount++;
			if (r.isWriting.get() != 0)
				writingRegionCount++;
			LevelContainer[] container = r.debugGetDataContainers().clone();
			if (container == null || container.length != LodUtil.DETAIL_OPTIONS)
			{
				ApiShared.LOGGER.warn("DumpRamUsage encountered an invalid region!");
				continue;
			}
			for (int i = 0; i < LodUtil.DETAIL_OPTIONS; i++)
			{
				if (container[i] == null)
					continue;
				detailCount[i]++;
				long byteUsage = container[i].getRoughRamUsage();
				detailUsage[i] += byteUsage;
				totalUsage += byteUsage;
			}
		}
		ramLogger.info("================================================");
		ramLogger.info("Non Null Regions: [{}], Non-Dirtied Regions: [{}], Writing Regions: [{}], Bytes: [{}]",
				nonNullRegionCount, nonDirtiedRegionCount, writingRegionCount, new UnitBytes(totalUsage));
		ramLogger.info("------------------------------------------------");
		for (int i = 0; i < LodUtil.DETAIL_OPTIONS; i++)
		{
			ramLogger.info("DETAIL {}: Containers: [{}], Bytes: [{}]", i, detailCount[i], new UnitBytes(detailUsage[i]));
		}
		ramLogger.info("================================================");
		ramLogger.incLogTries();
	}
	
	/** Save all dirty regions in this LodDimension to file */
	public void saveDirtyRegionsToFile(boolean blockUntilFinished)
	{
		for (int i = 0; i < lodDimension.getWidth(); i++)
		{
			for (int j = 0; j < lodDimension.getWidth(); j++)
			{
				LodRegion r = lodDimension.getRegionByArrayIndex(i, j);
				
				// FIXME: Note that the isWriting is a crude attempt at syncing. It won't work.
				// It just reduce the chance of race condition
				if (r != null && r.needSaving)
				{
					regionToSave.put(r.getRegionPos(), r);
				}
			}
		}
		trySaveRegionsToBeSaved();
		if (blockUntilFinished)
		{
			if (ENABLE_SAVE_THREAD_LOGGING)
				ApiShared.LOGGER.info("Blocking until lod file save finishes!");
			try
			{
				fileWritingThreadPool.shutdown();
				boolean worked = fileWritingThreadPool.awaitTermination(30, TimeUnit.SECONDS);
				if (!worked)
					ApiShared.LOGGER.error("File writing timed out! File data may not be saved correctly and may cause corruptions!!!");
			}
			catch (InterruptedException e)
			{
				ApiShared.LOGGER.error("File writing wait is interrupted! File data may not be saved correctly and may cause corruptions!!!: ", e);
			}
			finally
			{
				fileWritingThreadPool = Executors.newSingleThreadExecutor(new LodThreadFactory(this.getClass().getSimpleName(), Thread.NORM_PRIORITY + 1));
			}
		}
	}
	
	public void trySaveRegionsToBeSaved()
	{
		if (regionToSave.isEmpty())
			return;
		// Use Memory order Acquire to acquire any memory changes on getting this boolean
		// (Corresponding call is the this::writerMain(...)::...setRelease(false);)
		//boolean haventStarted = !isFileWritingThreadRunning.compareAndExchangeAcquire(false, true);
		// The above needs java 9!
		boolean haventStarted = isFileWritingThreadRunning.compareAndSet(false, true);
		
		if (haventStarted)
		{
			// We acquired the atomic lock.
			fileWritingThreadPool.execute(this::writerMain);
		}
	}
	
	private void writerMain()
	{
		// Use Memory order Relaxed as no additional memory changes needed to be visible.
		// (This is just a safety checks)
		// boolean isStarted = isFileWritingThreadRunning.getPlain();
		// The above needs java 9!
		boolean isStarted = isFileWritingThreadRunning.get();
		
		if (!isStarted)
			throw new ConcurrentModificationException("WriterMain Triggered but the thead state is not started!?");
		
		if (ENABLE_SAVE_THREAD_LOGGING)
			ApiShared.LOGGER.info("Lod File Writer started. To-be-written-regions: " + regionToSave.size());
		
		Instant start = Instant.now();
		// Note: Since regionToSave is a ConcurrentHashMap, and the .values() return one that support concurrency,
		//       this for loop should be safe and loop until all values are gone.
		while (!regionToSave.isEmpty())
		{
			for (LodRegion r : regionToSave.values())
			{
				
				try
				{
					if (r.isWriting.getAndIncrement() > 0)
						continue;
					//Check if the data has been swapped out right under me. Otherwise remove it from the entry
					if (!regionToSave.remove(r.getRegionPos(), r))
						continue;
					r.needSaving = false;
					Instant i = Instant.now();
					if (ENABLE_SAVE_REGION_LOGGING)
						ApiShared.LOGGER.info("Lod: Saving Region " + r.getRegionPos());
					saveRegionToFile(r);
					Instant j = Instant.now();
					Duration d = Duration.between(i, j);
					if (ENABLE_SAVE_REGION_LOGGING)
						ApiShared.LOGGER.info("Lod: Region " + r.getRegionPos() + " save finish. Took " + d);
				}
				catch (Exception e)
				{
					ApiShared.LOGGER.error("Lod: UNCAUGHT exception when saving region " + r.getRegionPos() + ": ", e);
				}
				finally
				{
					r.isWriting.decrementAndGet();
				}
			}
		}
		Instant end = Instant.now();
		
		if (ENABLE_SAVE_THREAD_LOGGING)
			ApiShared.LOGGER.info("Lod File Writer completed. Took " + Duration.between(start, end));
		
		// Use Memory order Release to release any memory changes on setting this boolean
		// (Corresponding call is the this::saveRegions(...)::...compareAndExchangeAcquire(false, true);)
		// isFileWritingThreadRunning.setRelease(false);
		// The above needs java 9!
		isFileWritingThreadRunning.set(false);
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
			File oldFile = getRegionFile(region.regionPosX, region.regionPosZ, detailLevel, region.getVerticalQuality());
			if (ENABLE_SAVE_REGION_LOGGING)
				ApiShared.LOGGER.debug("saving region [" + region.regionPosX + ", " + region.regionPosZ + "] detail " + detailLevel + " to file.");
			
			boolean isFileFullyGened = false;
			// make sure the file and folder exists
			if (!oldFile.exists())
			{
				// the file doesn't exist,
				// create it and the folder if need be
				if (!oldFile.getParentFile().exists())
					oldFile.getParentFile().mkdirs();
				
				try
				{
					oldFile.createNewFile();
				}
				catch (IOException e)
				{
					ApiShared.LOGGER.error("LOD file write error. Unable to create parent directory for [" + oldFile + "] error [" + e.getMessage() + "]: ");
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
					ApiShared.LOGGER.warn("LOD file write warning. Unable to read existing file [" + oldFile + "] version. Treating it as latest version. [" + e.getMessage() + "]: ");
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
				DataOutputStream dataStream = new DataOutputStream(outputStream);
				boolean isNewDataFullyGened = region.getLevel(detailLevel).writeData(dataStream);
				dataStream.close();
				outputStream.close();
				
				if (!isNewDataFullyGened && isFileFullyGened)
				{
					// existing file is complete while new one is only partially generate
					// this can happen is for some reason loading failed
					// this doesn't fix the bug, but at least protects old data
					ApiShared.LOGGER.error("LOD file write error. Attempted to overwrite complete region with incomplete one [" + oldFile + "]");
					try
					{
						tempFile.delete();
					}
					catch (SecurityException e)
					{
						// Failed to delete temp file... just continue.
					}
					continue;
				}
			}
			catch (IOException e)
			{
				ApiShared.LOGGER.error("LOD file write error. Unable to write to temp file [" + tempFile + "]: ", e);
				continue;
			}
			
			// overwrite the old file with the new one
			try
			{
				Files.move(tempFile.toPath(), oldFile.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			}
			catch (IOException e)
			{
				ApiShared.LOGGER.error("LOD file write error. Unable to update file [" + oldFile + "]: ", e);
			}
		}
	}
	
	//================//
	// helper methods //
	//================//
	
	/**
	 * Returns the save folder used for this dimension.
	 * 
	 * @throws RuntimeException if there was an error getting the folder
	 */
	private String getFileBasePath() throws RuntimeException
	{
		try
		{
			return dimensionDataSaveFolder.getCanonicalPath() + File.separatorChar;
		}
		catch (IOException e)
		{
			ApiShared.LOGGER.warn("Unable to get the base save file path. One possible cause is that"
					+ " the process failed to read the current path location due to security configs.");
			throw new RuntimeException("DistantHorizons Get Save File Path Failure");
		}
	}
	
	/**
	 * Return the name of the file that should contain the
	 * region at the given x and z. <br>
	 * Returns null if this object isn't available to read and write. <br><br>
	 * <p>
	 * example: "lod.0.0.txt" <br>
	 * <p>
	 * Returns null if there is an IO or security Exception.
	 */
	private File getRegionFile(int regionX, int regionZ, byte detail, VerticalQuality vertQuality)
	{
		return new File(getFileBasePath() + vertQuality + File.separatorChar +
				DETAIL_FOLDER_NAME_PREFIX + detail + File.separatorChar +
				FILE_NAME_PREFIX + "." + regionX + "." + regionZ + FILE_EXTENSION);
	}
	
	/** Returns null if no file is found */
	private File getBestMatchingRegionFile(byte detailLevel, int regionX, int regionZ, VerticalQuality targetVertQuality)
	{
		// Search from least vertQuality to max vertQuality
		do
		{
			File file = getRegionFile(regionX, regionZ, detailLevel, targetVertQuality);
			if (file.exists())
				return file; // Found target file.
			
			targetVertQuality = VerticalQuality.next(targetVertQuality);
		}
		while (targetVertQuality != null);
		
		return null;
	}
	
	
}
