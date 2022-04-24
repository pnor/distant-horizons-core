/*
 *    This file is part of the Distant Horizons mod (formerly the LOD Mod),
 *    licensed under the GNU LGPL v3 License.
 *
 *    Copyright (C) 2020-2022  James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.seibel.lod.core.util;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.concurrent.ThreadFactory;

import com.seibel.lod.core.api.ApiShared;
import com.seibel.lod.core.api.ClientApi;

/**
 * Just a simple ThreadFactory to name ExecutorService
 * threads, which can be helpful when debugging.
 * @author James Seibel
 * @version 8-15-2021
 */
public class LodThreadFactory implements ThreadFactory
{
	public final String threadName;
	public final int priority;
	private int threadCount = 0;
	private LinkedList<WeakReference<Thread>> threads = new LinkedList<WeakReference<Thread>>();
	
	
	public LodThreadFactory(String newThreadName, int priority)
	{
		if (priority < 1 || priority > 10) throw new IllegalArgumentException("Thread priority should be [1-10]!");
		threadName = newThreadName + " Thread";
		this.priority = priority;
	}
	
	@Override
	public Thread newThread(Runnable r)
	{
		Thread t = new Thread(r, threadName + "[" + (threadCount++) + "]");
		t.setPriority(priority);
		threads.add(new WeakReference<Thread>(t));
		return t;
	}
	
	private static String StackTraceToString(StackTraceElement[] e) {
		StringBuilder str = new StringBuilder();
		str.append(e[0]);
		str.append('\n');
		for (int i = 1; i<e.length; i++) {
			str.append("  at ");
			str.append(e[i]);
			str.append('\n');
		}
		return str.toString();
	}
	
	public void dumpAllThreadStacks() {
		for (WeakReference<Thread> tRef : threads) {
			Thread t = tRef.get();
			if (t != null) {
				StackTraceElement[] stacks = t.getStackTrace();
				if (stacks.length != 0) {
					ApiShared.LOGGER.info("===========================================\n"
							+ "Thread: "+t.getName()+"\n"+StackTraceToString(stacks));
				}
			}
		}
		threads.removeIf((weakRef) -> {return weakRef.get() == null;});
	}
	
}
