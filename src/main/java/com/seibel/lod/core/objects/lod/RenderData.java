package com.seibel.lod.core.objects.lod;

public class RenderData implements RenderDataContainer
{
	public final byte detailLevel;
	public final int size;
	
	public byte[] hasChildToRendered;
	public byte[] hasRenderedChild;
	public boolean[] toBeRendered;
	public boolean[] rendered;
	
	RenderData(byte detailLevel)
	{
		this.detailLevel = detailLevel;
		this.size =
	}
	
	public void setChildToRendered(int posX, int posZ, boolean newValue){
		hasChildToRendered[][]
	}
	public void setRenderedChild(int posX, int posZ, boolean newValue){
	
	}
	public void setToBeRendered(int posX, int posZ, boolean newValue){
	
	}
	public void setRendered(int posX, int posZ, boolean newValue){
	
	}
	
	public boolean isChildToRendered(int posX, int posZ){
	
	}
	public boolean isChildRendered(int posX, int posZ){
	
	}
	public boolean isToBeRendered(int posX, int posZ){
	
	}
	public boolean isRendered(int posX, int posZ){
	
	}
}
