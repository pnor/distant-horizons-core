package com.seibel.lod.core.objects;

/**
 * Used when setting up configuration fields.
 * 
 * @author James Seibel
 * @version 11-14-2021
 * @param <T> The type of data this object is storing
 */
public class MinDefaultMax<T>
{
	public T minValue;
	public T defaultValue;
	public T maxValue;
	
	public MinDefaultMax(T newMinValue, T newDefaultValue, T newMaxValue)
	{
		minValue = newMinValue;
		defaultValue = newDefaultValue;
		maxValue = newMaxValue;
	}
}
