package com.seibel.lod.core.objects.a7.data;

import com.google.common.collect.HashMultimap;
import com.seibel.lod.core.objects.Pos2D;
import com.seibel.lod.core.objects.a7.DHLevel;
import com.seibel.lod.core.objects.a7.RenderDataProvider;
import com.seibel.lod.core.objects.a7.pos.DhSectionPos;
import com.seibel.lod.core.objects.a7.render.RenderDataSource;
import com.seibel.lod.core.objects.a7.render.RenderDataSourceLoader;
import com.seibel.lod.core.util.LodUtil;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class DataFileHandler implements RenderDataProvider {
    public static final String FILE_EXTENSION = ".lod";

    public final DHLevel level;

    public final File folder;
    // A hash map of all data files.

    private byte maxDataLevel = 0;

    private final HashMultimap<DhSectionPos, DataFile> dataFiles;

    public static final String[] FoldersToScan = {
            "data",
    }; // TODO: Add more folders to scan

    public DataFileHandler(File folderPath, DHLevel level) {
        this.folder = folderPath;
        this.level = level;
        dataFiles = HashMultimap.create();

        File[] foldersToScan = new File[FoldersToScan.length + 1];
        for (int i = 0; i < FoldersToScan.length; i++) {
            foldersToScan[i] = new File(folder, FoldersToScan[i]);
        }
        foldersToScan[FoldersToScan.length] = folder;
        scanFiles(foldersToScan);
    }
    private List<DataFile>[] getFilesInPos(DhSectionPos pos) {
        List<DataFile>[] files = new LinkedList[maxDataLevel + 1];
        for (DhSectionPos p : dataFiles.keySet()) {
            if (p.overlaps(pos)) {
                for (DataFile f : dataFiles.get(p)) {
                    files[f.dataLevel].add(f);
                }
            }
        }
        return files;
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
                        if (dataFiles.containsKey(dataFile.pos)) {
                            Set<DataFile> fileSet = dataFiles.get(dataFile.pos);
                            if (fileSet.stream().anyMatch(f -> f.dataType.equals(dataFile.dataType))) {
                                // A file with the same type and same position already exists
                                // TODO: Handle this case
                                continue; // For now, ignore the file
                            }
                        }
                        maxDataLevel = LodUtil.max(maxDataLevel, dataFile.dataLevel);
                        dataFiles.put(dataFile.pos, dataFile);
                    }
                }
            }
        }
    }


    @Override
    public CompletableFuture<RenderDataSource> createRenderData(RenderDataSourceLoader renderSourceLoader, DhSectionPos pos) {
        return CompletableFuture.supplyAsync(() -> {
            List<DataFile> files = renderSourceLoader.selectFiles(pos, level, getFilesInPos(pos));
            List<LodDataSource> dataSource = files.stream().map(f -> {
                try {
                    return f.load(level);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).collect(Collectors.toList());
            return renderSourceLoader.construct(dataSource, pos, level);
        });
    }

}
