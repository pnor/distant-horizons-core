package com.seibel.lod.core.objects.a7.data;

import com.seibel.lod.core.objects.a7.RenderDataContainer;
import com.seibel.lod.core.objects.a7.RenderDataSource;
import com.seibel.lod.core.objects.a7.pos.DhSectionPos;

import java.io.File;

public class DataHandler implements RenderDataSource {
    public final File folder;

    public DataHandler(File folderPath) {
        this.folder = folderPath;
    }

    @Override
    public RenderDataContainer createRenderData(DhSectionPos pos) {


        //TODO
        return null;
    }
}
