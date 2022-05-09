package com.seibel.lod.core.objects.a7.render;

public class EmptyRenderContainer extends RenderContainer {
    @Override
    public void notifyUnload() {

    }

    @Override
    public void notifyDispose() {

    }

    @Override
    public boolean render() {
        return true; //Always render successfully since there is nothing to render
    }
}
