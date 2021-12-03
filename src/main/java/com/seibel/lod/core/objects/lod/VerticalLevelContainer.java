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
	
	public final DataPoint[] dataContainer;
	
	public VerticalLevelContainer(byte detailLevel)
	{
		this.detailLevel = detailLevel;
		size = 1 << (LodUtil.REGION_DETAIL_LEVEL - detailLevel);
		maxVerticalData = DetailDistanceUtil.getMaxVerticalData(detailLevel);
		dataContainer = new DataPoint[size * size * DetailDistanceUtil.getMaxVerticalData(detailLevel)];
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
			dataContainer[posX * size * maxVerticalData + posZ * maxVerticalData + verticalIndex] = DataPointUtil.EMPTY_DATA;
		}
	}
	
	@Override
	public boolean addData(DataPoint data, int posX, int posZ, int verticalIndex)
	{
		posX = LevelPosUtil.getRegionModule(detailLevel, posX);
		posZ = LevelPosUtil.getRegionModule(detailLevel, posZ);
		dataContainer[posX * size * maxVerticalData + posZ * maxVerticalData + verticalIndex] = data;
		return true;
	}
	
	@Override
	public boolean addVerticalData(DataPoint[] data, int posX, int posZ)
	{
		posX = LevelPosUtil.getRegionModule(detailLevel, posX);
		posZ = LevelPosUtil.getRegionModule(detailLevel, posZ);
		for (int verticalIndex = 0; verticalIndex < maxVerticalData; verticalIndex++)
			dataContainer[posX * size * maxVerticalData + posZ * maxVerticalData + verticalIndex] = data[verticalIndex];
		return true;
	}
	
	@Override
	public boolean addSingleData(DataPoint data, int posX, int posZ)
	{
		return addData(data, posX, posZ, 0);
	}
	
	@Override
	public DataPoint getData(int posX, int posZ, int verticalIndex)
	{
		posX = LevelPosUtil.getRegionModule(detailLevel, posX);
		posZ = LevelPosUtil.getRegionModule(detailLevel, posZ);
		return dataContainer[posX * size * maxVerticalData + posZ * maxVerticalData + verticalIndex];
	}
	
	@Override
	public DataPoint getSingleData(int posX, int posZ)
	{
		return getData(posX, posZ, 0);
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
		return DataPointUtil.doesItExist(getSingleData(posX, posZ));
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
		DataPoint[] tempDataContainer = new DataPoint[x];
		
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
				DataPoint newData = DataPointUtil.createDataPoint(
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
				tempDataContainer[i] = newData;
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
				DataPoint newData = DataPointUtil.createDataPoint(
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
				tempDataContainer[i] = newData;
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
				
				tempDataContainer[i] = new DataPoint(color, data, flags);
			}
		}
		
		if (tempMaxVerticalData > DetailDistanceUtil.getMaxVerticalData(detailLevel))
		{
			int tempMaxVerticalData2 = DetailDistanceUtil.getMaxVerticalData(detailLevel);
			DataPoint[] dataToMerge = new DataPoint[tempMaxVerticalData];
			DataPoint[] tempDataContainer2 = new DataPoint[size * size * tempMaxVerticalData2];
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
		DataPoint[] dataToMerge = ThreadMapUtil.getVerticalUpdateArray(detailLevel);
		
		int lowerMaxVertical = dataToMerge.length / 4;
		int childPosX;
		int childPosZ;
		DataPoint[] data;
		posX = LevelPosUtil.getRegionModule(detailLevel, posX);
		posZ = LevelPosUtil.getRegionModule(detailLevel, posZ);
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
		data = DataPointUtil.mergeMultiData(dataToMerge, lowerMaxVertical, getMaxVerticalData());
		
		addVerticalData(data, posX, posZ);
	}
	
	@Override
	public byte[] toDataString()
	{
		int index = 0;
		int x = size * size;
		int tempIndex;
		DataPoint current;
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
				if (current != null)
				{
					tempData[index] = current.flags;
					index++;
					for (tempIndex = 0; tempIndex < 4; tempIndex++)
					{
						tempData[index + tempIndex] = (byte) (current.data >>> (8 * tempIndex));
						tempData[index + tempIndex + 4] = (byte) (current.color >>> (8 * tempIndex));
					}
				}
				index += 8;
			}
			if(dataContainer[i] == null || !DataPointUtil.doesItExist(dataContainer[i]))
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
