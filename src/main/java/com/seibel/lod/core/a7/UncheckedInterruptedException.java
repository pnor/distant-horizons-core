package com.seibel.lod.core.a7;

public class UncheckedInterruptedException extends RuntimeException {
    public UncheckedInterruptedException(String message) {
        super(message);
    }
    public UncheckedInterruptedException(Throwable cause) {
        super(cause);
    }
    public UncheckedInterruptedException(String message, Throwable cause) {
        super(message, cause);
    }
    public UncheckedInterruptedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
    public UncheckedInterruptedException() {
        super();
    }

    public static void throwIfInterrupted() {
        if (Thread.currentThread().isInterrupted()) {
            throw new UncheckedInterruptedException();
        }
    }

    public static UncheckedInterruptedException convert(InterruptedException e) {
        return new UncheckedInterruptedException(e);
    }
}
