package com.seibel.lod.core.objects.lod.quadtree;

import com.seibel.lod.core.util.DetailDistanceUtil;

public class LodSection
{
	public final int detail;
	public final int levelPosX;
	public final int levelPosZ;
	public final int horizontalSize;
	public final int verticalSize;
	public final int numberOfBlockBiomePerLod;
	public final byte sectionGenerationMode;
	public boolean avoidPregeneratedChunk;
	
	//Position data hold information about each level position like generation mode used, number of vertical lods in that area...
	private byte[] positionData;
	
	//Position data hold vertical information about each lod, like minY and maxY...
	private int[] verticalData;
	
	//Lights data hold lights information about each lod, like skylight and block light values...
	private byte[] lightsData;
	
	//BlockBiomeId hold a unique ID for each lod relative to a block-biome couple. This couple is capable of generating color
	//We could just reference
	private int[] BlockBiomeId;
	
	//BlockBiomeFrequency for each
	private byte[] BlockBiomeFrequency;
	
	public LodSection(int detail, int horizontalSize, int verticalSize, int levelPosX, int levelPosZ, byte sectionGenerationMode, boolean avoidPregeneratedChunk, int numberOfBlockBiomePerLod)
	{
		this.detail = detail;
		this.levelPosX = levelPosX;
		this.levelPosZ = levelPosZ;
		this.horizontalSize = horizontalSize;
		this.verticalSize = verticalSize;
		this.sectionGenerationMode = sectionGenerationMode;
		this.avoidPregeneratedChunk = avoidPregeneratedChunk;
		this.numberOfBlockBiomePerLod = numberOfBlockBiomePerLod;
		//Now we should search if there is a file with this values
		//if so we load it and end the process
		
		//Otherwise we initialize this section
		positionData = new byte[horizontalSize*horizontalSize];
		verticalData = new int[horizontalSize*horizontalSize*verticalSize];
		lightsData = new byte[horizontalSize*horizontalSize*verticalSize];
		BlockBiomeId = new int[horizontalSize*horizontalSize*verticalSize*numberOfBlockBiomePerLod];
		BlockBiomeFrequency = new byte[horizontalSize*horizontalSize*verticalSize*numberOfBlockBiomePerLod];
	}
}
