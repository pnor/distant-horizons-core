
package com.seibel.lod.core.wrapperAdapters.block;

//This class wraps the minecraft Block class
public interface IBlockShapeWrapper
{
	public boolean ofBlockToAvoid();
	
	//-----------------//
	//Avoidance getters//
	//-----------------//
	
	public boolean isNonFull();
	
	public boolean hasNoCollision();
	
	public boolean isToAvoid();
}