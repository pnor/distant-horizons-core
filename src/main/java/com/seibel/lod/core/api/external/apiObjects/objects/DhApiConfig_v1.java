package com.seibel.lod.core.api.external.apiObjects.objects;

import com.seibel.lod.core.config.types.ConfigEntry;

/**
 * A wrapper used to interface with Distant Horizon's Config.
 *
 * @param <T>
 * @author James Seibel
 * @version 2022-6-2
 */
public class DhApiConfig_v1<T>
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
	
	
	/**
	 * Returns the active value for this config. <br>
	 * Returns the True value if either the config cannot be overridden by
	 * the API or if it hasn't been set by the API.
	 */
	public T getValue() { return this.configEntry.get(); }
	/**
	 * Returns the value held by this config. <br>
	 * This is the value stored in the config file.
	 */
	public T getTrueValue() { return this.configEntry.getTrueValue(); }
	/**
	 * Returns the value of the config if it was set by the API.
	 * Returns null if the config wasn't set by the API.
	 */
	public T getApiValue() { return this.configEntry.getApiValue(); }
	
	/**
	 * Sets the config's value. <br>
	 * If the newValue is set to null then the config
	 * will revert to using the True Value.<br>
	 * If the config cannot be set via the API this method will return false.
	 *
	 * @return true if the value was set, false otherwise.
	 */
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
	
	/** Returns true if this config can be set via the API, false otherwise. */
	public boolean getCanBeOverrodeByApi() { return this.configEntry.allowApiOverride; }
	
	/** Returns the default value for this config. */
	public T getDefaultValue() { return this.configEntry.getDefaultValue(); }
	/** Returns the max value for this config, null if there is no max. */
	public T getMaxValue() { return this.configEntry.getMax(); }
	/** Returns the min value for this config, null if there is no min. */
	public T getMinValue() { return this.configEntry.getMin(); }
	
}
