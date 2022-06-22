package com.seibel.lod.core.a7.io.file;

import com.google.common.collect.HashMultimap;
import com.seibel.lod.core.a7.data.LodDataSource;
import com.seibel.lod.core.a7.datatype.full.FullDatatype;
import com.seibel.lod.core.a7.level.IServerLevel;
import com.seibel.lod.core.a7.pos.DhSectionPos;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.a7.level.DhClientServerLevel;
import com.seibel.lod.core.util.LodUtil;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

public class LocalDataFileHandler implements DataSourceProvider {
    // Note: Single main thread only for now. May make it multi-thread later, depending on the usage.
	ExecutorService fileReaderThread = LodUtil.makeSingleThreadPool("FileReaderThread");
    Logger logger = DhLoggerBuilder.getLogger("LocalDataFileHandler");

    ConcurrentHashMap<DhSectionPos, DataMetaFile> files = new ConcurrentHashMap<>();

    boolean isScanned = false;

    File saveDir;
    final IServerLevel level;
    public LocalDataFileHandler(IServerLevel level, File saveRootDir) {
        this.saveDir = saveRootDir;
        this.level = level;
    }

    /*
    * Caller must ensure that this method is called only once,
    *  and that this object is not used before this method is called.
     */
    public void addScannedFile(Collection<File> detectedFiles) {
        HashMultimap<DhSectionPos, DataMetaFile> filesByPos = HashMultimap.create();
        { // Sort files by pos.
            for (File file : detectedFiles) {
                try {
                    DataMetaFile metaFile = new DataMetaFile(level, file);
                    filesByPos.put(metaFile.pos, metaFile);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        // Warn for multiple files with the same pos, and then select the one with latest timestamp.
        for (DhSectionPos pos : filesByPos.keySet()) {
            Collection<DataMetaFile> metaFiles = filesByPos.get(pos);
            DataMetaFile fileToUse;
            if (metaFiles.size() > 1) {
                fileToUse = Collections.max(metaFiles, Comparator.comparingLong(a -> a.timestamp));
                {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Multiple files with the same pos: ");
                    sb.append(pos);
                    sb.append("\n");
                    for (DataMetaFile metaFile : metaFiles) {
                        sb.append("\t");
                        sb.append(metaFile.path);
                        sb.append("\n");
                    }
                    sb.append("\tUsing: ");
                    sb.append(fileToUse.path);
                    sb.append("\n");
                    sb.append("(Other files will be renamed by appending \".old\" to their name.)");
                    logger.warn(sb.toString());

                    // Rename all other files with the same pos to .old
                    for (DataMetaFile metaFile : metaFiles) {
                        if (metaFile == fileToUse) continue;
                        File oldFile = new File(metaFile.path + ".old");
                        try {
                            if (!metaFile.path.renameTo(oldFile)) throw new RuntimeException("Renaming failed");
                        } catch (Exception e) {
                            logger.error("Failed to rename file: " + metaFile.path + " to " + oldFile, e);
                        }
                    }
                }
            } else {
                fileToUse = metaFiles.iterator().next();
            }
            // Add file to the list of files.
            files.put(pos, fileToUse);
        }
    }

    /*
    * This call is concurrent. I.e. it supports multiple threads calling this method at the same time.
     */
    @Override
    public CompletableFuture<LodDataSource> read(DhSectionPos pos) {
        DataMetaFile metaFile = files.get(pos);
        if (metaFile == null) {
            return CompletableFuture.completedFuture(null);
        }
        return metaFile.loadOrGetCached(fileReaderThread);
    }

    /*
    * This call is concurrent. I.e. it supports multiple threads calling this method at the same time.
     */
    @Override
    public void write(DhSectionPos sectionPos, FullDatatype chunkData) {
        DataMetaFile metaFile = files.get(sectionPos);
        if (metaFile != null) { // Fast path: if there is a file for this section, just write to it.
            metaFile.addToWriteQueue(chunkData);
            return;
        }
        // Slow path: if there is no file for this section, create one.

        DataMetaFile newMetaFile = new DataMetaFile(level, saveDir, sectionPos);

        // We add to the queue first so on CAS onto the map, no other thread
        // will see the new file without our write entry.
        newMetaFile.addToWriteQueue(chunkData);
        DataMetaFile casResult = files.putIfAbsent(sectionPos, newMetaFile); // This is a CAS with expected null value.
        if (casResult != null) { // another thread already created the file. CAS failed.
            // Drop our version and use the cas result.
            casResult.addToWriteQueue(chunkData);
        }
    }

    /*
     * This call is concurrent. I.e. it supports multiple threads calling this method at the same time.
     */
    @Override
    public CompletableFuture<Void> flushAndSave() {
        ArrayList<CompletableFuture<Void>> futures = new ArrayList<CompletableFuture<Void>>();
        for (DataMetaFile metaFile : files.values()) {
            futures.add(metaFile.flushAndSave(fileReaderThread));
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    private File computeDefaultFilePath(DhSectionPos pos) { //TODO: Temp code as we haven't decided on the file naming & location yet.
        return new File(saveDir, pos.serialize() + ".lod");
    }

    @Override
    public void close() {
         //TODO
    }
}
