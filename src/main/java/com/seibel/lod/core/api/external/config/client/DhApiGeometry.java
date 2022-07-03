package com.seibel.lod.core.api.external.config.client;

import com.seibel.lod.core.api.external.apiObjects.enums.EDhApiBufferRebuildTimes;
import com.seibel.lod.core.api.external.apiObjects.enums.EDhApiGpuUploadMethod;
import com.seibel.lod.core.api.external.apiObjects.wrapperInterfaces.IDhApiConfig;
import com.seibel.lod.core.api.implementation.objects.GenericEnumConverter;
import com.seibel.lod.core.api.implementation.wrappers.DhApiConfig;
import com.seibel.lod.core.config.Config.Client.Advanced.Buffers;
import com.seibel.lod.core.enums.config.EBufferRebuildTimes;
import com.seibel.lod.core.enums.config.EGpuUploadMethod;


/**
 * General Threading settings.
 *
 * @author James Seibel
 * @version 2022-6-13
 */
public class DhApiGeometry
{
	
	/**
	 * Returns the config related to how geometry data is
	 * uploaded to the GPU.
	 */
	public static IDhApiConfig<EDhApiGpuUploadMethod> getGpuUploadMethodConfig()
	{ return new DhApiConfig<>(Buffers.gpuUploadMethod, new GenericEnumConverter<>(EGpuUploadMethod.class, EDhApiGpuUploadMethod.class)); }
	
	/**
	 * Returns the config related to how long we should wait after
	 * uploading one Megabyte of geometry data to the GPU before uploading
	 * the next Megabyte of data.
	 */
	public static IDhApiConfig<Integer> getBufferUploadTimeoutPerMegabyteInMillisecondsConfig()
	{ return new DhApiConfig<>(Buffers.gpuUploadPerMegabyteInMilliseconds); }
	
	/**
	 * Returns the config related to how long we should wait after
	 * uploading one Megabyte of geometry data to the GPU before uploading
	 * the next Megabyte of data.
	 */
	public static IDhApiConfig<EDhApiBufferRebuildTimes> getBufferRebuildTimeConfig()
	{ return new DhApiConfig<>(Buffers.rebuildTimes, new GenericEnumConverter<>(EBufferRebuildTimes.class, EDhApiBufferRebuildTimes.class)); }
	
}
