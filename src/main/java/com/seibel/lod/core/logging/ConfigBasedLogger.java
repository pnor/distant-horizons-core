/*
 *    This file is part of the Distant Horizon mod (formerly the LOD Mod),
 *    licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2020  James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
 
package com.seibel.lod.core.logging;

import com.seibel.lod.core.api.ClientApi;
import com.seibel.lod.core.enums.config.LoggerMode;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.Message;

import java.lang.ref.WeakReference;
import java.util.Arrays;
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

    private LoggerMode mode;
    private final Supplier<LoggerMode> getter;
    private final Logger logger;
    public ConfigBasedLogger(Logger logger, Supplier<LoggerMode> configQuery) {
        getter = configQuery;
        mode = getter.get();
        this.logger = logger;
        loggers.add(new WeakReference<>(this));
    }
    public void update() {
        mode = getter.get();
    }
    public boolean canMaybeLog() {return mode != LoggerMode.DISABLED;}

    public void log(Level level, String str, Object... param) {

        Message msg = logger.getMessageFactory().newMessage(str, param);
        String msgStr = msg.getFormattedMessage();
        if (mode.levelForFile.isLessSpecificThan(level)) {
            Level logLevel = level.isLessSpecificThan(Level.INFO) ? Level.INFO : level;
            if (param.length > 0 && param[param.length-1] instanceof Throwable)
                logger.log(logLevel, msgStr, (Throwable)param[param.length-1]);
            else logger.log(logLevel, msgStr);
        }
        if (mode.levelForChat.isLessSpecificThan(level)) {
            if (param.length > 0 && param[param.length-1] instanceof Throwable)
                ClientApi.logToChat(level, msgStr + "\nat\n" + Arrays.toString(((Throwable) param[param.length - 1]).getStackTrace()));
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
