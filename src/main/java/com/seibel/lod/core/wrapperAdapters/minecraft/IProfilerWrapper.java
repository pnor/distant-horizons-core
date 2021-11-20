package com.seibel.lod.core.wrapperAdapters.minecraft;

/**
 * 
 * 
 * @author James Seibel
 * @version 11-20-2021
 */
public interface IProfilerWrapper
{
	public void push(String newSection);
	
	public void popPush(String newSection);
	
	public void pop();
}
