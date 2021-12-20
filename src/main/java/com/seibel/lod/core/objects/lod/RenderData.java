package com.seibel.lod.core.objects.lod;

import com.seibel.lod.core.util.LodUtil;

import java.util.Arrays;

public class RenderData implements RenderDataContainer
{
	public final byte detailLevel;
	public final int size;
	
	public boolean[] hasChildToRendered;
	public boolean[] hasRenderedChild;
	public boolean[] toBeRendered;
	public boolean[] rendered;
	
	RenderData(byte detailLevel)
	{
		this.detailLevel = detailLevel;
		this.size = 1 << (LodUtil.REGION_DETAIL_LEVEL - detailLevel);
		hasChildToRendered = new boolean[size*size];
		hasRenderedChild = new boolean[size*size];
		if(detailLevel > 0)
		{
			toBeRendered = new boolean[size*size];
			rendered = new boolean[size*size];
		}
	}
	
	public void clear()
	{
		Arrays.fill(hasChildToRendered,false);
		Arrays.fill(hasRenderedChild,false);
		if(detailLevel > 0)
		{
			Arrays.fill(toBeRendered, false);
			Arrays.fill(rendered, false);
		}
	}
	
	public void setChildToRendered(int posX, int posZ, boolean newValue){
		hasChildToRendered[posX*size + posZ] = newValue;
	}
	public void setRenderedChild(int posX, int posZ, boolean newValue){
		hasRenderedChild[posX*size + posZ] = newValue;
	}
	public void setToBeRendered(int posX, int posZ, boolean newValue){
		toBeRendered[posX*size + posZ] = newValue;
	}
	public void setRendered(int posX, int posZ, boolean newValue){
		rendered[posX*size + posZ] = newValue;
	}
	
	public boolean isChildToRendered(int posX, int posZ){
		return hasChildToRendered[posX*size + posZ];
	}
	public boolean isChildRendered(int posX, int posZ){
		return hasRenderedChild[posX*size + posZ];
	}
	public boolean isToBeRendered(int posX, int posZ){
		return toBeRendered[posX*size + posZ];
	}
	public boolean isRendered(int posX, int posZ){
		return rendered[posX*size + posZ];
	}
}
