package com.seibel.lod.core.a7.level;

import java.util.concurrent.CompletableFuture;

public interface ILevel extends AutoCloseable {
    int getMinY();
    CompletableFuture<Void> save();

    void dumpRamUsage();
}
