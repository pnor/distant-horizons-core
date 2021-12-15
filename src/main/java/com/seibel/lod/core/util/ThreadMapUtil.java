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

package com.seibel.lod.core.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.seibel.lod.core.dataFormat.VerticalDataFormat;
import com.seibel.lod.core.enums.LodDirection;
import com.seibel.lod.core.objects.VertexOptimizer;

/**
 * Holds data used by specific threads so
 * the data doesn't have to be recreated every
 * time it is needed.
 * 
 * @author Leonardo Amato
 * @version 9-25-2021
 */
public class ThreadMapUtil
{
	public static final ConcurrentMap<String, long[]> threadSingleUpdateMap = new ConcurrentHashMap<>();
	public static final ConcurrentMap<String, long[][]> threadBuilderArrayMap = new ConcurrentHashMap<>();
	public static final ConcurrentMap<String, long[][]> threadBuilderVerticalArrayMap = new ConcurrentHashMap<>();
	public static final ConcurrentMap<String, byte[][]> saveContainer = new ConcurrentHashMap<>();
	public static final ConcurrentMap<String, long[][]> verticalUpdate = new ConcurrentHashMap<>();
	
	//___________________//
	// used in the Merge //
	//___________________//
	public static final ConcurrentMap<String, short[]> heightAndDepthMap = new ConcurrentHashMap<>();
	
	/** returns the array NOT cleared every time */
	public static short[] getHeightAndDepth(int arrayLength)
	{
		if (!heightAndDepthMap.containsKey(Thread.currentThread().getName()) || (heightAndDepthMap.get(Thread.currentThread().getName()) == null))
		{
			heightAndDepthMap.put(Thread.currentThread().getName(), new short[arrayLength]);
		}
		return heightAndDepthMap.get(Thread.currentThread().getName());
	}
	//_________________________//
	// used in the lodBuilder  //
	//_________________________//
	
	public static final ConcurrentMap<String, short[][]> positionDataBuildingArrayMap = new ConcurrentHashMap<>();
	public static final ConcurrentMap<String, int[][]> verticalDataBuildingArrayMap = new ConcurrentHashMap<>();
	public static final ConcurrentMap<String, int[][]> colorDataBuildingArrayMap = new ConcurrentHashMap<>();
	public static final ConcurrentMap<String, byte[][]> lightDataBuildingArrayMap = new ConcurrentHashMap<>();
	
	
	public static short[] getPositionDataBuilding(int detail)
	{
		if (!positionDataUpdateArrayMap.containsKey(Thread.currentThread().getName())
					|| (positionDataUpdateArrayMap.get(Thread.currentThread().getName()) == null))
		{
			int count = LodUtil.DETAIL_OPTIONS - 1;
			short[][] tempArray = new short[count][];
			for(int tempDetail = 0; tempDetail < count; tempDetail++)
			{
				tempArray[tempDetail] = new short[(1 << tempDetail) * (1 << tempDetail)];
			}
			positionDataBuildingArrayMap.put(Thread.currentThread().getName(), tempArray);
		}
		
		short[] tempArray = positionDataBuildingArrayMap.get(Thread.currentThread().getName())[detail];
		Arrays.fill(tempArray, (short) 0);
		return tempArray;
	}
	
	public static int[] getVerticalDataBuilding(byte detail)
	{
		if (!verticalDataUpdateArrayMap.containsKey(Thread.currentThread().getName())
					|| (verticalDataUpdateArrayMap.get(Thread.currentThread().getName()) == null))
		{
			int verticalSize = VerticalDataFormat.WORLD_HEIGHT / 2 + 1;
			int count = LodUtil.DETAIL_OPTIONS - 1;
			int[][] tempArray = new int[count][];
			for(int tempDetail = 0; tempDetail < count; tempDetail++)
			{
				tempArray[tempDetail] = new int[(1 << tempDetail) * (1 << tempDetail) * verticalSize];
			}
			verticalDataUpdateArrayMap.put(Thread.currentThread().getName(), tempArray);
		}
		
		int[] tempArray = verticalDataUpdateArrayMap.get(Thread.currentThread().getName())[detail];
		Arrays.fill(tempArray, 0);
		return tempArray;
	}
	
	public static int[] getColorDataBuilding(byte detail)
	{
		if (!colorDataUpdateArrayMap.containsKey(Thread.currentThread().getName())
					|| (colorDataUpdateArrayMap.get(Thread.currentThread().getName()) == null))
		{
			int verticalSize = VerticalDataFormat.WORLD_HEIGHT / 2 + 1;
			int count = LodUtil.DETAIL_OPTIONS - 1;
			int[][] tempArray = new int[count][];
			for(int tempDetail = 0; tempDetail < count; tempDetail++)
			{
				tempArray[tempDetail] = new int[(1 << tempDetail) * (1 << tempDetail) * verticalSize];
			}
			colorDataUpdateArrayMap.put(Thread.currentThread().getName(), tempArray);
		}
		
		int[] tempArray = colorDataUpdateArrayMap.get(Thread.currentThread().getName())[detail];
		Arrays.fill(tempArray, 0);
		return tempArray;
	}
	
	public static byte[] getLightDataBuilding(byte detail)
	{
		if (!lightDataUpdateArrayMap.containsKey(Thread.currentThread().getName())
					|| (lightDataUpdateArrayMap.get(Thread.currentThread().getName()) == null))
		{
			int verticalSize = VerticalDataFormat.WORLD_HEIGHT / 2 + 1;
			int count = LodUtil.DETAIL_OPTIONS - 1;
			byte[][] tempArray = new byte[count][];
			for(int tempDetail = 0; tempDetail < count; tempDetail++)
			{
				tempArray[tempDetail] = new byte[(1 << tempDetail) * (1 << tempDetail) * verticalSize];
			}
			lightDataUpdateArrayMap.put(Thread.currentThread().getName(), tempArray);
		}
		
		byte[] tempArray = lightDataUpdateArrayMap.get(Thread.currentThread().getName())[detail];
		Arrays.fill(tempArray, (byte) 0);
		return tempArray;
	}
	
	//_____________________//
	// used in the update  //
	//_____________________//
	
	public static final ConcurrentMap<String, short[]> positionDataUpdateArrayMap = new ConcurrentHashMap<>();
	public static final ConcurrentMap<String, int[][]> verticalDataUpdateArrayMap = new ConcurrentHashMap<>();
	public static final ConcurrentMap<String, int[][]> colorDataUpdateArrayMap = new ConcurrentHashMap<>();
	public static final ConcurrentMap<String, byte[][]> lightDataUpdateArrayMap = new ConcurrentHashMap<>();
	
	
	public static short[] getPositionDataArray()
	{
		if (!positionDataUpdateArrayMap.containsKey(Thread.currentThread().getName())
					|| (positionDataUpdateArrayMap.get(Thread.currentThread().getName()) == null))
		{
			positionDataUpdateArrayMap.put(Thread.currentThread().getName(), new short[4]);
		}
		Arrays.fill(positionDataUpdateArrayMap.get(Thread.currentThread().getName()), (short) 0);
		return positionDataUpdateArrayMap.get(Thread.currentThread().getName());
	}
	
	public static int[] getVerticalDataArray(byte detail)
	{
		if (!verticalDataUpdateArrayMap.containsKey(Thread.currentThread().getName())
					|| (verticalDataUpdateArrayMap.get(Thread.currentThread().getName()) == null))
		{
			int count = LodUtil.DETAIL_OPTIONS - 1;
			int[][] tempArray = new int[count][];
			for(int tempDetail = 0; tempDetail < count; tempDetail++)
			{
				tempArray[tempDetail] = new int[4 * DetailDistanceUtil.getMaxVerticalData(detail)];
			}
			verticalDataUpdateArrayMap.put(Thread.currentThread().getName(), tempArray);
		}
		
		int[] tempArray = verticalDataUpdateArrayMap.get(Thread.currentThread().getName())[detail];
		Arrays.fill(tempArray, 0);
		return tempArray;
	}
	
	public static int[] getColorDataArray(byte detail)
	{
		if (!colorDataUpdateArrayMap.containsKey(Thread.currentThread().getName())
					|| (colorDataUpdateArrayMap.get(Thread.currentThread().getName()) == null))
		{
			int count = LodUtil.DETAIL_OPTIONS - 1;
			int[][] tempArray = new int[count][];
			for(int tempDetail = 0; tempDetail < count; tempDetail++)
			{
				tempArray[tempDetail] = new int[4 * DetailDistanceUtil.getMaxVerticalData(detail)];
			}
			colorDataUpdateArrayMap.put(Thread.currentThread().getName(), tempArray);
		}
		
		int[] tempArray = colorDataUpdateArrayMap.get(Thread.currentThread().getName())[detail];
		Arrays.fill(tempArray, 0);
		return tempArray;
	}
	
	public static byte[] getLightDataArray(byte detail)
	{
		if (!lightDataUpdateArrayMap.containsKey(Thread.currentThread().getName())
					|| (lightDataUpdateArrayMap.get(Thread.currentThread().getName()) == null))
		{
			int count = LodUtil.DETAIL_OPTIONS - 1;
			byte[][] tempArray = new byte[count][];
			for(int tempDetail = 0; tempDetail < count; tempDetail++)
			{
				tempArray[tempDetail] = new byte[4 * DetailDistanceUtil.getMaxVerticalData(detail)];
			}
			lightDataUpdateArrayMap.put(Thread.currentThread().getName(), tempArray);
		}
		
		byte[] tempArray = lightDataUpdateArrayMap.get(Thread.currentThread().getName())[detail];
		Arrays.fill(tempArray, (byte) 0);
		return tempArray;
	}
	
	//________________________//
	// used in BufferBuilder  //
	//________________________//
	
	public static final ConcurrentMap<String, VertexOptimizer> boxMap = new ConcurrentHashMap<>();
	
	
	public static VertexOptimizer getBox()
	{
		if (!boxMap.containsKey(Thread.currentThread().getName())
				|| (boxMap.get(Thread.currentThread().getName()) == null))
		{
			boxMap.put(Thread.currentThread().getName(), new VertexOptimizer());
		}
		boxMap.get(Thread.currentThread().getName()).reset();
		return boxMap.get(Thread.currentThread().getName());
	}
	
	/** returns the array NOT cleared every time */
	public static byte[] getSaveContainer(int detailLevel)
	{
		if (!saveContainer.containsKey(Thread.currentThread().getName()) || (saveContainer.get(Thread.currentThread().getName()) == null))
		{
			byte[][] array = new byte[LodUtil.DETAIL_OPTIONS][];
			int size = 1;
			int posSize;
			int fullSize;
			for (int i = LodUtil.DETAIL_OPTIONS - 1; i >= 0; i--)
			{
				posSize = size * size;
				fullSize = size * size * DetailDistanceUtil.getMaxVerticalData(i);
				array[i] = new byte[4 + 2 * posSize + 9 * fullSize];
				size = size << 1;
			}
			saveContainer.put(Thread.currentThread().getName(), array);
		}
		//Arrays.fill(threadBuilderVerticalArrayMap.get(Thread.currentThread().getName())[detailLevel], 0);
		return saveContainer.get(Thread.currentThread().getName())[detailLevel];
	}
	
	/** returns the array filled with 0's */
	public static long[] getVerticalUpdateArray(int detailLevel)
	{
		if (!verticalUpdate.containsKey(Thread.currentThread().getName()) || (verticalUpdate.get(Thread.currentThread().getName()) == null))
		{
			long[][] array = new long[LodUtil.DETAIL_OPTIONS][];
			for (int i = 1; i < LodUtil.DETAIL_OPTIONS; i++)
				array[i] = new long[DetailDistanceUtil.getMaxVerticalData(i - 1) * 4];
			verticalUpdate.put(Thread.currentThread().getName(), array);
		}
		else
		{
			Arrays.fill(verticalUpdate.get(Thread.currentThread().getName())[detailLevel], 0);
		}
		return verticalUpdate.get(Thread.currentThread().getName())[detailLevel];
	}
	
	
	/** clears all arrays so they will have to be rebuilt */
	public static void clearMaps()
	{
		boxMap.clear();
		threadSingleUpdateMap.clear();
		threadBuilderArrayMap.clear();
		threadBuilderVerticalArrayMap.clear();
		saveContainer.clear();
		heightAndDepthMap.clear();
		verticalUpdate.clear();
	}
}
