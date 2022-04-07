/*
 *    This file is part of the Distant Horizon mod (formerly the LOD Mod),
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
 
package com.seibel.lod.core.handlers;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

import com.seibel.lod.core.api.ApiShared;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.seibel.lod.core.api.ClientApi;
import com.seibel.lod.core.enums.config.DistanceGenerationMode;
import com.seibel.lod.core.enums.config.VerticalQuality;
import com.seibel.lod.core.objects.lod.RegionPos;
import com.seibel.lod.core.objects.lod.VerticalLevelContainer;
import com.seibel.lod.core.util.LodUtil;

public class LodDimensionOldFileStructureHandler
{

	/** This is the dimension that owns this file handler */
	private final File dimensionDataSaveFolder;
	private final LodDimensionFileHandler newFileHandler;
	
	enum OldDistanceGenerationMode {
		NONE,
		BIOME_ONLY,
		BIOME_ONLY_SIMULATE_HEIGHT,
		SURFACE,
		FEATURES,
		FULL
	}
	
	
	
	/** lod */
	private static final String FILE_NAME_PREFIX = "lod";
	/** .txt */
	private static final String FILE_EXTENSION = ".xz";
	/** detail- */
	private static final String DETAIL_FOLDER_NAME_PREFIX = "detail-";
	
	private static final String RETIRED_OLD_STRUCT_POSTFIX = "-RETIRED-CAN-BE-DELETED";
	
	public static final int LOD_SAVE_FILE_VERSION = 8;
	public static final ExecutorService mergerThreads = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
	
	private static class TempLodRegion {
		final VerticalLevelContainer[] containers;
		final VerticalQuality vertQual;
		final int posX;
		final int posZ;
		TempLodRegion(VerticalQuality vertQual, RegionPos pos) {
			this.vertQual = vertQual;
			posX = pos.x;
			posZ = pos.z;
			containers = new VerticalLevelContainer[LodUtil.REGION_DETAIL_LEVEL+1];
		}
	}
	
	
	
	public LodDimensionOldFileStructureHandler(LodDimensionFileHandler fileHandler)
	{
		dimensionDataSaveFolder = fileHandler.dimensionDataSaveFolder;
		newFileHandler = fileHandler;
	}
	

	private void loadGenModeToRegion(TempLodRegion region, OldDistanceGenerationMode genMode)
	{
		int regionX = region.posX;
		int regionZ = region.posZ;
		for (byte detail = LodUtil.REGION_DETAIL_LEVEL; detail >= 0; detail--) {
			File file = new File(getFileBasePath() + region.vertQual + File.separatorChar +
					genMode + File.separatorChar + DETAIL_FOLDER_NAME_PREFIX + detail + File.separatorChar +
					FILE_NAME_PREFIX + "." + regionX + "." + regionZ + FILE_EXTENSION);
			if (!file.exists()) continue;
			if (!file.isFile()) continue;
			long fileSize = file.length();
			if (fileSize == 0) continue;

			try (FileInputStream fileInStream = new FileInputStream(file))
			{
				XZCompressorInputStream inputStream = new XZCompressorInputStream(fileInStream);
				int fileVersion;
				fileVersion = inputStream.read();
				
				// check if this file can be read by this file handler
				if (fileVersion < 6)
				{
					// the file we are reading is too old.
					// close the reader and delete the file.
					inputStream.close();
					ApiShared.LOGGER.info("Outdated LOD region file for region: (" + regionX + "," + regionZ + ")"
							+ " version found: " + fileVersion
							+ ", version requested: " + LOD_SAVE_FILE_VERSION
							+ ". this region file will not be read and merged into the new save structure.");
					continue;
				}
				else if (fileVersion > LOD_SAVE_FILE_VERSION)
				{
					// the file we are reading is a newer version,
					// close the reader and ignore the file, we don't
					// want to accidentally delete anything the user may want.
					inputStream.close();
					ApiShared.LOGGER.info("Unexpected newer LOD region file for region: (" + regionX + "," + regionZ + ")"
							+ " version found: " + fileVersion
							+ ", version requested: " + LOD_SAVE_FILE_VERSION
							+ " this region file will not be read and merged into the new save structure.");
					continue;
				}
				else if (fileVersion < LOD_SAVE_FILE_VERSION)
				{
					ApiShared.LOGGER.debug("Old LOD region file for region: (" + regionX + "," + regionZ + ")"
							+ " version found: " + fileVersion
							+ ", version requested: " + LOD_SAVE_FILE_VERSION
							+ ". this region file be read, updated, and merged into the new save structure.");
				}
				VerticalLevelContainer data = new VerticalLevelContainer(new DataInputStream(inputStream), fileVersion, detail);
				if (region.containers[detail] == null) {
					region.containers[detail] = data;
				} else {
					region.containers[detail].addChunkOfData(data.dataContainer, 0, 0, data.size, data.size, false);
				}
				inputStream.close();
			}
			catch (IOException ioEx)
			{
				ApiShared.LOGGER.error("LOD file read error. Unable to read xz compressed file [" + file + "] error [" + ioEx.getMessage() + "]: ");
				ioEx.printStackTrace();
			}
		}
	}
	
	private void saveRegion(TempLodRegion region) {
		for (int detail=0; detail<=LodUtil.REGION_DETAIL_LEVEL; detail++) {
			if (region.containers[detail] == null) continue;
			newFileHandler.saveDirect(region.posX, region.posZ, region.vertQual, region.containers[detail]);
		}
	}
	
	private void loadAndMergeAndSaveRegion(VerticalQuality verticalQuality, RegionPos regionPos)
	{
		ApiShared.LOGGER.info("Merging region "+regionPos+" at "+verticalQuality+"...");
		TempLodRegion region = new TempLodRegion(verticalQuality, regionPos);
		ApiShared.LOGGER.info("Reading data...");
		loadGenModeToRegion(region, OldDistanceGenerationMode.FULL);
		loadGenModeToRegion(region, OldDistanceGenerationMode.FEATURES);
		loadGenModeToRegion(region, OldDistanceGenerationMode.SURFACE);
		loadGenModeToRegion(region, OldDistanceGenerationMode.BIOME_ONLY_SIMULATE_HEIGHT);
		loadGenModeToRegion(region, OldDistanceGenerationMode.BIOME_ONLY);
		loadGenModeToRegion(region, OldDistanceGenerationMode.NONE);
		ApiShared.LOGGER.info("Writing data...");
		saveRegion(region);
		ApiShared.LOGGER.info("region "+regionPos+" at "+verticalQuality+" merged");
	}
	
	
	
	private RegionPos parseFileName(String fileName) {
		if (!fileName.endsWith(FILE_EXTENSION)) return null;
		if (!fileName.startsWith(FILE_NAME_PREFIX)) return null;
		String[] array = fileName.split("\\."); // Array content: "lod", "-1", "1", ".xz"
		if (array.length!=4) return null;
		try {
			return new RegionPos(Integer.parseInt(array[1]), Integer.parseInt(array[2]));
		} catch (NumberFormatException e) {
			return null;
		}
	}
	
	private HashSet<RegionPos> scanOldRegionFiles(VerticalQuality vertQual, OldDistanceGenerationMode genMode) {
		HashSet<RegionPos> result = new HashSet<RegionPos>();
		File baseBaseFolder = new File(getFileBasePath() + vertQual + File.separatorChar + genMode);
		if (!baseBaseFolder.exists()) return result;
		for (byte detail=0; detail <= LodUtil.REGION_DETAIL_LEVEL; detail++) {
			File baseFolder = new File(getFileBasePath() + vertQual + File.separatorChar +
					genMode + File.separatorChar + DETAIL_FOLDER_NAME_PREFIX + detail);
			if (!baseFolder.exists()) continue;
			if (!baseFolder.isDirectory()) continue;
			File[] subFiles = baseFolder.listFiles();
			for (File subFile : subFiles) {
				if (!subFile.isFile()) continue;
				if (!subFile.canRead()) continue;
				RegionPos pos = parseFileName(subFile.getName());
				if (pos != null) result.add(pos);
			}
		}
		return result;
	}
	
	private void renameOldFileStructure(VerticalQuality vertQual, OldDistanceGenerationMode genMode) {
		File baseBaseFolder = new File(getFileBasePath() + vertQual + File.separatorChar + genMode);
		if (!baseBaseFolder.exists()) return;
		baseBaseFolder.renameTo(new File(getFileBasePath() + vertQual + File.separatorChar + genMode + RETIRED_OLD_STRUCT_POSTFIX));
	}
	
	public void mergeOldFileStructureForVertQuality(VerticalQuality vertQual) {
		File baseFile = new File(getFileBasePath() + vertQual);
		if (!baseFile.exists()) return;
		if (!baseFile.isDirectory()) return;
		HashSet<RegionPos> totalPos = new HashSet<RegionPos>();
		totalPos.addAll(scanOldRegionFiles(vertQual, OldDistanceGenerationMode.NONE));
		totalPos.addAll(scanOldRegionFiles(vertQual, OldDistanceGenerationMode.BIOME_ONLY));
		totalPos.addAll(scanOldRegionFiles(vertQual, OldDistanceGenerationMode.BIOME_ONLY_SIMULATE_HEIGHT));
		totalPos.addAll(scanOldRegionFiles(vertQual, OldDistanceGenerationMode.SURFACE));
		totalPos.addAll(scanOldRegionFiles(vertQual, OldDistanceGenerationMode.FEATURES));
		totalPos.addAll(scanOldRegionFiles(vertQual, OldDistanceGenerationMode.FULL));
		ArrayList<Future<?>> futures = new ArrayList<Future<?>>();
		for (RegionPos pos : totalPos) {
			futures.add(mergerThreads.submit(() -> {
				loadAndMergeAndSaveRegion(vertQual, pos);
				return true;
			}));
		}
		futures.forEach(t ->
		{
			try
			{
				t.get();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		});
		
		renameOldFileStructure(vertQual, OldDistanceGenerationMode.NONE);
		renameOldFileStructure(vertQual, OldDistanceGenerationMode.BIOME_ONLY);
		renameOldFileStructure(vertQual, OldDistanceGenerationMode.BIOME_ONLY_SIMULATE_HEIGHT);
		renameOldFileStructure(vertQual, OldDistanceGenerationMode.SURFACE);
		renameOldFileStructure(vertQual, OldDistanceGenerationMode.FEATURES);
		renameOldFileStructure(vertQual, OldDistanceGenerationMode.FULL);
	}

	private String getFileBasePath()
	{
		try
		{
			return dimensionDataSaveFolder.getCanonicalPath() + File.separatorChar;
		}
		catch (IOException e)
		{
			ApiShared.LOGGER.warn("Unable to get the base save file path. Error: " + e.getMessage(), e);
			throw new RuntimeException("DistantHorizons Get Save File Path Failure");
		}
	}
	
}
