package com.seibel.lod.core.api.externalImplementation.apiObjects.wrappers;

import com.seibel.lod.core.api.external.apiObjects.wrapperInterfaces.IDhApiConfig_v1;
import com.seibel.lod.core.config.types.ConfigEntry;

/**
 * A wrapper used to interface with Distant Horizon's Config.
 *
 * @param <T>
 * @author James Seibel
 * @version 2022-6-9
 */
public class DhApiConfig_v1<T> implements IDhApiConfig_v1<T>
{
	private final ConfigEntry<T> configEntry;
	
	
	/**
	 * This constructor should only be called internally. <br>
	 * There is no reason to create this object.
	 */
	public DhApiConfig_v1(ConfigEntry<T> newConfigEntry)
	{
		this.configEntry = newConfigEntry;
	}
	
	
	public T getValue() { return this.configEntry.get(); }
	public T getTrueValue() { return this.configEntry.getTrueValue(); }
	public T getApiValue() { return this.configEntry.getApiValue(); }
	
	public boolean setValue(T newValue)
	{
		if (this.configEntry.allowApiOverride)
		{
			this.configEntry.setApiValue(newValue);
			return true;
		}
		else
		{
			return false;
		}
	}
	
	public boolean getCanBeOverrodeByApi() { return this.configEntry.allowApiOverride; }
	
	public T getDefaultValue() { return this.configEntry.getDefaultValue(); }
	public T getMaxValue() { return this.configEntry.getMax(); }
	public T getMinValue() { return this.configEntry.getMin(); }
	
}
