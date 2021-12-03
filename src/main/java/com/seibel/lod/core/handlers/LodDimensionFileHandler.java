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
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
import com.seibel.lod.core.util.ThreadMapUtil;

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
	private final ExecutorService fileWritingThreadPool = Executors.newSingleThreadExecutor(new LodThreadFactory(this.getClass().getSimpleName()));
	
	
	
	
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
	 * Returns the LodRegion at the given coordinates.
	 * Returns an empty region if the file doesn't exist.
	 */
	public LodRegion loadRegionFromFile(byte detailLevel, RegionPos regionPos, DistanceGenerationMode generationMode, VerticalQuality verticalQuality)
	{
		int regionX = regionPos.x;
		int regionZ = regionPos.z;
		LodRegion region = new LodRegion(LodUtil.REGION_DETAIL_LEVEL, regionPos, generationMode, verticalQuality);
		
		for (byte tempDetailLevel = LodUtil.REGION_DETAIL_LEVEL; tempDetailLevel >= detailLevel; tempDetailLevel--)
		{
			String fileName = getFileNameAndPathForRegion(regionX, regionZ, generationMode, tempDetailLevel, verticalQuality);
			
			try
			{
				// if the fileName was null that means the folder is inaccessible
				// for some reason
				if (fileName == null)
					throw new IllegalArgumentException("Unable to read region [" + regionX + ", " + regionZ + "] file, no fileName.");
				
				File file = new File(fileName);
				if (!file.exists())
				{
					//there is no file for current gen mode
					//search others above current from the most to the least detailed
					VerticalQuality tempVerticalQuality = VerticalQuality.HIGH;
					do {
						DistanceGenerationMode tempGenMode = DistanceGenerationMode.FULL;
						do {
							fileName = getFileNameAndPathForRegion(regionX, regionZ, tempGenMode, tempDetailLevel, verticalQuality);
							if (fileName != null)
							{
								file = new File(fileName);
								if (file.exists())
									break;
							}
							//decrease gen mode
							if (tempGenMode == DistanceGenerationMode.FULL)
								tempGenMode = DistanceGenerationMode.FEATURES;
							else if (tempGenMode == DistanceGenerationMode.FEATURES)
								tempGenMode = DistanceGenerationMode.SURFACE;
							else if (tempGenMode == DistanceGenerationMode.SURFACE)
								tempGenMode = DistanceGenerationMode.BIOME_ONLY_SIMULATE_HEIGHT;
							else if (tempGenMode == DistanceGenerationMode.BIOME_ONLY_SIMULATE_HEIGHT)
								tempGenMode = DistanceGenerationMode.BIOME_ONLY;
							else if (tempGenMode == DistanceGenerationMode.BIOME_ONLY)
								tempGenMode = DistanceGenerationMode.NONE;
						} while (tempGenMode != generationMode);
						if (fileName != null)
						{
							file = new File(fileName);
							if (file.exists())
								break;
						}
						if (tempVerticalQuality == VerticalQuality.HIGH)
							tempVerticalQuality = VerticalQuality.MEDIUM;
						else if (tempVerticalQuality == VerticalQuality.MEDIUM)
							tempVerticalQuality = VerticalQuality.LOW;
					} while (tempVerticalQuality != verticalQuality);
					if (!file.exists())
						//there wasn't a file, don't return anything
						continue;
				}
				
				
				
				// don't try parsing empty files
				long dataSize = file.length();
				dataSize -= 1;
				if (dataSize > 0)
				{
					try (XZCompressorInputStream inputStream = new XZCompressorInputStream(new FileInputStream(file)))
					{
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
									+ ". File was been deleted.");
							
							break;
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
							
							break;
						}
						else if (fileVersion < LOD_SAVE_FILE_VERSION)
						{
							//this is old, but readable version
							byte[] data = ThreadMapUtil.getSaveContainer(tempDetailLevel);
							inputStream.read(data);
							inputStream.close();
							// add the data to our region
							region.addLevelContainer(new VerticalLevelContainer(data, fileVersion));
						} else
						{
							// this file is a readable version,
							// read the file
							byte[] data = ThreadMapUtil.getSaveContainer(tempDetailLevel);
							inputStream.read(data);
							inputStream.close();
							// add the data to our region
							region.addLevelContainer(new VerticalLevelContainer(data, LOD_SAVE_FILE_VERSION));
						}
					}
					catch (IOException ioEx)
					{
						ClientApi.LOGGER.error("LOD file read error. Unable to read to [" + fileName + "] error [" + ioEx.getMessage() + "]: ");
						ioEx.printStackTrace();
					}
				}
			}
			catch (Exception e)
			{
				// the buffered reader encountered a
				// problem reading the file
				ClientApi.LOGGER.error("LOD file read error. Unable to read to [" + fileName + "] error [" + e.getMessage() + "]: ");
				e.printStackTrace();
			}
		}// for each detail level
		
		if (region.getMinDetailLevel() >= detailLevel)
			region.growTree(detailLevel);
		
		return region;
	}
	
	
	//==============//
	// Save to File //
	//==============//
	
	/** Save all dirty regions in this LodDimension to file */
	public void saveDirtyRegionsToFileAsync()
	{
		fileWritingThreadPool.execute(saveDirtyRegionsThread);
	}
	
	private final Thread saveDirtyRegionsThread = new Thread(() ->
	{
		try
		{
			for (int i = 0; i < lodDimension.getWidth(); i++)
			{
				for (int j = 0; j < lodDimension.getWidth(); j++)
				{
					if (lodDimension.GetIsRegionDirty(i, j) && lodDimension.getRegionByArrayIndex(i, j) != null)
					{
						saveRegionToFile(lodDimension.getRegionByArrayIndex(i, j));
						lodDimension.SetIsRegionDirty(i, j, false);
					}
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	});
	
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
			String fileName = getFileNameAndPathForRegion(region.regionPosX, region.regionPosZ, region.getGenerationMode(), detailLevel, region.getVerticalQuality());
			
			// if the fileName was null that means the folder is inaccessible
			// for some reason
			if (fileName == null)
			{
				ClientApi.LOGGER.warn("Unable to save region [" + region.regionPosX + ", " + region.regionPosZ + "] to file, file is inaccessible.");
				return;
			}
			File oldFile = new File(fileName);
			//ClientProxy.LOGGER.info("saving region [" + region.regionPosX + ", " + region.regionPosZ + "] to file.");
			byte[] temp = region.getLevel(detailLevel).toDataString();
			
			try
			{
				// make sure the file and folder exists
				if (!oldFile.exists())
				{
					// the file doesn't exist,
					// create it and the folder if need be
					if (!oldFile.getParentFile().exists())
						oldFile.getParentFile().mkdirs();
					oldFile.createNewFile();
				}
				else
				{
					// the file exists, make sure it
					// is the correct version.
					// (to make sure we don't overwrite a newer
					// version file if it exists)
					int fileVersion = LOD_SAVE_FILE_VERSION;
					int isFull = 0;
					try (XZCompressorInputStream inputStream = new XZCompressorInputStream(new FileInputStream(oldFile)))
					{
						fileVersion = inputStream.read();
						inputStream.skip(1);
						isFull = inputStream.read() & 0b10000000;
						inputStream.close();
					}
					catch (IOException ex)
					{
						ex.printStackTrace();
					}
					
					// check if this file can be written to by the file handler
					if (fileVersion > LOD_SAVE_FILE_VERSION)
					{
						// the file we are reading is a newer version,
						// don't write anything, we don't want to accidentally
						// delete anything the user may want.
						return;
					}
					if ((temp[1] & 0b10000000) != 0b10000000 && isFull == 0b10000000)
					{
						// existing file is complete while new one is only partially generate
						// this can happen is for some reason loading failed
						// this doesn't fix the bug, but at least protects old data
						ClientApi.LOGGER.error("LOD file write error. Attempted to overwrite complete region with incomplete one [" + fileName + "]");
						return;
					}
					// if we got this far then we are good
					// to overwrite the old file
				}
				// the old file is good, now create a new temporary save file
				File newFile = new File(fileName + TMP_FILE_EXTENSION);
				try (XZCompressorOutputStream outputStream = new XZCompressorOutputStream(new FileOutputStream(newFile), 3))
				{
					// add the version of this file
					outputStream.write(LOD_SAVE_FILE_VERSION);
					
					// add each LodChunk to the file
					outputStream.write(temp);
					outputStream.close();
					
					// overwrite the old file with the new one
					Files.move(newFile.toPath(), oldFile.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
				}
				catch (IOException ex)
				{
					ex.printStackTrace();
				}
			}
			catch (Exception e)
			{
				ClientApi.LOGGER.error("LOD file write error. Unable to write to [" + fileName + "] error [" + e.getMessage() + "]: ");
				e.printStackTrace();
			}
		}
	}
	
	
	public void saveRegionFile (byte[] regionFile, RegionPos regionPos, DistanceGenerationMode generationMode, byte detailLevel, VerticalQuality verticalQuality)
	{
		int regionX = regionPos.x;
		int regionZ = regionPos.z;
		String fileName = getFileNameAndPathForRegion(regionX, regionZ, generationMode, detailLevel, verticalQuality);
		
		if (fileName != null)
		{
			File oldFile = new File(fileName);
			File newFile = new File(fileName + TMP_FILE_EXTENSION);
			try (OutputStream os = new FileOutputStream(newFile))
			{
				os.write(regionFile);
				Files.move(newFile.toPath(), oldFile.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
				os.close();
			}
			catch (IOException ioEx)
			{
				ClientApi.LOGGER.error("LOD file write error. Unable to write to [" + fileName + "] error [" + ioEx.getMessage() + "]: ");
				ioEx.printStackTrace();
			}
		}
	}
	
	public byte[] getRegionFile (RegionPos regionPos, DistanceGenerationMode generationMode, byte detailLevel, VerticalQuality verticalQuality)
	{
		int regionX = regionPos.x;
		int regionZ = regionPos.z;
		String fileName = getFileNameAndPathForRegion(regionX, regionZ, generationMode, detailLevel, verticalQuality);
		if (fileName != null)
		{
			File file = new File(fileName);
			try (InputStream is = new FileInputStream(file))
			{
				byte[] data = ThreadMapUtil.getSaveContainer(detailLevel);
				is.read(data);
				is.close();
				return Arrays.copyOf(data, (int) file.length());
			}
			catch (IOException ioEx)
			{
				ClientApi.LOGGER.error("LOD file read error. Unable to read to [" + fileName + "] error [" + ioEx.getMessage() + "]: ");
				ioEx.printStackTrace();
			}
		}
		return new byte[0];
	}
	
	
	//================//
	// helper methods //
	//================//
	
	public int getHashFromFile(RegionPos regionPos, DistanceGenerationMode generationMode, byte detailLevel, VerticalQuality verticalQuality)
	{
		int regionX = regionPos.x;
		int regionZ = regionPos.z;
		String fileName = getFileNameAndPathForRegion(regionX, regionZ, generationMode, detailLevel, verticalQuality);
		if (fileName == null)
			return 0;
		
		File file = new File(fileName);
		return file.hashCode();
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
	private String getFileNameAndPathForRegion(int regionX, int regionZ, DistanceGenerationMode generationMode, byte detailLevel, VerticalQuality verticalQuality)
	{
		try
		{
			// saveFolder is something like
			// ".\Super Flat\DIM-1\data\"
			// or
			// ".\Super Flat\data\"
			return dimensionDataSaveFolder.getCanonicalPath() + File.separatorChar +
					verticalQuality + File.separatorChar +
					generationMode.toString() + File.separatorChar +
					DETAIL_FOLDER_NAME_PREFIX + detailLevel + File.separatorChar +
					FILE_NAME_PREFIX + "." + regionX + "." + regionZ + FILE_EXTENSION;
		}
		catch (IOException | SecurityException e)
		{
			ClientApi.LOGGER.warn("Unable to get the filename for the region [" + regionX + ", " + regionZ + "], error: [" + e.getMessage() + "], stacktrace: ");
			e.printStackTrace();
			return null;
		}
	}
	
}
