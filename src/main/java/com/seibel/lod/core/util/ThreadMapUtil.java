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

import com.seibel.lod.core.enums.LodDirection;
import com.seibel.lod.core.objects.VertexOptimizer;

// FIXME: Nuke this whole thing and use ThreadLocal instead. And no more redundant get() please!

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
	public static final ConcurrentMap<String, long[]> threadVerticalAddDataMap = new ConcurrentHashMap<>();
	public static final ConcurrentMap<String, short[]> projectionArrayMap = new ConcurrentHashMap<>();
	public static final ConcurrentMap<String, short[]> heightAndDepthMap = new ConcurrentHashMap<>();
	public static final ConcurrentMap<String, long[]> singleDataToMergeMap = new ConcurrentHashMap<>();
	public static final ConcurrentMap<String, long[][]> verticalUpdate = new ConcurrentHashMap<>();
	
	
	//________________________//
	// used in BufferBuilder  //
	//________________________//
	
	public static final ConcurrentMap<String, boolean[]> adjShadeDisabled = new ConcurrentHashMap<>();
	public static final ConcurrentMap<String, Map<LodDirection, long[]>> adjDataMap = new ConcurrentHashMap<>();
	public static final ConcurrentMap<String, VertexOptimizer> boxMap = new ConcurrentHashMap<>();
	
	
	
	/** returns the array NOT cleared every time */
	public static boolean[] getAdjShadeDisabledArray()
	{
		if (!adjShadeDisabled.containsKey(Thread.currentThread().getName())
				|| (adjShadeDisabled.get(Thread.currentThread().getName()) == null))
		{
			adjShadeDisabled.put(Thread.currentThread().getName(), new boolean[VertexOptimizer.DIRECTIONS.length]);
		}
		Arrays.fill(adjShadeDisabled.get(Thread.currentThread().getName()), false);
		return adjShadeDisabled.get(Thread.currentThread().getName());
	}
	
	/** returns the array NOT cleared every time */
	public static Map<LodDirection, long[]> getAdjDataArray(int verticalData)
	{
		if (!adjDataMap.containsKey(Thread.currentThread().getName())
				|| (adjDataMap.get(Thread.currentThread().getName()) == null)
				|| (adjDataMap.get(Thread.currentThread().getName()).get(LodDirection.NORTH) == null)
				|| (adjDataMap.get(Thread.currentThread().getName()).get(LodDirection.NORTH).length != verticalData))
		{
			adjDataMap.put(Thread.currentThread().getName(), new HashMap<>());
			adjDataMap.get(Thread.currentThread().getName()).put(LodDirection.UP, new long[1]);
			adjDataMap.get(Thread.currentThread().getName()).put(LodDirection.DOWN, new long[1]);
			for (LodDirection lodDirection : VertexOptimizer.ADJ_DIRECTIONS)
				adjDataMap.get(Thread.currentThread().getName()).put(lodDirection, new long[verticalData]);
		}
		else
		{
			
			for (LodDirection lodDirection : VertexOptimizer.ADJ_DIRECTIONS)
				Arrays.fill(adjDataMap.get(Thread.currentThread().getName()).get(lodDirection), DataPointUtil.EMPTY_DATA);
		}
		return adjDataMap.get(Thread.currentThread().getName());
	}
	
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
	
	//________________________//
	// used in DataPointUtil  //
	// mergeVerticalData      //
	//________________________//
	
	
	//________________________//
	// used in DataPointUtil  //
	// mergeSingleData        //
	//________________________//
	
	

	//TODO: Maybe use actual valid total world height instead of always assuming the worse and alloc 1024 blocks.
	/** returns the array filled with 0's */
	public static long[] getBuilderVerticalArray(int detailLevel)
	{
		if (!threadBuilderVerticalArrayMap.containsKey(Thread.currentThread().getName()) || (threadBuilderVerticalArrayMap.get(Thread.currentThread().getName()) == null))
		{
			long[][] array = new long[5][];
			int size;
			for (int i = 0; i < 5; i++)
			{
				size = 1 << i;
				array[i] = new long[size * size * (DataPointUtil.WORLD_HEIGHT / 2 + 1)];
			}
			threadBuilderVerticalArrayMap.put(Thread.currentThread().getName(), array);
		}
		Arrays.fill(threadBuilderVerticalArrayMap.get(Thread.currentThread().getName())[detailLevel], 0);
		return threadBuilderVerticalArrayMap.get(Thread.currentThread().getName())[detailLevel];
	}
	
	/** returns the array filled with 0's */
	public static long[] getVerticalDataArray(int arrayLength)
	{
		long[] array = threadVerticalAddDataMap.get(Thread.currentThread().getName());
		if (array == null || array.length != arrayLength)
		{
			array = new long[arrayLength];
			threadVerticalAddDataMap.put(Thread.currentThread().getName(), array);
		}
		else
			Arrays.fill(array, 0);
		return array;
	}
	

	//FIXME: If the arrayLength change, this may return incorrect sized array
	/** returns the array NOT cleared every time */
	public static short[] getHeightAndDepth(int arrayLength)
	{
		if (!heightAndDepthMap.containsKey(Thread.currentThread().getName()) || (heightAndDepthMap.get(Thread.currentThread().getName()) == null))
		{
			heightAndDepthMap.put(Thread.currentThread().getName(), new short[arrayLength]);
		}
		return heightAndDepthMap.get(Thread.currentThread().getName());
	}
	
	
	/** returns the array filled with 0's */
	public static long[] getVerticalUpdateArray(int detailLevel)
	{
		detailLevel--; //we are asking for a place to fit lower level detail, so this will never get called with value of 0
		long[][] arrays = verticalUpdate.get(Thread.currentThread().getName());
		if (arrays == null)
		{
			arrays = new long[LodUtil.DETAIL_OPTIONS - 1][];
			verticalUpdate.put(Thread.currentThread().getName(), arrays);
		}
		long[] array = arrays[detailLevel];
		int arrayLength = DetailDistanceUtil.getMaxVerticalData(detailLevel) * 4;
		if (array == null || array.length != arrayLength)
			array = new long[arrayLength];
		else
			Arrays.fill(array, 0);
		return array;
	}
	
	/** clears all arrays so they will have to be rebuilt */
	public static void clearMaps()
	{
		adjShadeDisabled.clear();
		adjDataMap.clear();
		boxMap.clear();
		threadSingleUpdateMap.clear();
		threadBuilderArrayMap.clear();
		threadBuilderVerticalArrayMap.clear();
		threadVerticalAddDataMap.clear();
		projectionArrayMap.clear();
		heightAndDepthMap.clear();
		singleDataToMergeMap.clear();
		verticalUpdate.clear();
	}
}
