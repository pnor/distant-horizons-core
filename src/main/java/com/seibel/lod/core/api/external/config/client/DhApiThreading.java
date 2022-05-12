package com.seibel.lod.core.api.external.config.client;

import com.seibel.lod.core.api.external.ExternalApiShared;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton;

/**
 * General Threading settings.
 *
 * @author James Seibel
 * @version 2022-4-26
 */
public class DhApiThreading
{
	private static final ILodConfigWrapperSingleton.IClient.IAdvanced.IThreading threadSettings = ExternalApiShared.CONFIG.client().advanced().threading();
	
	
	/** Number of threads used to generate terrain outside the vanilla render distance. */
	public static int getNumberOfWorldGeneratorThreads_v1()
	{
		return threadSettings._getWorldGenerationThreadPoolSize();
	}
	
	/**
	 * Returns a number between 0.0 and 1.0.
	 *
	 * 0.0 = active 0% of the time
	 * 0.5 = active 50% of the time
	 * 1.0 = active 100% of the time
	 */
	public static double getWorldGeneratorThreadActivePercentage_v1()
	{
		return threadSettings._getWorldGenerationPartialRunTime();
	}
	
	/** Number of threads used to rebuild geometry data. */
	public static int getNumberOfBufferBuilderThreads_v1()
	{
		return threadSettings.getNumberOfBufferBuilderThreads();
	}
}
