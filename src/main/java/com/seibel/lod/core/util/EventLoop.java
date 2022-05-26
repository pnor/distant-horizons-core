package com.seibel.lod.core.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class EventLoop {
    private final ExecutorService executorService;
    private final Runnable runnable;
    private CompletableFuture<Void> future;
    public EventLoop(ExecutorService executorService, Runnable runnable) {
        this.executorService = executorService;
        this.runnable = runnable;
    }
    public void tick() {
        if (future != null && future.isDone()) {
            try {
                future.join();
            } catch (Exception ignored) {} finally {future = null;}
        }
        if (future == null) {
            future = CompletableFuture.runAsync(runnable, executorService);
        }
    }
    public void halt() {
        if (future != null) {
            future.cancel(true);
        }
    }
    public boolean isRunning() {
        return future != null && !future.isDone();
    }
}
