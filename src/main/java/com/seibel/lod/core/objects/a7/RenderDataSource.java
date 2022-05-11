package com.seibel.lod.core.objects.a7;

import com.seibel.lod.core.objects.a7.pos.DhSectionPos;
import com.seibel.lod.core.objects.a7.render.RenderContainer;

public interface RenderDataSource {
    /**
     * Returns the render container for the given section.
     * @param pos The section position.
     * @return The render container. If there are no data, returns EmptyRenderContainer.
     */
    RenderContainer createRenderData(DhSectionPos pos);
}
