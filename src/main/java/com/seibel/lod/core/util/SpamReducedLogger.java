package com.seibel.lod.core.util;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicInteger;

import com.seibel.lod.core.api.ClientApi;

public class SpamReducedLogger {
	private final int maxLogCount;
	private final AtomicInteger logTries = new AtomicInteger(0);
	private int sectionLogCount = -1;
	public SpamReducedLogger(int maxLogPerSec) {
		maxLogCount = maxLogPerSec;
		ClientApi.spamReducedLoggers.add(new WeakReference<SpamReducedLogger>(this));
	}
	public void reset() {logTries.set(0);}
	public boolean canMaybeLog() {return logTries.get() < maxLogCount;}

	public void info(String str, Object... param) {
		if (sectionLogCount == -1) sectionLogCount = logTries.getAndIncrement();
		if (sectionLogCount >= maxLogCount) return;
		ClientApi.LOGGER.info(str, param);
	}
	public void debug(String str, Object... param) {
		if (sectionLogCount == -1) sectionLogCount = logTries.getAndIncrement();
		if (sectionLogCount >= maxLogCount) return;
		ClientApi.LOGGER.debug(str, param);
	}
	public void warn(String str, Object... param) {
		if (sectionLogCount == -1) sectionLogCount = logTries.getAndIncrement();
		if (sectionLogCount >= maxLogCount) return;
		ClientApi.LOGGER.warn(str, param);
	}
	public void error(String str, Object... param) {
		if (sectionLogCount == -1) sectionLogCount = logTries.getAndIncrement();
		if (sectionLogCount >= maxLogCount) return;
		ClientApi.LOGGER.error(str, param);
	}
	public void incLogTries() {
		sectionLogCount = -1;
	}
	
	public void infoInc(String str, Object... param) {
		if (logTries.getAndIncrement() >= maxLogCount) return;
		ClientApi.LOGGER.info(str, param);
	}
	public void debugInc(String str, Object... param) {
		if (logTries.getAndIncrement() >= maxLogCount) return;
		ClientApi.LOGGER.debug(str, param);
	}
	public void warnInc(String str, Object... param) {
		if (logTries.getAndIncrement() >= maxLogCount) return;
		ClientApi.LOGGER.warn(str, param);
	}
	public void errorInc(String str, Object... param) {
		if (logTries.getAndIncrement() >= maxLogCount) return;
		ClientApi.LOGGER.error(str, param);
	}
}