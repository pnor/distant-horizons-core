package com.seibel.lod.core.api.implementation.wrappers;

import com.seibel.lod.core.api.external.apiObjects.wrapperInterfaces.IDhApiConfig;
import com.seibel.lod.core.config.types.ConfigEntry;

/**
 * A wrapper used to interface with Distant Horizon's Config.
 *
 * @param <T>
 * @author James Seibel
 * @version 2022-6-13
 */
public class DhApiConfig<T> implements IDhApiConfig<T>
{
	private final ConfigEntry<T> configEntry;
	
	
	/**
	 * This constructor should only be called internally. <br>
	 * There is no reason to create this object.
	 */
	public DhApiConfig(ConfigEntry<T> newConfigEntry)
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
