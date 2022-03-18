package com.seibel.lod.core.objects.lod.quadtree;

import com.seibel.lod.core.util.DetailDistanceUtil;

public class QuadTreeProperties
{
	public int MAX_NUMBER_OF_DETAIL=23;
	public int SECTION_SIZE=128;
	public int[] absoluteDetailCircleSize = new int[MAX_NUMBER_OF_DETAIL];
	public int[] relativeDetailCircleSize = new int[MAX_NUMBER_OF_DETAIL];
	public int[] generationModeOfDetail = new int[MAX_NUMBER_OF_DETAIL];
	
	public LodQuadTree lodQuadTree;
	public RenderQuadTree renderQuadTree;
	
	public void update()
	{
		for(int detail = 0; detail < MAX_NUMBER_OF_DETAIL; detail++)
		{
			//Compute circle distance for this detail in term of blocks
			absoluteDetailCircleSize[detail] = DetailDistanceUtil.getDrawDistanceFromDetail(detail);
			//Compute circle distance in terms of number of section
			relativeDetailCircleSize[detail] = (int) Math.ceil(absoluteDetailCircleSize[detail]/(SECTION_SIZE*2^detail));
		}
		updateGridSize();
	}
	
	private void updateGridSize()
	{
		for(int detail = 0; detail < MAX_NUMBER_OF_DETAIL; detail++)
		{
			//Use this value to change to update the size of the matrices
			//relativeDetailCircleSize[detail];
		}
	}
}
