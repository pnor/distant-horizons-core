package com.seibel.lod.core.api.external.config.client;

import com.seibel.lod.core.api.external.apiObjects.objects.DhApiConfig_v1;
import com.seibel.lod.core.config.Config.Client.Advanced.Threading;


/**
 * General Threading settings.
 *
 * @author James Seibel
 * @version 2022-6-2
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
	 */
	public static DhApiConfig_v1<Double> getWorldGeneratorThreadConfig_v1()
	{ return new DhApiConfig_v1<>(Threading.numberOfWorldGenerationThreads); }
	
	
	/** Returns the config related to the buffer (GPU Terrain data) builder threads. */
	public static DhApiConfig_v1<Integer> getBufferBuilderThreadConfig_v1()
	{ return new DhApiConfig_v1<>(Threading.numberOfBufferBuilderThreads); }
	
}
