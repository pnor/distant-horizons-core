package com.seibel.lod.core.objects.a7.data;

import com.google.common.collect.HashMultimap;
import com.seibel.lod.core.objects.a7.DHLevel;
import com.seibel.lod.core.objects.a7.RenderDataProvider;
import com.seibel.lod.core.objects.a7.pos.DhSectionPos;
import com.seibel.lod.core.objects.a7.render.EmptyRenderContainer;
import com.seibel.lod.core.objects.a7.render.RenderDataSource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class DataFileHandler implements RenderDataProvider {
    public static final String FILE_EXTENSION = ".lod";

    public final DHLevel level;

    public final File folder;
    private final HashMultimap<DhSectionPos, DataFile> unloadedDataFileCache;

    public static final String[] FoldersToScan = {
            "data",
    }; // TODO: Add more folders to scan

    public DataFileHandler(File folderPath, DHLevel level) {
        this.folder = folderPath;
        this.level = level;
        unloadedDataFileCache = HashMultimap.create();
        File[] foldersToScan = new File[FoldersToScan.length + 1];
        for (int i = 0; i < FoldersToScan.length; i++) {
            foldersToScan[i] = new File(folder, FoldersToScan[i]);
        }
        foldersToScan[FoldersToScan.length] = folder;
        scanFiles(foldersToScan);
    }

    public void scanFiles(File[] foldersToScan) {
        // Scan all files in the folder and read their metadata
        for (File folder : foldersToScan) {
            if (!folder.exists() || !folder.isDirectory()) continue;
            File[] files = folder.listFiles();
            if (files == null) throw new RuntimeException("Could not list files in folder: " + folder.getAbsolutePath());

            for (File file : files) {
                if (file.isFile()) {
                    String fileName = file.getName();
                    if (fileName.endsWith(FILE_EXTENSION)) {
                        DataFile dataFile;
                        try {
                            dataFile = DataFile.readMeta(file);
                        } catch (IOException e) {
                            // FIXME: Log error
                            continue;
                        }
                        if (unloadedDataFileCache.containsKey(dataFile.pos)) {
                            Set<DataFile> fileSet = unloadedDataFileCache.get(dataFile.pos);
                            if (fileSet.stream().anyMatch(f -> f.dataType.equals(dataFile.dataType))) {
                                // A file with the same type and same position already exists
                                // TODO: Handle this case
                                continue; // For now, ignore the file
                            }
                        }
                        unloadedDataFileCache.put(dataFile.pos, dataFile);
                    }
                }
            }
        }
    }

    @Override
    public CompletableFuture<RenderDataSource> createRenderData(RenderDataSource.RenderDataSourceLoader renderSourceLoader, DhSectionPos pos) {
        return CompletableFuture.supplyAsync(() -> {
            Set<DataFile> files = renderSourceLoader.selectFiles(pos, level, unloadedDataFileCache.get(pos));
            LodDataSource[] dataSource = files.stream().map(f -> {
                try {
                    return f.load(level);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).toArray(LodDataSource[]::new);
            return renderSourceLoader.construct(dataSource, pos, level);
        });
    }

}
