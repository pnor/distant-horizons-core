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

import com.seibel.lod.core.enums.config.DistanceGenerationMode;
import com.seibel.lod.core.util.DataPointUtil;
import com.seibel.lod.core.util.DetailDistanceUtil;
import com.seibel.lod.core.util.LevelPosUtil;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.util.ThreadMapUtil;

/**
 * 
 * @author Leonardo Amato
 * @version ??
 */
public class VerticalLevelContainer implements LevelContainer
{
	
	public final byte detailLevel;
	public final int size;
	public final int maxVerticalData;
	
	public final long[] dataContainer;
	
	public VerticalLevelContainer(byte detailLevel)
	{
		this.detailLevel = detailLevel;
		size = 1 << (LodUtil.REGION_DETAIL_LEVEL - detailLevel);
		maxVerticalData = DetailDistanceUtil.getMaxVerticalData(detailLevel);
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
		for (int verticalIndex = 0; verticalIndex < maxVerticalData; verticalIndex++)
			dataContainer[posX * size * maxVerticalData + posZ * maxVerticalData + verticalIndex] = DataPointUtil.EMPTY_DATA;
	}
	
	@Override
	public boolean addData(long data, int posX, int posZ, int verticalIndex)
	{
		dataContainer[posX * size * maxVerticalData + posZ * maxVerticalData + verticalIndex] = data;
		return true;
	}
	
	@Override
	public boolean addVerticalData(long[] data, int posX, int posZ)
	{
		for (int verticalIndex = 0; verticalIndex < maxVerticalData; verticalIndex++)
			dataContainer[posX * size * maxVerticalData + posZ * maxVerticalData + verticalIndex] = data[verticalIndex];
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
		return dataContainer[posX * size * maxVerticalData + posZ * maxVerticalData + verticalIndex];
	}
	
	@Override
	public long getSingleData(int posX, int posZ)
	{
		return dataContainer[posX * size * maxVerticalData + posZ * maxVerticalData];
	}
	
	@Override
	public int getMaxVerticalData()
	{
		return maxVerticalData;
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
						DataPointUtil.getHeight(newData) - DataPointUtil.VERTICAL_OFFSET,
						DataPointUtil.getDepth(newData) - DataPointUtil.VERTICAL_OFFSET,
						DataPointUtil.getLightSky(newData),
						DataPointUtil.getLightBlock(newData),
						DataPointUtil.getGenerationMode(newData),
						DataPointUtil.getFlag(newData)
				);
				tempDataContainer[i] = newData;
			}
		}
		else //if (version == 7)
		{
			for (int i = 0; i < x; i++)
			{
				newData = 0;
				for (tempIndex = 0; tempIndex < 8; tempIndex++)
					newData += (((long) inputData[index + tempIndex]) & 0xff) << (8 * tempIndex);
				index += 8;
				tempDataContainer[i] = newData;
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
			maxVerticalData = tempMaxVerticalData2;
			this.dataContainer = tempDataContainer2;
		}
		else
		{
			maxVerticalData = tempMaxVerticalData;
			this.dataContainer = tempDataContainer;
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
		mergeMultiData(posX, posZ, dataToMerge, lowerMaxVertical, getMaxVerticalData());
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
		tempData[index] = (byte) maxVerticalData;
		index++;
		int j;
		for (int i = 0; i < x; i++)
		{
			for (j = 0; j < maxVerticalData; j++)
			{
				current = dataContainer[i * maxVerticalData + j];
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
	
	public static void shrinkArray(short[] array, int packetSize, int start, int length, int arraySize)
	{
		start *= packetSize;
		length *= packetSize;
		arraySize *= packetSize;
		for (int i = 0; i < arraySize - start; i++)
		{
			array[start + i] = array[start + length + i];
			//remove comment to not leave garbage at the end
			//array[start + packetSize + i] = 0;
		}
	}
	
	public static void extendArray(short[] array, int packetSize, int start, int length, int arraySize)
	{
		start *= packetSize;
		length *= packetSize;
		arraySize *= packetSize;
		for (int i = arraySize - start - 1; i >= 0; i--)
		{
			array[start + length + i] = array[start + i];
			array[start + i] = 0;
		}
	}
	
	/**
	 * This method merge column of multiple data together
	 * @param dataToMerge one or more columns of data
	 * @param inputVerticalData vertical size of an input data
	 * @param maxVerticalData max vertical size of the merged data
	 */
	public void mergeMultiData(int posX, int posZ, long[] dataToMerge, int inputVerticalData, int maxVerticalData)
	{
		int size = dataToMerge.length / inputVerticalData;
		
		// We initialize the arrays that are going to be used
		short[] heightAndDepth = ThreadMapUtil.getHeightAndDepth((DataPointUtil.WORLD_HEIGHT / 2 + 1) * 2);
		long[] dataPoint = ThreadMapUtil.getVerticalDataArray(DetailDistanceUtil.getMaxVerticalData(0));
		
		
		int genMode = DistanceGenerationMode.FULL.complexity;
		boolean allEmpty = true;
		boolean allVoid = true;
		boolean allDefault;
		long singleData;
		
		
		short depth;
		short height;
		int count = 0;
		int i;
		int ii;
		int dataIndex;
		//We collect the indexes of the data, ordered by the depth
		for (int index = 0; index < size; index++)
		{
			for (dataIndex = 0; dataIndex < inputVerticalData; dataIndex++)
			{
				singleData = dataToMerge[index * inputVerticalData + dataIndex];
				if (DataPointUtil.doesItExist(singleData))
				{
					genMode = Math.min(genMode, DataPointUtil.getGenerationMode(singleData));
					allEmpty = false;
					if (!DataPointUtil.isVoid(singleData))
					{
						allVoid = false;
						depth = DataPointUtil.getDepth(singleData);
						height = DataPointUtil.getHeight(singleData);
						
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
								extendArray(heightAndDepth, 2, 0, 1, count);
								heightAndDepth[0] = height;
								heightAndDepth[1] = depth;
								count++;
							}
							else if (!botExtend)
							{
								//only top falls above extending it there, while bottom is inside existing
								shrinkArray(heightAndDepth, 2, 0, botPos, count);
								heightAndDepth[0] = height;
								count -= botPos;
							}
							else
							{
								//top falls between some blocks, extending those as well
								shrinkArray(heightAndDepth, 2, 0, botPos, count);
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
							shrinkArray(heightAndDepth, 2, topPos + 1, botPos - topPos, count);
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
								shrinkArray(heightAndDepth, 2, topPos + 1, botPos - topPos, count);
								count -= botPos - topPos;
							}
							else
							{
								//both top and bottom are outside existing blocks
								shrinkArray(heightAndDepth, 2, topPos + 1, botPos - topPos, count);
								count -= botPos - topPos;
								extendArray(heightAndDepth, 2, topPos + 1, 1, count);
								count++;
								heightAndDepth[topPos * 2 + 2] = height;
								heightAndDepth[topPos * 2 + 3] = depth;
							}
						}
					}
				}
				else
					break;
			}
		}
		
		//We check if there is any data that's not empty or void
		if (allEmpty)
			return;
		if (allVoid)
		{
			dataPoint[0] = DataPointUtil.createVoidDataPoint(genMode);
			return;
		}
		
		//we limit the vertical portion to maxVerticalData
		int j = 0;
		while (count > maxVerticalData)
		{
			ii = DataPointUtil.WORLD_HEIGHT - DataPointUtil.VERTICAL_OFFSET;
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
		//As standard the vertical lods are ordered from top to bottom
		for (j = count - 1; j >= 0; j--)
		{
			final int arrayPos = posX * size * maxVerticalData + posZ * maxVerticalData + j;
			height = heightAndDepth[j * 2];
			depth = heightAndDepth[j * 2 + 1];
			
			if ((depth == 0 && height == 0) || j >= heightAndDepth.length / 2)
				break;
			
			int numberOfChildren = 0;
			int tempAlpha = 0;
			int tempRed = 0;
			int tempGreen = 0;
			int tempBlue = 0;
			int tempLightBlock = 0;
			int tempLightSky = 0;
			byte tempGenMode = DistanceGenerationMode.FULL.complexity;
			allEmpty = true;
			allVoid = true;
			allDefault = true;
			long data = 0;
			
			for (int index = 0; index < size; index++)
			{
				for (dataIndex = 0; dataIndex < inputVerticalData; dataIndex++)
				{
					singleData = dataToMerge[index * inputVerticalData + dataIndex];
					if (DataPointUtil.doesItExist(singleData) && !DataPointUtil.isVoid(singleData))
					{
						
						if ((depth <= DataPointUtil.getDepth(singleData) && DataPointUtil.getDepth(singleData) <= height)
								|| (depth <= DataPointUtil.getHeight(singleData) && DataPointUtil.getHeight(singleData) <= height))
						{
							if (DataPointUtil.getHeight(singleData) > DataPointUtil.getHeight(data))
								data = singleData;
						}
					}
					else
						break;
				}
				if (!DataPointUtil.doesItExist(data))
				{
					singleData = dataToMerge[index * inputVerticalData];
					data = DataPointUtil.createVoidDataPoint(DataPointUtil.getGenerationMode(singleData));
				}
				
				if (DataPointUtil.doesItExist(data))
				{
					allEmpty = false;
					if (!DataPointUtil.isVoid(data))
					{
						numberOfChildren++;
						allVoid = false;
						tempAlpha += DataPointUtil.getAlpha(data);
						tempRed += DataPointUtil.getRed(data);
						tempGreen += DataPointUtil.getGreen(data);
						tempBlue += DataPointUtil.getBlue(data);
						tempLightBlock += DataPointUtil.getLightBlock(data);
						tempLightSky += DataPointUtil.getLightSky(data);
						if (!DataPointUtil.getFlag(data)) allDefault = false;
					}
					tempGenMode = (byte) Math.min(tempGenMode, DataPointUtil.getGenerationMode(data));
				}
				else
					tempGenMode = (byte) Math.min(tempGenMode, DistanceGenerationMode.NONE.complexity);
			}
			
			if (allEmpty)
				//no child has been initialized
				dataContainer[arrayPos] = DataPointUtil.EMPTY_DATA;
			else if (allVoid)
				//all the children are void
				dataContainer[arrayPos] = DataPointUtil.createVoidDataPoint(tempGenMode);
			else
			{
				//we have at least 1 child
				tempAlpha = tempAlpha / numberOfChildren;
				tempRed = tempRed / numberOfChildren;
				tempGreen = tempGreen / numberOfChildren;
				tempBlue = tempBlue / numberOfChildren;
				tempLightBlock = tempLightBlock / numberOfChildren;
				tempLightSky = tempLightSky / numberOfChildren;
				dataContainer[arrayPos] = DataPointUtil.createDataPoint(tempAlpha, tempRed, tempGreen, tempBlue, height, depth, tempLightSky, tempLightBlock, tempGenMode, allDefault);
			}
		}
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
		return size * size * getMaxVerticalData();
	}
}
