package com.seibel.lod.core.logging;

import com.seibel.lod.core.api.ApiShared;
import com.seibel.lod.core.api.ClientApi;
import com.seibel.lod.core.enums.config.LoggerMode;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.message.Message;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

public class ConfigBasedLogger {
    public static final List<WeakReference<ConfigBasedLogger>> loggers
            = Collections.synchronizedList(new LinkedList<WeakReference<ConfigBasedLogger>>());
    public static synchronized void updateAll() {
        loggers.removeIf((logger) -> logger.get()==null);
        loggers.forEach((logger) -> {
            ConfigBasedLogger l = logger.get();
            if (l!=null) l.update();
        });
    }

    LoggerMode mode;
    final Supplier<LoggerMode> getter;
    public ConfigBasedLogger(Supplier<LoggerMode> configQuery) {
        getter = configQuery;
        mode = getter.get();
        loggers.add(new WeakReference<>(this));
    }
    public void update() {
        mode = getter.get();
    }
    public boolean canMaybeLog() {return mode != LoggerMode.DISABLED;}

    public void log(Level level, String str, Object... param) {

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
}
