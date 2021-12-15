/*
 *    This file is part of the Distant Horizon mod (formerly the LOD Mod),
 *    licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2020  James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.seibel.lod.core.objects.lod;

import com.seibel.lod.core.dataFormat.*;
import com.seibel.lod.core.enums.config.DistanceGenerationMode;
import com.seibel.lod.core.util.*;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftWrapper;

/**
 *
 * @author Leonardo Amato
 * @version ??
 */
public class VerticalLevelContainer implements LevelContainer
{
	private final short minHeight = SingletonHandler.get(IMinecraftWrapper.class).getWrappedClientWorld().getMinHeight();
	public final byte detailLevel;
	public final int size;
	public final int verticalSize;
	
	public final long[] dataContainer;
	
	//Currently these variable are not used. We are going to use them in the new data format
	public final int[] verticalDataContainer = null;
	public final int[] colorDataContainer = null;
	public final byte[] lightDataContainer = null;
	public final short[] positionDataContainer = null;
	
	/*WE PROBABLY ARE GOING TO USE THIS IN THE FUTURE
	FOR NOW WE KEEP THE OLD SYSTEM
	public final short[] sectionVerticalSize = null;
	public final int[][] verticalDataContainer = null;
	public final int[][] colorDataContainer = null;
	public final byte[][] lightDataContainer = null;
	public final short[] positionDataContainer = null;
	*/
	
	public VerticalLevelContainer(byte detailLevel)
	{
		this.detailLevel = detailLevel;
		size = 1 << (LodUtil.REGION_DETAIL_LEVEL - detailLevel);
		verticalSize = DetailDistanceUtil.getMaxVerticalData(detailLevel);
		dataContainer = new long[size * size * DetailDistanceUtil.getMaxVerticalData(detailLevel)];
	}
	
	@Override
	public byte getDetailLevel()
	{
		return detailLevel;
	}
	
	@Override
	public void clear(int posX, int posZ)
	{
		for (int verticalIndex = 0; verticalIndex < verticalSize; verticalIndex++)
			dataContainer[posX * size * verticalSize + posZ * verticalSize + verticalIndex] = DataPointUtil.EMPTY_DATA;
	}
	
	@Override
	public boolean addData(long data, int posX, int posZ, int verticalIndex)
	{
		dataContainer[posX * size * verticalSize + posZ * verticalSize + verticalIndex] = data;
		return true;
	}
	
	@Override
	public boolean addVerticalData(long[] data, int posX, int posZ)
	{
		for (int verticalIndex = 0; verticalIndex < verticalSize; verticalIndex++)
			dataContainer[posX * size * verticalSize + posZ * verticalSize + verticalIndex] = data[verticalIndex];
		return true;
	}
	
	@Override
	public boolean addSingleData(long data, int posX, int posZ)
	{
		return addData(data, posX, posZ, 0);
	}
	
	@Override
	public long getData(int posX, int posZ, int verticalIndex)
	{
		return dataContainer[posX * size * verticalSize + posZ * verticalSize + verticalIndex];
	}
	
	public short getPositionData(int posX, int posZ)
	{
		return positionDataContainer[posX * size + posZ];
	}
	
	public int getVerticalData(int posX, int posZ, int verticalIndex)
	{
		return verticalDataContainer[posX * size * verticalSize + posZ * verticalSize + verticalIndex];
	}
	
	public int getColorData(int posX, int posZ, int verticalIndex)
	{
		return colorDataContainer[posX * size * verticalSize + posZ * verticalSize + verticalIndex];
	}
	
	public byte getLightData(int posX, int posZ, int verticalIndex)
	{
		return lightDataContainer[posX * size * verticalSize + posZ * verticalSize + verticalIndex];
	}
	
	public void setPositionData(short positionData, int posX, int posZ)
	{
		positionDataContainer[posX * size + posZ] = positionData;
	}
	
	public void setVerticalData(int verticalData, int posX, int posZ, int verticalIndex)
	{
		verticalDataContainer[posX * size * verticalSize + posZ * verticalSize + verticalIndex] = verticalData;
	}
	
	public void setColorData(int colorData, int posX, int posZ, int verticalIndex)
	{
		colorDataContainer[posX * size * verticalSize + posZ * verticalSize + verticalIndex] = colorData;
	}
	
	public void setLightData(byte lightData, int posX, int posZ, int verticalIndex)
	{
		lightDataContainer[posX * size * verticalSize + posZ * verticalSize + verticalIndex] = lightData;
	}
	
	@Override
	public long getSingleData(int posX, int posZ)
	{
		return dataContainer[posX * size * verticalSize + posZ * verticalSize];
	}
	
	@Override
	public int getVerticalSize()
	{
		return verticalSize;
	}
	
	public int getSize()
	{
		return size;
	}
	
	@Override
	public boolean doesItExist(int posX, int posZ)
	{
		return DataPointUtil.doesItExist(getSingleData(posX, posZ));
	}
	
	public VerticalLevelContainer(byte[] inputData, int version)
	{
		int tempMaxVerticalData;
		int tempIndex;
		int index = 0;
		long newData;
		detailLevel = inputData[index];
		index++;
		tempMaxVerticalData = inputData[index] & 0b01111111;
		index++;
		size = 1 << (LodUtil.REGION_DETAIL_LEVEL - detailLevel);
		int x = size * size * tempMaxVerticalData;
		long[] tempDataContainer = new long[x];
		
		if (version == 6)
		{
			for (int i = 0; i < x; i++)
			{
				newData = 0;
				for (tempIndex = 0; tempIndex < 8; tempIndex++)
					newData += (((long) inputData[index + tempIndex]) & 0xff) << (8 * tempIndex);
				index += 8;
				
				newData = DataPointUtil.createDataPoint(
						DataPointUtil.getAlpha(newData),
						DataPointUtil.getRed(newData),
						DataPointUtil.getGreen(newData),
						DataPointUtil.getBlue(newData),
						DataPointUtil.getHeight(newData) - minHeight,
						DataPointUtil.getDepth(newData) - minHeight,
						DataPointUtil.getLightSky(newData),
						DataPointUtil.getLightBlock(newData),
						DataPointUtil.getGenerationMode(newData),
						DataPointUtil.getFlag(newData));
				
				tempDataContainer[i] = newData;
			}
		}
		else if (version == 7)
		{
			for (int i = 0; i < x; i++)
			{
				newData = 0;
				for (tempIndex = 0; tempIndex < 8; tempIndex++)
					newData += (((long) inputData[index + tempIndex]) & 0xff) << (8 * tempIndex);
				index += 8;
				
				newData = DataPointUtil.createDataPoint(
						DataPointUtil.getAlpha(newData),
						DataPointUtil.getRed(newData),
						DataPointUtil.getGreen(newData),
						DataPointUtil.getBlue(newData),
						DataPointUtil.getHeight(newData) - 64 - minHeight,
						DataPointUtil.getDepth(newData) - 64 - minHeight,
						DataPointUtil.getLightSky(newData),
						DataPointUtil.getLightBlock(newData),
						DataPointUtil.getGenerationMode(newData),
						DataPointUtil.getFlag(newData));
				
				tempDataContainer[i] = newData;
			}
		}
		else //if (version == 8)
		{
			short tempMinHeight = inputData[index];
			index++;
			tempMinHeight |= (((short) inputData[index]) & 0xff) << 8;
			index++;
			if (tempMinHeight != minHeight)
			{
				for (int i = 0; i < x; i++)
				{
					newData = 0;
					for (tempIndex = 0; tempIndex < 8; tempIndex++)
						newData |= (((long) inputData[index + tempIndex]) & 0xff) << (8 * tempIndex);
					index += 8;
					newData = DataPointUtil.createDataPoint(
							DataPointUtil.getAlpha(newData),
							DataPointUtil.getRed(newData),
							DataPointUtil.getGreen(newData),
							DataPointUtil.getBlue(newData),
							DataPointUtil.getHeight(newData) + tempMinHeight - minHeight,
							DataPointUtil.getDepth(newData) + tempMinHeight - minHeight,
							DataPointUtil.getLightSky(newData),
							DataPointUtil.getLightBlock(newData),
							DataPointUtil.getGenerationMode(newData),
							DataPointUtil.getFlag(newData));
					
					tempDataContainer[i] = newData;
				}
			}
			else
			{
				for (int i = 0; i < x; i++)
				{
					newData = 0;
					for (tempIndex = 0; tempIndex < 8; tempIndex++)
						newData |= (((long) inputData[index + tempIndex]) & 0xff) << (8 * tempIndex);
					index += 8;
					tempDataContainer[i] = newData;
				}
			}
		}
		
		if (tempMaxVerticalData > DetailDistanceUtil.getMaxVerticalData(detailLevel))
		{
			int tempMaxVerticalData2 = DetailDistanceUtil.getMaxVerticalData(detailLevel);
			long[] dataToMerge = new long[tempMaxVerticalData];
			long[] tempDataContainer2 = new long[size * size * tempMaxVerticalData2];
			for (int i = 0; i < size * size; i++)
			{
				System.arraycopy(tempDataContainer, i * tempMaxVerticalData, dataToMerge, 0, tempMaxVerticalData);
				dataToMerge = DataPointUtil.mergeMultiData(dataToMerge, tempMaxVerticalData, tempMaxVerticalData2);
				System.arraycopy(dataToMerge, 0, tempDataContainer2, i * tempMaxVerticalData2, tempMaxVerticalData2);
			}
			verticalSize = tempMaxVerticalData2;
			this.dataContainer = tempDataContainer2;
		}
		else
		{
			verticalSize = tempMaxVerticalData;
			this.dataContainer = tempDataContainer;
		}
	}
	
	/**
	 * This method merge column of multiple data together
	 * @return one column of correctly parsed data
	 */
	public void mergeAndAddData(int posZ, int posX, short[] inputPositionDataToMerge, int[] inputVerticalData, int[] inputColorData, byte[] inputLightData, byte inputDetailLevel, int inputVerticalSize)
	{
		mergeAndAddData(0, inputPositionDataToMerge.length, posZ, posX, inputPositionDataToMerge, inputVerticalData, inputColorData, inputLightData, inputDetailLevel, inputVerticalSize);
	}
	
	/**
	 * This method merge column of multiple data together
	 * @return one column of correctly parsed data
	 */
	public void mergeAndAddData(int sliceStart, int sliceEnd, int posZ, int posX, short[] inputPositionData, int[] inputVerticalData, int[] inputColorData, byte[] inputLightData, byte inputDetailLevel, int inputVerticalSize)
	{
		
		
		//STEP 1//
		//We initially reset this position of the data container
		positionDataContainer[posX * size + posZ] = PositionDataFormat.EMPTY_DATA;
		for (int verticalIndex = 0; verticalIndex < verticalSize; verticalIndex++)
		{
			verticalDataContainer[posX * size * verticalSize + posZ * verticalSize + verticalIndex] = VerticalDataFormat.EMPTY_LOD;
			lightDataContainer[posX * size * verticalSize + posZ * verticalSize + verticalIndex] = 0;
			colorDataContainer[posX * size * verticalSize + posZ * verticalSize + verticalIndex] = 0;
		}
		
		
		
		//STEP 2//
		//We start by populating the PositionDataToMerge
		byte genMode = DistanceGenerationMode.FULL.complexity;
		boolean correctLight = true;
		boolean allEmpty = true;
		boolean allVoid = true;
		
		short tempPositionData;
		//we combine every position in the slice of the input
		//I THINK YOU CAN SEE HOW TO USE THE SLICE FROM HERE
		for (int positionIndex = sliceStart; positionIndex <= sliceEnd; positionIndex++)
		{
			tempPositionData = inputPositionData[positionIndex];
			genMode = (byte) Math.min(genMode, PositionDataFormat.getGenerationMode(tempPositionData));
			correctLight &= PositionDataFormat.getFlag(tempPositionData);
			allVoid &= PositionDataFormat.isVoid(tempPositionData);
			allEmpty &= PositionDataFormat.doesItExist(tempPositionData);
		}
		
		//Case 1: should never happen but we use this just in case
		//if all the data is empty (maybe a bug) then we simply return
		if (allEmpty)
		{
			return;
		}
		
		//Case 2: if all the data is void
		//if all the data is empty (maybe a bug) then we simply return
		if (allVoid)
		{
			positionDataContainer[posX * size + posZ] = PositionDataFormat.createVoidPositionData(genMode);
			return;
		}
		
		//Case 3: data is non void and non empty, we continue
		positionDataContainer[posX * size + posZ] = PositionDataFormat.createPositionData(0, correctLight, genMode);
		
		
		//STEP 3//
		//now we firstly merge the height and depth values of the input data
		//in this process we do a sort of "projection" of the data on a single column
		
		/* simple visualization of the process
		input:       ->    projection:
		| | |				|
		  |   |				|
		  |					|
		  
		  
		|     				|
		    
		    |				|
		      |      		|
		 */
		
		int inputSize = 1 << inputDetailLevel;
		// I'll disable the ThreadMap array for the initial testing //ThreadMapUtil.getHeightAndDepth(inputVerticalSize * 2 * 4)
		short[] heightAndDepth = new short[inputVerticalSize * 2 * 4];
		
		int tempVerticalData;
		short depth;
		short height;
		int count = 0;
		int i;
		int ii;
		//We collect the indexes of the data, ordered by the depth
		for (int positionIndex = sliceStart; positionIndex <= sliceEnd; positionIndex++)
		{
			tempPositionData = inputPositionData[positionIndex];
			if (!PositionDataFormat.doesItExist(tempPositionData) || PositionDataFormat.isVoid(tempPositionData))
				continue;
			for (int verticalIndex = 0; verticalIndex < inputVerticalSize; verticalIndex++)
			{
				tempVerticalData = inputVerticalData[positionIndex * inputVerticalSize + verticalIndex];
				if (VerticalDataFormat.doesItExist(tempVerticalData))
				{
					depth = VerticalDataFormat.getDepth(tempVerticalData);
					height = VerticalDataFormat.getDepth(tempVerticalData);
					
					int botPos = -1;
					int topPos = -1;
					//values fall in between and possibly require extension of array
					boolean botExtend = false;
					boolean topExtend = false;
					for (i = 0; i < count; i++)
					{
						if (depth <= heightAndDepth[i * 2] && depth >= heightAndDepth[i * 2 + 1])
						{
							botPos = i;
							break;
						}
						else if (depth < heightAndDepth[i * 2 + 1] && ((i + 1 < count && depth > heightAndDepth[(i + 1) * 2]) || i + 1 == count))
						{
							botPos = i;
							botExtend = true;
							break;
						}
					}
					for (i = 0; i < count; i++)
					{
						if (height <= heightAndDepth[i * 2] && height >= heightAndDepth[i * 2 + 1])
						{
							topPos = i;
							break;
						}
						else if (height < heightAndDepth[i * 2 + 1] && ((i + 1 < count && height > heightAndDepth[(i + 1) * 2]) || i + 1 == count))
						{
							topPos = i;
							topExtend = true;
							break;
						}
					}
					if (topPos == -1)
					{
						if (botPos == -1)
						{
							//whole block falls above
							DataMergeUtil.extendArray(heightAndDepth, 2, 0, 1, count);
							heightAndDepth[0] = height;
							heightAndDepth[1] = depth;
							count++;
						}
						else if (!botExtend)
						{
							//only top falls above extending it there, while bottom is inside existing
							DataMergeUtil.shrinkArray(heightAndDepth, 2, 0, botPos, count);
							heightAndDepth[0] = height;
							count -= botPos;
						}
						else
						{
							//top falls between some blocks, extending those as well
							DataMergeUtil.shrinkArray(heightAndDepth, 2, 0, botPos, count);
							heightAndDepth[0] = height;
							heightAndDepth[1] = depth;
							count -= botPos;
						}
					}
					else if (!topExtend)
					{
						if (!botExtend)
							//both top and bottom are within some exiting blocks, possibly merging them
							heightAndDepth[topPos * 2 + 1] = heightAndDepth[botPos * 2 + 1];
						else
							//top falls between some blocks, extending it there
							heightAndDepth[topPos * 2 + 1] = depth;
						DataMergeUtil.shrinkArray(heightAndDepth, 2, topPos + 1, botPos - topPos, count);
						count -= botPos - topPos;
					}
					else
					{
						if (!botExtend)
						{
							//only top is within some exiting block, extending it
							topPos++; //to make it easier
							heightAndDepth[topPos * 2] = height;
							heightAndDepth[topPos * 2 + 1] = heightAndDepth[botPos * 2 + 1];
							DataMergeUtil.shrinkArray(heightAndDepth, 2, topPos + 1, botPos - topPos, count);
							count -= botPos - topPos;
						}
						else
						{
							//both top and bottom are outside existing blocks
							DataMergeUtil.shrinkArray(heightAndDepth, 2, topPos + 1, botPos - topPos, count);
							count -= botPos - topPos;
							DataMergeUtil.extendArray(heightAndDepth, 2, topPos + 1, 1, count);
							count++;
							heightAndDepth[topPos * 2 + 2] = height;
							heightAndDepth[topPos * 2 + 3] = depth;
						}
					}
				}
				else
					break;
			}
		}
		
		
		
		//STEP 4//
		//we merge height and depth to respect the verticalSize of this LevelContainer
		//and we save the values directly in the VerticalDataContainer
		//In this process we can easily compute the count of this position to be inserted in the positionDataContainer
		//if the size of the array heightAndDepth is over the maxVerticalSize, then we use that
		//otherwise we use the size of the heightAndDepth
		/* simple visualization of the process
		before:			after:
			|				|
			|				|
			|				|
		 
		 
			|				|
		        we fill-->  |
			|				|
			|				|
			
		this way we reduce from verticalSize 3 to verticalSize 2
		 */
		
		//we limit the vertical portion to maxVerticalData
		int j = 0;
		while (count > verticalSize)
		{
			ii = DataPointUtil.WORLD_HEIGHT;
			for (i = 0; i < count - 1; i++)
			{
				if (heightAndDepth[i * 2 + 1] - heightAndDepth[(i + 1) * 2] <= ii)
				{
					ii = heightAndDepth[i * 2 + 1] - heightAndDepth[(i + 1) * 2];
					j = i;
				}
			}
			heightAndDepth[j * 2 + 1] = heightAndDepth[(j + 1) * 2 + 1];
			for (i = j + 1; i < count - 1; i++)
			{
				heightAndDepth[i * 2] = heightAndDepth[(i + 1) * 2];
				heightAndDepth[i * 2 + 1] = heightAndDepth[(i + 1) * 2 + 1];
			}
			//System.arraycopy(heightAndDepth, j + 1, heightAndDepth, j, count - j - 1);
			count--;
		}
		
		short lodCount = 0;
		for (j = 0; j < count; j--)
		{
			height = heightAndDepth[j * 2];
			depth = heightAndDepth[j * 2 + 1];
			
			if ((depth == 0 && height == 0) || j >= heightAndDepth.length / 2)
				break;
			
			setVerticalData(
					VerticalDataFormat.createVerticalData(height, depth, 0, false, false),
					posX,
					posZ,
					lodCount);
			lodCount++;
		}
		
		//we update the count
		setPositionData(
				PositionDataFormat.setLodCount(
						getPositionData(posX,posZ),
						lodCount),
				posX,
				posZ);
		
		//STEP 5//
		//we now get the top lods on each vertical index and we merge
		//the color, the data and ligth of all of them
		
		boolean allDefault;
		int tempColorData;
		byte tempLightData;
		int storedVerticalData;
		int newVerticalData = 0;
		int newColorData = 0;
		byte newLightData = 0;
		//As standard the vertical lods are ordered from top to bottom
		for (int verticalIndex = lodCount; verticalIndex >= 0; verticalIndex--)
		{
			storedVerticalData = getVerticalData(posX, posZ, verticalIndex);
			height = VerticalDataFormat.getHeight(storedVerticalData);
			depth = VerticalDataFormat.getDepth(storedVerticalData);
			
			if ((depth == 0 && height == 0) || verticalIndex >= heightAndDepth.length / 2)
				break;
			
			int numberOfChildren = 0;
			int tempAlpha = 0;
			int tempRed = 0;
			int tempGreen = 0;
			int tempBlue = 0;
			int tempLightBlock = 0;
			int tempLightSky = 0;
			
			for (int positionIndex = sliceStart; positionIndex <= sliceEnd; positionIndex++)
			{
				tempPositionData = inputPositionData[positionIndex];
				
				if (!PositionDataFormat.doesItExist(tempPositionData) || PositionDataFormat.isVoid(tempPositionData))
					continue;
				for (int inputVerticalIndex = 0; inputVerticalIndex < inputVerticalSize; inputVerticalIndex++)
				{
					tempVerticalData = inputVerticalData[positionIndex * inputVerticalSize + inputVerticalIndex];
					tempColorData = inputColorData[positionIndex * inputVerticalSize + inputVerticalIndex];
					tempLightData = inputLightData[positionIndex * inputVerticalSize + inputVerticalIndex];
					if (VerticalDataFormat.doesItExist(tempVerticalData))
					{
						if ((depth <= VerticalDataFormat.getDepth(tempVerticalData) && VerticalDataFormat.getDepth(tempVerticalData) <= height)
									|| (depth <= VerticalDataFormat.getHeight(tempVerticalData) && VerticalDataFormat.getHeight(tempVerticalData) <= height))
						{
							if (VerticalDataFormat.getHeight(tempVerticalData) > VerticalDataFormat.getHeight(newVerticalData))
							{
								newVerticalData = tempColorData;
								newColorData = tempVerticalData;
								newLightData = tempLightData;
							}
						}
					}
					else
						break;
				}
				
				numberOfChildren++;
				tempAlpha += ColorFormat.getAlpha(newColorData);
				tempRed += ColorFormat.getRed(newColorData);
				tempGreen += ColorFormat.getGreen(newColorData);
				tempBlue += ColorFormat.getBlue(newColorData);
				tempLightBlock += LightFormat.getBlockLight(newLightData);
				tempLightSky += LightFormat.getSkyLight(newLightData);
			}
			
			//we have at least 1 child
			tempAlpha = tempAlpha / numberOfChildren;
			tempRed = tempRed / numberOfChildren;
			tempGreen = tempGreen / numberOfChildren;
			tempBlue = tempBlue / numberOfChildren;
			tempLightBlock = tempLightBlock / numberOfChildren;
			tempLightSky = tempLightSky / numberOfChildren;
			setColorData(ColorFormat.createColorData(tempAlpha, tempRed, tempGreen, tempBlue), posX, posZ, verticalIndex);
			setLightData(LightFormat.formatLightAsByte((byte) tempLightSky, (byte) tempLightBlock), posX, posZ, verticalIndex);
		}
	}
	
	//SPLITTED VERSION OF THE MERGE AND ADD
	private void resetPosition(int posX, int posZ)
	{
		positionDataContainer[posX * size + posZ] = PositionDataFormat.EMPTY_DATA;
		for (int verticalIndex = 0; verticalIndex < verticalSize; verticalIndex++)
		{
			verticalDataContainer[posX * size * verticalSize + posZ * verticalSize + verticalIndex] = VerticalDataFormat.EMPTY_LOD;
			lightDataContainer[posX * size * verticalSize + posZ * verticalSize + verticalIndex] = 0;
			colorDataContainer[posX * size * verticalSize + posZ * verticalSize + verticalIndex] = 0;
		}
	}
	
	private boolean mergeAndAddPositionData(int sliceStart, int sliceEnd, int posZ, int posX, short[] inputPositionData)
	{
		
		//We start by populating the PositionDataToMerge
		byte genMode = DistanceGenerationMode.FULL.complexity;
		boolean correctLight = true;
		boolean allEmpty = true;
		boolean allVoid = true;
		
		short tempPositionData;
		//we combine every position in the slice of the input
		//I THINK YOU CAN SEE HOW TO USE THE SLICE FROM HERE
		for (int positionIndex = sliceStart; positionIndex <= sliceEnd; positionIndex++)
		{
			tempPositionData = inputPositionData[positionIndex];
			genMode = (byte) Math.min(genMode, PositionDataFormat.getGenerationMode(tempPositionData));
			correctLight &= PositionDataFormat.getFlag(tempPositionData);
			allVoid &= PositionDataFormat.isVoid(tempPositionData);
			allEmpty &= PositionDataFormat.doesItExist(tempPositionData);
		}
		
		//Case 1: should never happen but we use this just in case
		//if all the data is empty (maybe a bug) then we simply return
		if (allEmpty)
		{
			return false;
		}
		
		//Case 2: if all the data is void
		//if all the data is empty (maybe a bug) then we simply return
		if (allVoid)
		{
			positionDataContainer[posX * size + posZ] = PositionDataFormat.createVoidPositionData(genMode);
			return false;
		}
		
		//Case 3: data is non void and non empty, we continue
		positionDataContainer[posX * size + posZ] = PositionDataFormat.createPositionData(0, correctLight, genMode);
		return true;
	}
	
	private void mergeAndAddVerticalData(int sliceStart, int sliceEnd, int posZ, int posX, short[] inputPositionData, int[] inputVerticalData, int[] inputColorData, byte[] inputLightData, byte inputDetailLevel, int inputVerticalSize)
	{
		//STEP 3//
		//now we firstly merge the height and depth values of the input data
		//in this process we do a sort of "projection" of the data on a single column
		
		/* simple visualization of the process
		input:       ->    projection:
		| | |				|
		  |   |				|
		  |					|
		  
		  
		|     				|
		    
		    |				|
		      |      		|
		 */
		
		int inputSize = 1 << inputDetailLevel;
		// I'll disable the ThreadMap array for the initial testing //ThreadMapUtil.getHeightAndDepth(inputVerticalSize * 2 * 4)
		short[] heightAndDepth = new short[inputVerticalSize * 2 * 4];
		short tempPositionData;
		int tempVerticalData;
		short depth;
		short height;
		int count = 0;
		int i;
		int ii;
		//We collect the indexes of the data, ordered by the depth
		for (int positionIndex = sliceStart; positionIndex < sliceEnd; positionIndex++)
		{
			tempPositionData = inputPositionData[positionIndex];
			if (!PositionDataFormat.doesItExist(tempPositionData) || PositionDataFormat.isVoid(tempPositionData))
				continue;
			for (int verticalIndex = 0; verticalIndex < inputVerticalSize; verticalIndex++)
			{
				tempVerticalData = inputVerticalData[positionIndex * inputVerticalSize + verticalIndex];
				if (VerticalDataFormat.doesItExist(tempVerticalData))
				{
					depth = VerticalDataFormat.getDepth(tempVerticalData);
					height = VerticalDataFormat.getDepth(tempVerticalData);
					
					int botPos = -1;
					int topPos = -1;
					//values fall in between and possibly require extension of array
					boolean botExtend = false;
					boolean topExtend = false;
					for (i = 0; i < count; i++)
					{
						if (depth <= heightAndDepth[i * 2] && depth >= heightAndDepth[i * 2 + 1])
						{
							botPos = i;
							break;
						}
						else if (depth < heightAndDepth[i * 2 + 1] && ((i + 1 < count && depth > heightAndDepth[(i + 1) * 2]) || i + 1 == count))
						{
							botPos = i;
							botExtend = true;
							break;
						}
					}
					for (i = 0; i < count; i++)
					{
						if (height <= heightAndDepth[i * 2] && height >= heightAndDepth[i * 2 + 1])
						{
							topPos = i;
							break;
						}
						else if (height < heightAndDepth[i * 2 + 1] && ((i + 1 < count && height > heightAndDepth[(i + 1) * 2]) || i + 1 == count))
						{
							topPos = i;
							topExtend = true;
							break;
						}
					}
					if (topPos == -1)
					{
						if (botPos == -1)
						{
							//whole block falls above
							DataMergeUtil.extendArray(heightAndDepth, 2, 0, 1, count);
							heightAndDepth[0] = height;
							heightAndDepth[1] = depth;
							count++;
						}
						else if (!botExtend)
						{
							//only top falls above extending it there, while bottom is inside existing
							DataMergeUtil.shrinkArray(heightAndDepth, 2, 0, botPos, count);
							heightAndDepth[0] = height;
							count -= botPos;
						}
						else
						{
							//top falls between some blocks, extending those as well
							DataMergeUtil.shrinkArray(heightAndDepth, 2, 0, botPos, count);
							heightAndDepth[0] = height;
							heightAndDepth[1] = depth;
							count -= botPos;
						}
					}
					else if (!topExtend)
					{
						if (!botExtend)
							//both top and bottom are within some exiting blocks, possibly merging them
							heightAndDepth[topPos * 2 + 1] = heightAndDepth[botPos * 2 + 1];
						else
							//top falls between some blocks, extending it there
							heightAndDepth[topPos * 2 + 1] = depth;
						DataMergeUtil.shrinkArray(heightAndDepth, 2, topPos + 1, botPos - topPos, count);
						count -= botPos - topPos;
					}
					else
					{
						if (!botExtend)
						{
							//only top is within some exiting block, extending it
							topPos++; //to make it easier
							heightAndDepth[topPos * 2] = height;
							heightAndDepth[topPos * 2 + 1] = heightAndDepth[botPos * 2 + 1];
							DataMergeUtil.shrinkArray(heightAndDepth, 2, topPos + 1, botPos - topPos, count);
							count -= botPos - topPos;
						}
						else
						{
							//both top and bottom are outside existing blocks
							DataMergeUtil.shrinkArray(heightAndDepth, 2, topPos + 1, botPos - topPos, count);
							count -= botPos - topPos;
							DataMergeUtil.extendArray(heightAndDepth, 2, topPos + 1, 1, count);
							count++;
							heightAndDepth[topPos * 2 + 2] = height;
							heightAndDepth[topPos * 2 + 3] = depth;
						}
					}
				}
				else
					break;
			}
		}
		
		
		
		//STEP 4//
		//we merge height and depth to respect the verticalSize of this LevelContainer
		//and we save the values directly in the VerticalDataContainer
		//In this process we can easily compute the count of this position to be inserted in the positionDataContainer
		//if the size of the array heightAndDepth is over the maxVerticalSize, then we use that
		//otherwise we use the size of the heightAndDepth
		/* simple visualization of the process
		before:			after:
			|				|
			|				|
			|				|
		 
		 
			|				|
		        we fill-->  |
			|				|
			|				|
			
		this way we reduce from verticalSize 3 to verticalSize 2
		 */
		
		//we limit the vertical portion to maxVerticalData
		int j = 0;
		while (count > verticalSize)
		{
			ii = DataPointUtil.WORLD_HEIGHT;
			for (i = 0; i < count - 1; i++)
			{
				if (heightAndDepth[i * 2 + 1] - heightAndDepth[(i + 1) * 2] <= ii)
				{
					ii = heightAndDepth[i * 2 + 1] - heightAndDepth[(i + 1) * 2];
					j = i;
				}
			}
			heightAndDepth[j * 2 + 1] = heightAndDepth[(j + 1) * 2 + 1];
			for (i = j + 1; i < count - 1; i++)
			{
				heightAndDepth[i * 2] = heightAndDepth[(i + 1) * 2];
				heightAndDepth[i * 2 + 1] = heightAndDepth[(i + 1) * 2 + 1];
			}
			//System.arraycopy(heightAndDepth, j + 1, heightAndDepth, j, count - j - 1);
			count--;
		}
		
		short lodCount = 0;
		for (j = 0; j < count; j--)
		{
			height = heightAndDepth[j * 2];
			depth = heightAndDepth[j * 2 + 1];
			
			if ((depth == 0 && height == 0) || j >= heightAndDepth.length / 2)
				break;
			
			setVerticalData(
					VerticalDataFormat.createVerticalData(height, depth, 0, false, false),
					posX,
					posZ,
					lodCount);
			lodCount++;
		}
		
		//we update the count
		setPositionData(
				PositionDataFormat.setLodCount(
						getPositionData(posX,posZ),
						lodCount),
				posX,
				posZ);
	}
	
	
	private void mergeAndAddColorLightData(int sliceStart, int sliceEnd, int posZ, int posX, short[] inputPositionData, int[] inputVerticalData, int[] inputColorData, byte[] inputLightData, byte inputDetailLevel, int inputVerticalSize)
	{
		//STEP 5//
		//we now get the top lods on each vertical index and we merge
		//the color, the data and ligth of all of them
		
		short tempPositionData;
		int tempVerticalData;
		int tempColorData;
		byte tempLightData;
		
		int storedVerticalData;
		
		int newVerticalData = 0;
		int newColorData = 0;
		byte newLightData = 0;
		
		int height;
		int depth;
		
		short lodCount = PositionDataFormat.getLodCount(getPositionData(posX,posZ));
		
		//As standard the vertical lods are ordered from top to bottom
		for (int verticalIndex = lodCount; verticalIndex >= 0; verticalIndex--)
		{
			storedVerticalData = getVerticalData(posX, posZ, verticalIndex);
			height = VerticalDataFormat.getHeight(storedVerticalData);
			depth = VerticalDataFormat.getDepth(storedVerticalData);
			
			int numberOfChildren = 0;
			int tempAlpha = 0;
			int tempRed = 0;
			int tempGreen = 0;
			int tempBlue = 0;
			int tempLightBlock = 0;
			int tempLightSky = 0;
			
			for (int positionIndex = sliceStart; positionIndex <= sliceEnd; positionIndex++)
			{
				tempPositionData = inputPositionData[positionIndex];
				
				if (!PositionDataFormat.doesItExist(tempPositionData) || PositionDataFormat.isVoid(tempPositionData))
					continue;
				for (int inputVerticalIndex = 0; inputVerticalIndex < inputVerticalSize; inputVerticalIndex++)
				{
					tempVerticalData = inputVerticalData[positionIndex * inputVerticalSize + inputVerticalIndex];
					tempColorData = inputColorData[positionIndex * inputVerticalSize + inputVerticalIndex];
					tempLightData = inputLightData[positionIndex * inputVerticalSize + inputVerticalIndex];
					if (VerticalDataFormat.doesItExist(tempVerticalData))
					{
						if ((depth <= VerticalDataFormat.getDepth(tempVerticalData) && VerticalDataFormat.getDepth(tempVerticalData) <= height)
									|| (depth <= VerticalDataFormat.getHeight(tempVerticalData) && VerticalDataFormat.getHeight(tempVerticalData) <= height))
						{
							if (VerticalDataFormat.getHeight(tempVerticalData) > VerticalDataFormat.getHeight(newVerticalData))
							{
								newVerticalData = tempColorData;
								newColorData = tempVerticalData;
								newLightData = tempLightData;
							}
						}
					}
					else
						break;
				}
				
				numberOfChildren++;
				tempAlpha += ColorFormat.getAlpha(newColorData);
				tempRed += ColorFormat.getRed(newColorData);
				tempGreen += ColorFormat.getGreen(newColorData);
				tempBlue += ColorFormat.getBlue(newColorData);
				tempLightBlock += LightFormat.getBlockLight(newLightData);
				tempLightSky += LightFormat.getSkyLight(newLightData);
			}
			
			//we have at least 1 child
			tempAlpha = tempAlpha / numberOfChildren;
			tempRed = tempRed / numberOfChildren;
			tempGreen = tempGreen / numberOfChildren;
			tempBlue = tempBlue / numberOfChildren;
			tempLightBlock = tempLightBlock / numberOfChildren;
			tempLightSky = tempLightSky / numberOfChildren;
			setColorData(ColorFormat.createColorData(tempAlpha, tempRed, tempGreen, tempBlue), posX, posZ, verticalIndex);
			setLightData(LightFormat.formatLightAsByte((byte) tempLightSky, (byte) tempLightBlock), posX, posZ, verticalIndex);
		}
	}
	
	@Override
	public LevelContainer expand()
	{
		return new VerticalLevelContainer((byte) (getDetailLevel() - 1));
	}
	
	@Override
	public void updateData(LevelContainer lowerLevelContainer, int posX, int posZ)
	{
		//We reset the array
		long[] dataToMerge = ThreadMapUtil.getVerticalUpdateArray(detailLevel);
		
		int lowerMaxVertical = dataToMerge.length / 4;
		int childPosX;
		int childPosZ;
		long[] data;
		for (int x = 0; x <= 1; x++)
		{
			for (int z = 0; z <= 1; z++)
			{
				childPosX = 2 * posX + x;
				childPosZ = 2 * posZ + z;
				
				for (int verticalIndex = 0; verticalIndex < lowerMaxVertical; verticalIndex++)
					dataToMerge[(z * 2 + x) * lowerMaxVertical + verticalIndex] = lowerLevelContainer.getData(childPosX, childPosZ, verticalIndex);
			}
		}
		data = DataPointUtil.mergeMultiData(dataToMerge, lowerMaxVertical, getVerticalSize());
		
		addVerticalData(data, posX, posZ);
	}
	
	public void newUpdateData(VerticalLevelContainer lowerLevelContainer, int posX, int posZ)
	{
		//We reset the array
		int lowerVerticalSize = lowerLevelContainer.getVerticalSize();
		short[] positionDataToMerge = new short[4];
		int[] verticalDataToMerge = new int[4 * lowerVerticalSize];
		int[] colorDataToMerge = new int[4 * lowerVerticalSize];
		byte[] ligthDataToMerge = new byte[4 * lowerVerticalSize];
		
		int childPosX;
		int childPosZ;
		for (int x = 0; x <= 1; x++)
		{
			for (int z = 0; z <= 1; z++)
			{
				childPosX = 2 * posX + x;
				childPosZ = 2 * posZ + z;
				positionDataToMerge[z * 2 + x] = lowerLevelContainer.getPositionData(childPosX, childPosZ);
				for (int verticalIndex = 0; verticalIndex < lowerVerticalSize; verticalIndex++)
				{
					final int i = (z * 2 + x) * lowerVerticalSize + verticalIndex;
					verticalDataToMerge[i] = lowerLevelContainer.getVerticalData(childPosX, childPosZ, verticalIndex);
					colorDataToMerge[i] = lowerLevelContainer.getColorData(childPosX, childPosZ, verticalIndex);
					ligthDataToMerge[i] = lowerLevelContainer.getLightData(childPosX, childPosZ, verticalIndex);
				}
			}
		}
		mergeAndAddData(posX,posZ, positionDataToMerge, verticalDataToMerge, colorDataToMerge, ligthDataToMerge, lowerLevelContainer.detailLevel, lowerVerticalSize);
	}
	
	@Override
	public byte[] toDataString()
	{
		int index = 0;
		int x = size * size;
		int tempIndex;
		long current;
		boolean allGenerated = true;
		byte[] tempData = ThreadMapUtil.getSaveContainer(detailLevel);
		
		tempData[index] = detailLevel;
		index++;
		tempData[index] = (byte) verticalSize;
		index++;
		tempData[index] = (byte) (minHeight & 0xFF);
		index++;
		tempData[index] = (byte) ((minHeight >> 8) & 0xFF);
		index++;
		
		int j;
		for (int i = 0; i < x; i++)
		{
			for (j = 0; j < verticalSize; j++)
			{
				current = dataContainer[i * verticalSize + j];
				for (tempIndex = 0; tempIndex < 8; tempIndex++)
					tempData[index + tempIndex] = (byte) (current >>> (8 * tempIndex));
				index += 8;
			}
			if (!DataPointUtil.doesItExist(dataContainer[i]))
				allGenerated = false;
		}
		if (allGenerated)
			tempData[1] |= 0b10000000;
		return tempData;
	}
	
	@Override
	@SuppressWarnings("unused")
	public String toString()
	{
		/*
		StringBuilder stringBuilder = new StringBuilder();
		int size = 1 << (LodUtil.REGION_DETAIL_LEVEL - detailLevel);
		stringBuilder.append(detailLevel);
		stringBuilder.append(DATA_DELIMITER);
		for (int x = 0; x < size; x++)
		{
			for (int z = 0; z < size; z++)
			{
				//Converting the dataToHex
				stringBuilder.append(Long.toHexString(dataContainer[x][z][0]));
				stringBuilder.append(DATA_DELIMITER);
			}
		}
		return stringBuilder.toString();
		 */
		return " ";
	}
	
	@Override
	public int getMaxNumberOfLods()
	{
		return size * size * getVerticalSize();
	}
	
	
}
