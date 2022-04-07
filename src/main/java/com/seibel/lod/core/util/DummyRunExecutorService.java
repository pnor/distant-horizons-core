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
 
package com.seibel.lod.core.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class DummyRunExecutorService implements ExecutorService {
	private boolean shutdownCalled = false;
	
	@Override
	public void execute(Runnable command)
	{
		command.run();
	}

	@Override
	public void shutdown()
	{
		shutdownCalled = true;
	}

	@Override
	public List<Runnable> shutdownNow()
	{
		shutdownCalled = true;
		return new ArrayList<Runnable>();
	}

	@Override
	public boolean isShutdown()
	{
		return shutdownCalled;
	}

	@Override
	public boolean isTerminated()
	{
		return shutdownCalled;
	}

	@Override
	public boolean awaitTermination(long timeout, TimeUnit unit)
	{
		shutdownCalled = true;
		return true;
	}

	@Override
	public <T> Future<T> submit(Callable<T> task)
	{
		try
		{
			return CompletableFuture.completedFuture(task.call());
		} catch (Throwable e) {
			return CompletableFuture.supplyAsync( () -> {throw new CompletionException(e);}, Runnable::run);
		}
	}

	@Override
	public <T> Future<T> submit(Runnable task, T result)
	{
		try
		{
			task.run();
			return CompletableFuture.completedFuture(result);
		}
		catch (Throwable e)
		{
			return CompletableFuture.supplyAsync( () -> {throw new CompletionException(e);}, Runnable::run);
		}
	}

	@Override
	public Future<?> submit(Runnable task)
	{
		try
		{
			task.run();
			return CompletableFuture.<Void>completedFuture(null);
		}
		catch (Throwable e)
		{
			return CompletableFuture.supplyAsync( () -> {throw new CompletionException(e);}, Runnable::run);
		}
	}

	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
	{
		List<Future<T>> futures = new ArrayList<Future<T>>(tasks.size());
		for (Callable<T> t : tasks) {
			futures.add(submit(t));
		}
		return futures;
	}

	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
	{
		return invokeAll(tasks);
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws ExecutionException
	{
		Throwable latestE = null;
		for (Callable<T> t : tasks) {
			try {
				return t.call();
			}
			catch (Throwable e)
			{
				latestE = e;
			}
		}
		throw new ExecutionException(latestE);
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws ExecutionException
	{
		return invokeAny(tasks);
	}
	
}