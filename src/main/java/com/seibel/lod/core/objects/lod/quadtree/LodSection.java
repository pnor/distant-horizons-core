package com.seibel.lod.core.objects.lod.quadtree;


/*
A lod section rappresent a distinct section in a precise level of the quadtree.
The section holds all the lods information as color (computed with the blockBiome object referenced by the ID), size, light...
The save and load of a section is handled by the section itself together with a handler class.
 */
public class LodSection
{
	//level of detail of this section
	public final int detail;
	
	//level position of this section
	public final int levelPosX;
	public final int levelPosZ;
	
	//horizontal size of this section
	public final int horizontalSize;
	//vertical size of this section
	public final int verticalSize;
	
	//how many id we save for each lod
	public final int idPerLod;
	
	//What generation mode should be used for chunk in this section
	public final byte generationMode;
	
	//if present in region file, use pregenerated chunk in lod creation
	public boolean usePregeneratedChunk;
	
	//Position data hold information about each level position like generation mode used, number of vertical lods in that area...
	private byte[] positionData;
	
	//Position data hold vertical information about each lod, like minY and maxY...
	private int[] verticalData;
	
	//Lights data hold lights information about each lod, like skylight and block light values...
	private byte[] lightsData;
	
	//BlockBiomeId hold a unique ID for each lod relative to a block-biome couple. This couple is capable of generating color
	//We could just reference
	private int[] BlockBiomeId;
	
	//BlockBiomeFrequency for each lod BlockBiomeId
	private byte[] BlockBiomeFrequency;
	
	public LodSection(int detail, int horizontalSize, int verticalSize, int levelPosX, int levelPosZ, byte generationMode, boolean avoidPregeneratedChunk, int idPerLod)
	{
		this.detail = detail;
		this.levelPosX = levelPosX;
		this.levelPosZ = levelPosZ;
		this.horizontalSize = horizontalSize;
		this.verticalSize = verticalSize;
		this.generationMode = generationMode;
		this.usePregeneratedChunk = avoidPregeneratedChunk;
		this.idPerLod = idPerLod;
		//Now we should search if there is a file with this values
		//if so we load it and end the process
		
		//Otherwise we initialize this section
		positionData = new byte[horizontalSize*horizontalSize];
		verticalData = new int[horizontalSize*horizontalSize*verticalSize];
		lightsData = new byte[horizontalSize*horizontalSize*verticalSize];
		BlockBiomeId = new int[horizontalSize*horizontalSize*verticalSize*idPerLod];
		BlockBiomeFrequency = new byte[horizontalSize*horizontalSize*verticalSize*idPerLod];
	}
	
	public void mergeAndAdd(LodSection otherSection,
			int inputStartX, int inputStartZ, int inputEndX, int inputEndZ,
			int targetStartX, int targetStartZ, int targetEndX, int targetEndZ)
	{
	}
	
	public void save()
	{
	}
	
	public void tryload()
	{
	}
}
