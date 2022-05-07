package com.seibel.lod.core.objects.a7;

import com.seibel.lod.core.objects.a7.pos.DhSectionPos;

public interface RenderDataSource {
    RenderDataContainer createRenderData(DhSectionPos pos);
}
