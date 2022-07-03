package com.seibel.lod.core.api.external.config.client;

import com.seibel.lod.core.api.external.apiObjects.wrapperInterfaces.IDhApiConfig;
import com.seibel.lod.core.api.implementation.wrappers.DhApiConfig;
import com.seibel.lod.core.config.Config.Client.Advanced.Threading;


/**
 * General Threading settings.
 *
 * @author James Seibel
 * @version 2022-6-13
 */
public class DhApiThreading
{
	
	/**
	 * Returns the config related to the world generator threads. <br>
	 * <br>
	 * If the number of threads is less than 1 it will be treated as a percentage
	 * representing how often a single thread will be actively generating terrain. <br> <br>
	 *
	 * 0.0 = 1 thread active 0% of the time <br>
	 * 0.5 = 1 thread active 50% of the time <br>
	 * 1.0 = 1 thread active 100% of the time <br>
	 * 1.5 = 2 threads active 100% of the time (partial values are rounded up) <br>
	 * 2.0 = 2 threads active 100% of the time <br>
	 *
	 * @deprecated this (and the related config) should be replaced with an int
	 * 				count of threads and then a double percent active config.
	 */
	@Deprecated
	public static IDhApiConfig<Double> getWorldGeneratorThreadConfig()
	{ return new DhApiConfig<>(Threading.numberOfWorldGenerationThreads); }
	
	// TODO the above should be replaced with these
//	public static IDhApiConfig<Integer> getWorldGeneratorThreadConfig()
//	{ return new DhApiConfig<>(Threading.numberOfWorldGenerationThreads); }
	
//	public static IDhApiConfig<Double> getWorldGeneratorThreadActivePercentConfig()
//	{ return new DhApiConfig<>(Threading.ToBeDetermined); }
	
	
	/** Returns the config related to the buffer (GPU Terrain data) builder threads. */
	public static IDhApiConfig<Integer> getBufferBuilderThreadConfig()
	{ return new DhApiConfig<>(Threading.numberOfBufferBuilderThreads); }
	
}
