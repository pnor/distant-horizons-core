package com.seibel.lod.core.logging;

import com.seibel.lod.core.api.ApiShared;
import com.seibel.lod.core.api.ClientApi;
import com.seibel.lod.core.enums.config.LoggerMode;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.message.Message;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class ConfigBasedSpamLogger {
    public static final List<WeakReference<ConfigBasedSpamLogger>> loggers
            = Collections.synchronizedList(new LinkedList<WeakReference<ConfigBasedSpamLogger>>());
    public static synchronized void updateAll(boolean flush) {
        loggers.removeIf((logger) -> logger.get()==null);
        loggers.forEach((logger) -> {
            ConfigBasedSpamLogger l = logger.get();
            if (l!=null) l.update();
            if (l!=null && flush) l.reset();
        });
    }

    LoggerMode mode;
    final Supplier<LoggerMode> getter;
    private final int maxLogCount;
    private final AtomicInteger logTries = new AtomicInteger(0);

    public ConfigBasedSpamLogger(Supplier<LoggerMode> configQuery, int maxLogPerSec) {
        getter = configQuery;
        mode = getter.get();
        maxLogCount = maxLogPerSec;
        loggers.add(new WeakReference<>(this));
    }
    public void reset() {logTries.set(0);}
    public boolean canMaybeLog() {return mode != LoggerMode.DISABLED && logTries.get() < maxLogCount;}
    public void update() {
        mode = getter.get();
    }

    public void log(Level level, String str, Object... param) {
        if (logTries.get() >= maxLogCount) return;
        Message msg = ApiShared.LOGGER.getMessageFactory().newMessage(str, param);
        String msgStr = msg.getFormattedMessage();
        if (mode.levelForFile.isLessSpecificThan(level)) {
            Level logLevel = level.isLessSpecificThan(Level.INFO) ? Level.INFO : level;
            if (param.length > 0 && param[param.length-1] instanceof Throwable)
                ApiShared.LOGGER.atLevel(logLevel).withLocation().withThrowable((Throwable)param[param.length-1]).log(msgStr);
            else ApiShared.LOGGER.atLevel(logLevel).withLocation().log(msgStr);
        }
        if (mode.levelForChat.isLessSpecificThan(level)) {
            if (param.length > 0 && param[param.length-1] instanceof Throwable)
                ClientApi.logToChat(level, msgStr + "\nat\n" + ((Throwable) param[param.length-1]).getStackTrace().toString());
            ClientApi.logToChat(level, msgStr);
        }
    }

    public void error(String str, Object... param) {
        log(Level.ERROR, str, param);
    }
    public void warn(String str, Object... param) {
        log(Level.WARN, str, param);
    }
    public void info(String str, Object... param) {
        log(Level.INFO, str, param);
    }
    public void debug(String str, Object... param) {
        log(Level.DEBUG, str, param);
    }
    public void trace(String str, Object... param) {
        log(Level.TRACE, str, param);
    }

    public void incLogTries() {
        logTries.getAndIncrement();
    }

    public void logInc(Level level, String str, Object... param) {
        if (logTries.getAndIncrement() >= maxLogCount) return;
        Message msg = ApiShared.LOGGER.getMessageFactory().newMessage(str, param);
        String msgStr = msg.getFormattedMessage();
        if (mode.levelForFile.isLessSpecificThan(level)) {
            Level logLevel = level.isLessSpecificThan(Level.INFO) ? Level.INFO : level;
            if (param.length > 0 && param[param.length-1] instanceof Throwable)
                ApiShared.LOGGER.atLevel(logLevel).withLocation().withThrowable((Throwable)param[param.length-1]).log(msgStr);
            else ApiShared.LOGGER.atLevel(logLevel).withLocation().log(msgStr);
        }
        if (mode.levelForChat.isLessSpecificThan(level)) {
            if (param.length > 0 && param[param.length-1] instanceof Throwable)
                ClientApi.logToChat(level, msgStr + "\nat\n" + Arrays.toString(((Throwable) param[param.length - 1]).getStackTrace()));
            ClientApi.logToChat(level, msgStr);
        }
    }

    public void errorInc(String str, Object... param) {
        logInc(Level.ERROR, str, param);
    }
    public void warnInc(String str, Object... param) {
        logInc(Level.WARN, str, param);
    }
    public void infoInc(String str, Object... param) {
        logInc(Level.INFO, str, param);
    }
    public void debugInc(String str, Object... param) {
        logInc(Level.DEBUG, str, param);
    }
    public void traceInc(String str, Object... param) {
        logInc(Level.TRACE, str, param);
    }
}
