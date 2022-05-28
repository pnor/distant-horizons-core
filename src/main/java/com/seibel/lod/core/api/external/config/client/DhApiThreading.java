package com.seibel.lod.core.api.external.config.client;

import com.seibel.lod.core.api.external.ExternalApiShared;
import com.seibel.lod.core.config.Config.Client.Advanced.Threading;


/**
 * General Threading settings.
 *
 * @author James Seibel
 * @version 2022-5-28
 */
public class DhApiThreading
{
	/**
	 * Returns the number of threads used to generate
	 * terrain outside the vanilla render distance.
	 */
	public static int getNumberOfWorldGeneratorThreads_v1()
	{
		return Threading.getWorldGenerationThreadPoolSize();
	}
	/** @return true if the value was set, false otherwise. */
	public static boolean setNumberOfWorldGeneratorThreads_v1(double newValue)
	{
		return ExternalApiShared.attemptToSetApiValue(Threading.numberOfWorldGenerationThreads, newValue);
	}
	
	/**
	 * Returns a number between 0.0 and 1.0, represents the expected time a world generator
	 * thread will be actively generating terrain as a percentage. <br> <br>
	 *
	 * 0.0 = active 0% of the time <br>
	 * 0.5 = active 50% of the time <br>
	 * 1.0 = active 100% of the time <br>
	 */
	public static double getWorldGeneratorThreadActivePercentage_v1()
	{
		return Threading.getWorldGenerationPartialRunTime();
	}
	
	
	/** Returns the number of threads used to rebuild geometry data. */
	public static int getNumberOfBufferBuilderThreads_v1()
	{
		return Threading.numberOfBufferBuilderThreads.get();
	}
	/** @return true if the value was set, false otherwise. */
	public static boolean setNumberOfBufferBuilderThreads_v1(int newValue)
	{
		return ExternalApiShared.attemptToSetApiValue(Threading.numberOfBufferBuilderThreads, newValue);
	}
}
