package com.seibel.lod.core.a7.save.io.render;

import com.seibel.lod.core.a7.save.io.MetaFile;
import com.seibel.lod.core.a7.pos.DhSectionPos;

import java.io.File;
import java.io.IOException;

public class RenderMetaFile extends MetaFile {

    protected RenderMetaFile(File path) throws IOException {
        super(path);
    }

    protected RenderMetaFile(File path, DhSectionPos pos) {
        super(path, pos);
    }
}
