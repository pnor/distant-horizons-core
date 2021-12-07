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
	
	public final int[] dataContainerColor;
	public final int[] dataContainerData;
	public final byte[] dataContainerFlags;
	
	public VerticalLevelContainer(byte detailLevel)
	{
		this.detailLevel = detailLevel;
		size = 1 << (LodUtil.REGION_DETAIL_LEVEL - detailLevel);
		maxVerticalData = DetailDistanceUtil.getMaxVerticalData(detailLevel);
		final int i = size * size * maxVerticalData;
		dataContainerColor = new int[i];
		dataContainerData = new int[i];
		dataContainerFlags = new byte[i];
	}
	
	@Override
	public byte getDetailLevel()
	{
		return detailLevel;
	}
	
	@Override
	public void clear(int posX, int posZ)
	{
		posX = LevelPosUtil.getRegionModule(detailLevel, posX);
		posZ = LevelPosUtil.getRegionModule(detailLevel, posZ);
		for (int verticalIndex = 0; verticalIndex < maxVerticalData; verticalIndex++)
		{
			final int i = (posX * size + posZ) * maxVerticalData + verticalIndex;
			dataContainerColor[i] = 0;
			dataContainerData[i] = 0;
			dataContainerFlags[i] = 0;
		}
	}
	
	@Override
	public boolean addData(int color, int data, byte flags, int posX, int posZ, int verticalIndex)
	{
		posX = LevelPosUtil.getRegionModule(detailLevel, posX);
		posZ = LevelPosUtil.getRegionModule(detailLevel, posZ);
		final int i = (posX * size + posZ) * maxVerticalData + verticalIndex;
		dataContainerColor[i] = color;
		dataContainerData[i] = data;
		dataContainerFlags[i] = flags;
		return true;
	}
	
	@Override
	public boolean addVerticalData(int[] color, int[] data, byte[] flags, int posX, int posZ)
	{
		posX = LevelPosUtil.getRegionModule(detailLevel, posX);
		posZ = LevelPosUtil.getRegionModule(detailLevel, posZ);
		for (int verticalIndex = 0; verticalIndex < maxVerticalData; verticalIndex++)
		{
			final int i = (posX * size + posZ) * maxVerticalData + verticalIndex;
			dataContainerColor[i] = color[verticalIndex];
			dataContainerData[i] = data[verticalIndex];
			dataContainerFlags[i] = flags[verticalIndex];
		}
		return true;
	}
	
	@Override
	public boolean addSingleData(int color, int data, byte flags, int posX, int posZ)
	{
		return addData(color, data, flags, posX, posZ, 0);
	}
	
	@Override
	public int getColor(int posX, int posZ, int verticalIndex)
	{
		return dataContainerColor[(posX * size + posZ) * maxVerticalData + verticalIndex];
	}
	
	
	@Override
	public int getData(int posX, int posZ, int verticalIndex)
	{
		return dataContainerData[(posX * size + posZ) * maxVerticalData + verticalIndex];
	}
	
	@Override
	public byte getFlags(int posX, int posZ, int verticalIndex)
	{
		return dataContainerFlags[(posX * size + posZ) * maxVerticalData + verticalIndex];
	}
	
	@Override
	public byte getSingleFlags(int posX, int posZ)
	{
		posX = LevelPosUtil.getRegionModule(detailLevel, posX);
		posZ = LevelPosUtil.getRegionModule(detailLevel, posZ);
		return dataContainerFlags[(posX * size + posZ) * maxVerticalData];
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
		posX = LevelPosUtil.getRegionModule(detailLevel, posX);
		posZ = LevelPosUtil.getRegionModule(detailLevel, posZ);
		return DataPointUtil.doesItExist(dataContainerFlags[(posX * size + posZ) * maxVerticalData]);
	}
	
	public VerticalLevelContainer(byte[] inputData, int version)
	{
		int tempMaxVerticalData;
		int tempIndex;
		int index = 0;
		detailLevel = inputData[index];
		index++;
		tempMaxVerticalData = inputData[index] & 0b01111111;
		index++;
		size = 1 << (LodUtil.REGION_DETAIL_LEVEL - detailLevel);
		int x = size * size * tempMaxVerticalData;
		int[] tempDataContainerColor = new int[x];
		int[] tempDataContainerData = new int[x];
		byte[] tempDataContainerFlags = new byte[x];
		
		if (version == 6)
		{
			long oldData;
			for (int i = 0; i < x; i++)
			{
				oldData = 0;
				for (tempIndex = 0; tempIndex < 8; tempIndex++)
					oldData += (((long) inputData[index + tempIndex]) & 0xff) << (8 * tempIndex);
				index += 8;
				/*
				|a  |a  |a  |a  |r  |r  |r  |r  |
				|r  |r  |r  |r  |g  |g  |g  |g  |
				|g  |g  |g  |g  |b  |b  |b  |b  |
				|b  |b  |b  |b  |h  |h  |h  |h  |
				|h  |h  |h  |h  |h  |h  |d  |d  |
				|d  |d  |d  |d  |d  |d  |d  |d  |
				|bl |bl |bl |bl |sl |sl |sl |sl |
				|l  |l  |f  |g  |g  |g  |v  |e  |
				 */
				DataPointUtil.createDataPoint(
						(int)((oldData >> 60) << 4) + 15,
						(int)(oldData >> 52) & 0xFF,
						(int)(oldData >> 44) & 0xFF,
						(int)(oldData >> 36) & 0xFF,
						(int)(oldData >> 26) & 0x3FF - DataPointUtil.VERTICAL_OFFSET,
						(int)(oldData >> 16) & 0x3FF - DataPointUtil.VERTICAL_OFFSET,
						(int)(oldData >> 8) & 0xF,
						(int)(oldData >> 12) & 0xF,
						(int)(oldData >> 5) & 0x1,
						((oldData >> 5) & 0x1) == 1
				);
				tempDataContainerColor[i] = ThreadMapUtil.dataPointColor;
				tempDataContainerData[i] = ThreadMapUtil.dataPointData;
				tempDataContainerFlags[i] = ThreadMapUtil.dataPointFlags;
			}
		}
		else if (version == 7)
		{
			long oldData;
			for (int i = 0; i < x; i++)
			{
				oldData = 0;
				for (tempIndex = 0; tempIndex < 8; tempIndex++)
					oldData += (((long) inputData[index + tempIndex]) & 0xff) << (8 * tempIndex);
				index += 8;
				DataPointUtil.createDataPoint(
						(int)((oldData >> 60) << 4) + 15,
						(int)(oldData >> 52) & 0xFF,
						(int)(oldData >> 44) & 0xFF,
						(int)(oldData >> 36) & 0xFF,
						(int)(oldData >> 26) & 0x3FF - DataPointUtil.VERTICAL_OFFSET - 64,
						(int)(oldData >> 16) & 0x3FF - DataPointUtil.VERTICAL_OFFSET - 64,
						(int)(oldData >> 8) & 0xF,
						(int)(oldData >> 12) & 0xF,
						(int)(oldData >> 5) & 0x1,
						((oldData >> 5) & 0x1) == 1
				);
				tempDataContainerColor[i] = ThreadMapUtil.dataPointColor;
				tempDataContainerData[i] = ThreadMapUtil.dataPointData;
				tempDataContainerFlags[i] = ThreadMapUtil.dataPointFlags;
			}
		}
		else //if (version == 8)
		{
			int color;
			int data;
			for (int i = 0; i < x; i++)
			{
				byte flags = inputData[index];
				index++;
				data = 0;
				color = 0;
				for (tempIndex = 0; tempIndex < 4; tempIndex++)
				{
					data += (((int) inputData[index + tempIndex]) & 0xff) << (8 * tempIndex);
					color += (((int) inputData[index + tempIndex + 4]) & 0xff) << (8 * tempIndex);
				}
				index += 8;
				
				tempDataContainerColor[i] = color;
				tempDataContainerData[i] = data;
				tempDataContainerFlags[i] = flags;
			}
		}
		
		if (tempMaxVerticalData > DetailDistanceUtil.getMaxVerticalData(detailLevel))
		{
			int tempMaxVerticalData2 = DetailDistanceUtil.getMaxVerticalData(detailLevel);
			int[] dataToMergeColor = new int[tempMaxVerticalData];
			int[] dataToMergeData = new int[tempMaxVerticalData];
			byte[] dataToMergeFlags = new byte[tempMaxVerticalData];
			int[] tempDataContainer2Color = new int[size * size * tempMaxVerticalData2];
			int[] tempDataContainer2Data = new int[size * size * tempMaxVerticalData2];
			byte[] tempDataContainer2Flags = new byte[size * size * tempMaxVerticalData2];
			for (int i = 0; i < size * size; i++)
			{
				System.arraycopy(tempDataContainerColor, i * tempMaxVerticalData, dataToMergeColor, 0, tempMaxVerticalData);
				System.arraycopy(tempDataContainerData, i * tempMaxVerticalData, dataToMergeData, 0, tempMaxVerticalData);
				System.arraycopy(tempDataContainerFlags, i * tempMaxVerticalData, dataToMergeFlags, 0, tempMaxVerticalData);
				DataPointUtil.mergeMultiData(dataToMergeColor, dataToMergeData, dataToMergeFlags, tempMaxVerticalData, tempMaxVerticalData2);
				System.arraycopy(ThreadMapUtil.getRawVerticalDataArrayColor(), 0, tempDataContainer2Color, i * tempMaxVerticalData2, tempMaxVerticalData2);
				System.arraycopy(ThreadMapUtil.getRawVerticalDataArrayData(), 0, tempDataContainer2Data, i * tempMaxVerticalData2, tempMaxVerticalData2);
				System.arraycopy(ThreadMapUtil.getRawVerticalDataArrayFlags(), 0, tempDataContainer2Flags, i * tempMaxVerticalData2, tempMaxVerticalData2);
			}
			maxVerticalData = tempMaxVerticalData2;
			this.dataContainerColor = tempDataContainer2Color;
			this.dataContainerData = tempDataContainer2Data;
			this.dataContainerFlags = tempDataContainer2Flags;
		}
		else
		{
			maxVerticalData = tempMaxVerticalData;
			this.dataContainerColor = tempDataContainerColor;
			this.dataContainerData = tempDataContainerData;
			this.dataContainerFlags = tempDataContainerFlags;
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
		int[] dataToMergeColor = ThreadMapUtil.getVerticalUpdateArrayColor(detailLevel);
		int[] dataToMergeData = ThreadMapUtil.getVerticalUpdateArrayData(detailLevel);
		byte[] dataToMergeFlags = ThreadMapUtil.getVerticalUpdateArrayFlags(detailLevel);
		
		int lowerMaxVertical = dataToMergeFlags.length / 4;
		int childPosX;
		int childPosZ;
		posX = LevelPosUtil.getRegionModule(detailLevel, posX);
		posZ = LevelPosUtil.getRegionModule(detailLevel, posZ);
		for (int x = 0; x <= 1; x++)
		{
			for (int z = 0; z <= 1; z++)
			{
				childPosX = 2 * posX + x;
				childPosZ = 2 * posZ + z;
				for (int verticalIndex = 0; verticalIndex < lowerMaxVertical; verticalIndex++)
				{
					
					final int i = (z * 2 + x) * lowerMaxVertical + verticalIndex;
					dataToMergeColor[i] = lowerLevelContainer.getColor(childPosX, childPosZ, verticalIndex);
					dataToMergeData[i] = lowerLevelContainer.getData(childPosX, childPosZ, verticalIndex);
					dataToMergeFlags[i] = lowerLevelContainer.getFlags(childPosX, childPosZ, verticalIndex);
				}
			}
		}
		DataPointUtil.mergeMultiData(dataToMergeColor, dataToMergeData, dataToMergeFlags, lowerMaxVertical, getMaxVerticalData());
		addVerticalData(ThreadMapUtil.getRawVerticalDataArrayColor(), ThreadMapUtil.getRawVerticalDataArrayData(), ThreadMapUtil.getRawVerticalDataArrayFlags(), posX, posZ);
	}
	
	@Override
	public byte[] toDataString()
	{
		int index = 0;
		int x = size * size;
		int tempIndex;
		int currentColor;
		int currentData;
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
				currentColor = dataContainerColor[i * maxVerticalData + j];
				currentData = dataContainerData[i * maxVerticalData + j];
				tempData[index] = dataContainerFlags[i * maxVerticalData + j];
				index++;
				for (tempIndex = 0; tempIndex < 4; tempIndex++)
				{
					tempData[index + tempIndex] = (byte) (currentData >>> (8 * tempIndex));
					tempData[index + tempIndex + 4] = (byte) (currentColor >>> (8 * tempIndex));
				}
				index += 8;
			}
			if(!DataPointUtil.doesItExist(dataContainerFlags[i]))
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
		return size * size * getMaxVerticalData();
	}
}
