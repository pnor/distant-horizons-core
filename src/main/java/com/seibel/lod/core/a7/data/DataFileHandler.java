package com.seibel.lod.core.a7.data;

import com.google.common.collect.HashMultimap;
import com.seibel.lod.core.a7.RenderDataProvider;
import com.seibel.lod.core.a7.level.DHLevel;
import com.seibel.lod.core.a7.datatype.column.DataSourceSaver;
import com.seibel.lod.core.a7.pos.DhSectionPos;
import com.seibel.lod.core.a7.render.RenderDataSource;
import com.seibel.lod.core.a7.render.RenderDataSourceLoader;
import com.seibel.lod.core.util.LodUtil;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class DataFileHandler implements RenderDataProvider, Closeable {
    public static final List<OldFileConverter> CONVERTERS = new ArrayList<>();
    public static final String FILE_EXTENSION = ".lod";

    public final DHLevel level;

    public final File folder;
    // A hash map of all data files.

    public final ExecutorService IO_MANAGER = LodUtil.makeSingleThreadPool("DataFileHandler IO Manager");

    private byte maxDataLevel = 0;

    private final HashMultimap<DhSectionPos, DataFile> dataFiles;

    public static final String[] FoldersToScan = {
            "data",
    }; // TODO: Add more folders to scan

    public DataFileHandler(File folderPath, DHLevel level) {
        this.folder = folderPath;
        this.level = level;
        dataFiles = HashMultimap.create();
        // Handle converting old files that doesn't have the meta data and stuff
        List<DataFile> oldFiles = new ArrayList<>();
        for (OldFileConverter converter : CONVERTERS) oldFiles.addAll(converter.scanAndConvert(folder, level));
        oldFiles.forEach(this::_addFile);

        // Scan for files
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
    private void _addFile(DataFile file) {
        if (dataFiles.containsKey(file.pos)) {
            Set<DataFile> fileSet = dataFiles.get(file.pos);
            if (fileSet.stream().anyMatch(f -> f.dataType.equals(file.dataType))) {
                // A file with the same type and same position already exists
                // TODO: Handle this case
                return;
            }
        }
        maxDataLevel = LodUtil.max(maxDataLevel, file.dataLevel);
        dataFiles.put(file.pos, file);
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
                        _addFile(dataFile);
                    }
                }
            }
        }
    }

    public DataFile registerNewLodDataSource(LodDataSource dataSource, DataSourceSaver saver) {
        DhSectionPos pos = dataSource.getSectionPos();
        File newFile = saver.generateFilePathAndName(folder, level, pos);
        if (!newFile.getName().endsWith(FILE_EXTENSION)) {
            //TODO: Log warning
            newFile = new File(newFile.getParentFile(), newFile.getName() + FILE_EXTENSION);
        }

        if (newFile.exists()) {
            //TODO: Log warning
            String fileStr = newFile.getPath().substring(0, newFile.getPath().length() - FILE_EXTENSION.length());
            int i = 1;
            do {
                newFile = new File(fileStr + "_" + i + FILE_EXTENSION);
                i++;
            } while (newFile.exists());
        }
        DataFile dataFile = new DataFile(newFile, saver, dataSource);
        dataFiles.put(pos, dataFile);
        try {
            dataFile.save(level, false);
        } catch (Exception e) {
            dataFiles.remove(pos, dataFile);
            //TODO: Log error
            return null;
        }
        return dataFile;
    }


    @Override
    public CompletableFuture<RenderDataSource> createRenderData(RenderDataSourceLoader renderSourceLoader, DhSectionPos pos) {
        return CompletableFuture.supplyAsync(() -> {
            List<DataFile> files = renderSourceLoader.selectFiles(pos, level, getFilesInPos(pos));
            List<LodDataSource> dataSource = files.stream().map(f -> f.load(level)).filter(Objects::nonNull).collect(Collectors.toList());
            return renderSourceLoader.construct(dataSource, pos, level);
        });
    }

    @Override
    public void close() {
        IO_MANAGER.shutdown();
        try {
            IO_MANAGER.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {}
        dataFiles.values().forEach((f) -> f.close(level));
    }

    public void save() {
        //TODO: Make it free memory that is not needed
        dataFiles.values().forEach(f -> f.saveIfNeeded(level, false));
    }
}
