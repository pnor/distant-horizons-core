package com.seibel.lod.core.a7.data;

import com.seibel.lod.core.a7.level.DHLevel;

import java.io.File;
import java.util.List;

public interface OldFileConverter {
    List<DataFile> scanAndConvert(File levelFolder, DHLevel level);
}
