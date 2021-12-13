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

import com.seibel.lod.core.dataFormat.PositionDataFormat;
import com.seibel.lod.core.dataFormat.VerticalDataFormat;
import com.seibel.lod.core.enums.config.DistanceGenerationMode;
import com.seibel.lod.core.util.*;
import com.seibel.lod.core.wrapperInterfaces.IVersionConstants;

/**
 * 
 * @author Leonardo Amato
 * @version ??
 */
public class VerticalLevelContainer implements LevelContainer
{
	
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
		final IVersionConstants VERSION_CONSTANTS = SingletonHandler.get(IVersionConstants.class);
		short minHeight = (short) VERSION_CONSTANTS.getMinimumWorldHeight();
		
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
			tempMinHeight |= inputData[index] << 8;
			index++;
			if (tempMinHeight != minHeight)
			{
				for (int i = 0; i < x; i++)
				{
					newData = 0;
					for (tempIndex = 0; tempIndex < 8; tempIndex++)
						newData |= ((long) inputData[index + tempIndex]) << (8 * tempIndex);
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
						newData |= ((long) inputData[index + tempIndex]) << (8 * tempIndex);
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
	public void mergeAndAddData(int posZ, int posX, short[] inputPositionDataToMerge, int[] inputVerticalData, int[] inputColorData, int[] inputLightData, byte inputDetailLevel, int inputVerticalSize)
	{
		
		
		//STEP 1//
		//We initially reset this position of the data container
		positionDataContainer[posX * size + posZ] = PositionDataFormat.EMPTY_DATA;
		for(int verticalIndex = 0; verticalIndex < verticalSize; verticalIndex++)
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
		
		//we
		for(short data : inputPositionDataToMerge)
		{
			genMode = (byte) Math.min(genMode, PositionDataFormat.getGenerationMode(data));
			correctLight &= PositionDataFormat.getFlag(data);
			allVoid &= PositionDataFormat.isVoid(data);
			allEmpty &= PositionDataFormat.doesItExist(data);
		}
		
		//Case 1: should never happen but we use this just in case
		//if all the data is empty (maybe a bug) then we simply return
		if(allEmpty)
		{
			return;
		}
		
		//Case 2: if all the data is void
		//if all the data is empty (maybe a bug) then we simply return
		if(allVoid)
		{
			positionDataContainer[posX * size + posZ] = PositionDataFormat.createVoidPositionData(genMode);
			return;
		}
		
		positionDataContainer[posX * size + posZ] = PositionDataFormat.createPositionData(0, correctLight, genMode);
		
		
		//STEP 3//
		//now we firstly merge the height and depth values of the input data
		
		int inputSize = 1 << inputDetailLevel;
		// I'll disable the ThreadMap array for the initial testing ThreadMapUtil.getHeightAndDepth(inputVerticalSize * 2 * 4)
		short[] heightAndDepth = new short[inputVerticalSize * 2 * 4];
		
		
		
		//STEP 4//
		//we merge height and depth to respect the verticalSize of this LevelContainer
		//and we save the values directly in the VerticalDataContainer
		//In this process we can easily compute the count of this position to be inserted in the positionDataContainer
		//if the size of the array heightAndDepth is over the maxVerticalSize, then we use that
		//otherwise we use the size of the heightAndDepth
		
		
		
		//STEP 5//
		//we now get the top lods on each vertical index and we merge
		//the color, the data
		
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
	
	@Override
	public byte[] toDataString()
	{
		int index = 0;
		int x = size * size;
		int tempIndex;
		long current;
		boolean allGenerated = true;
		byte[] tempData = ThreadMapUtil.getSaveContainer(detailLevel);
		final IVersionConstants VERSION_CONSTANTS = SingletonHandler.get(IVersionConstants.class);
		short minHeight = (short) VERSION_CONSTANTS.getMinimumWorldHeight();
		
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
			if(!DataPointUtil.doesItExist(dataContainer[i]))
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
