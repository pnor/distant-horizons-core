package com.seibel.lod.core.objects.a7.render;

import com.seibel.lod.core.objects.opengl.RenderBuffer;
import com.seibel.lod.core.objects.opengl.RenderRegion;

public abstract class RenderContainer {

    public abstract void notifyUnload();
    public abstract void notifyDispose();

    public abstract RenderRegion getRenderRegion();

}
