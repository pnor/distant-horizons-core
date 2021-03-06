package com.seibel.lod.core.api.implementation.interfaces;


/**
 * Interface used for converting Core and API objects.
 *
 * @param <CoreType> The type used by DH Core (not visible to the API user)
 * @param <ApiType> The type used by DH API (not used by Core)
 * @author James Seibel
 * @version 2022-6-30
 */
public interface IConverter<CoreType, ApiType>
{
	
	CoreType convertToCoreType(ApiType apiObject);
	
	ApiType convertToApiType(CoreType coreObject);
	
}

