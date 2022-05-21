package com.seibel.lod.core.objects.a7.data;

import com.seibel.lod.core.objects.a7.DHLevel;

import java.io.File;
import java.util.List;

public interface OldFileConverter {
    List<DataFile> scanAndConvert(File levelFolder, DHLevel level);
}
