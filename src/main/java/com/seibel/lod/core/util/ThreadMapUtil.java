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
	public static final ConcurrentMap<String, int[][]> threadBuilderVerticalArrayMapColor = new ConcurrentHashMap<>();
	public static final ConcurrentMap<String, int[][]> threadBuilderVerticalArrayMapData = new ConcurrentHashMap<>();
	public static final ConcurrentMap<String, byte[][]> threadBuilderVerticalArrayMapFlags = new ConcurrentHashMap<>();
	public static final ConcurrentMap<String, int[]> threadVerticalAddDataMapColor = new ConcurrentHashMap<>();
	public static final ConcurrentMap<String, int[]> threadVerticalAddDataMapData = new ConcurrentHashMap<>();
	public static final ConcurrentMap<String, byte[]> threadVerticalAddDataMapFlags = new ConcurrentHashMap<>();
	public static final ConcurrentMap<String, byte[][]> saveContainer = new ConcurrentHashMap<>();
	public static final ConcurrentMap<String, short[]> projectionArrayMap = new ConcurrentHashMap<>();
	public static final ConcurrentMap<String, short[]> heightAndDepthMap = new ConcurrentHashMap<>();
	public static final ConcurrentMap<String, int[][]> verticalUpdateColor = new ConcurrentHashMap<>();
	public static final ConcurrentMap<String, int[][]> verticalUpdateData = new ConcurrentHashMap<>();
	public static final ConcurrentMap<String, byte[][]> verticalUpdateFlags = new ConcurrentHashMap<>();
	
	
	//________________________//
	// used in BufferBuilder  //
	//________________________//
	
	public static final ConcurrentMap<String, boolean[]> adjShadeDisabled = new ConcurrentHashMap<>();
	public static final ConcurrentMap<String, Map<LodDirection, int[]>> adjDataMap = new ConcurrentHashMap<>();
	public static final ConcurrentMap<String, Map<LodDirection, byte[]>> adjFlagsMap = new ConcurrentHashMap<>();
	public static final ConcurrentMap<String, VertexOptimizer> boxMap = new ConcurrentHashMap<>();
	
	public static int dataPointColor = 0;
	public static int dataPointData = 0;
	public static byte dataPointFlags = 0;
	
	
	
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
	public static Map<LodDirection, int[]> getAdjDataArray(int verticalData)
	{
		if (!adjDataMap.containsKey(Thread.currentThread().getName())
				|| (adjDataMap.get(Thread.currentThread().getName()) == null)
				|| (adjDataMap.get(Thread.currentThread().getName()).get(LodDirection.NORTH) == null)
				|| (adjDataMap.get(Thread.currentThread().getName()).get(LodDirection.NORTH).length != verticalData))
		{
			adjDataMap.put(Thread.currentThread().getName(), new HashMap<>());
			adjDataMap.get(Thread.currentThread().getName()).put(LodDirection.UP, new int[1]);
			adjDataMap.get(Thread.currentThread().getName()).put(LodDirection.DOWN, new int[1]);
			for (LodDirection lodDirection : VertexOptimizer.ADJ_DIRECTIONS)
				adjDataMap.get(Thread.currentThread().getName()).put(lodDirection, new int[verticalData]);
		}
		else
		{
			for (LodDirection lodDirection : VertexOptimizer.ADJ_DIRECTIONS)
				Arrays.fill(adjDataMap.get(Thread.currentThread().getName()).get(lodDirection), 0);
		}
		return adjDataMap.get(Thread.currentThread().getName());
	}
	
	/** returns the array NOT cleared every time */
	public static Map<LodDirection, byte[]> getAdjFlagsArray(int verticalData)
	{
		if (!adjFlagsMap.containsKey(Thread.currentThread().getName())
				|| (adjFlagsMap.get(Thread.currentThread().getName()) == null)
				|| (adjFlagsMap.get(Thread.currentThread().getName()).get(LodDirection.NORTH) == null)
				|| (adjFlagsMap.get(Thread.currentThread().getName()).get(LodDirection.NORTH).length != verticalData))
		{
			adjFlagsMap.put(Thread.currentThread().getName(), new HashMap<>());
			adjFlagsMap.get(Thread.currentThread().getName()).put(LodDirection.UP, new byte[1]);
			adjFlagsMap.get(Thread.currentThread().getName()).put(LodDirection.DOWN, new byte[1]);
			for (LodDirection lodDirection : VertexOptimizer.ADJ_DIRECTIONS)
				adjFlagsMap.get(Thread.currentThread().getName()).put(lodDirection, new byte[verticalData]);
		}
		else
		{
			for (LodDirection lodDirection : VertexOptimizer.ADJ_DIRECTIONS)
				Arrays.fill(adjFlagsMap.get(Thread.currentThread().getName()).get(lodDirection), (byte) 0);
		}
		return adjFlagsMap.get(Thread.currentThread().getName());
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
	
	
	
	/** returns the array filled with 0's */
	public static int[] getBuilderVerticalArrayColor(int detailLevel)
	{
		if (!threadBuilderVerticalArrayMapColor.containsKey(Thread.currentThread().getName()) || (threadBuilderVerticalArrayMapColor.get(Thread.currentThread().getName()) == null))
		{
			int[][] array = new int[5][];
			int size;
			for (int i = 0; i < 5; i++)
			{
				size = 1 << i;
				array[i] = new int[size * size * (DataPointUtil.WORLD_HEIGHT / 2 + 1)];
			}
			threadBuilderVerticalArrayMapColor.put(Thread.currentThread().getName(), array);
		}
		Arrays.fill(threadBuilderVerticalArrayMapColor.get(Thread.currentThread().getName())[detailLevel], 0);
		return threadBuilderVerticalArrayMapColor.get(Thread.currentThread().getName())[detailLevel];
	}
	
	/** returns the array filled with 0's */
	public static int[] getBuilderVerticalArrayData(int detailLevel)
	{
		if (!threadBuilderVerticalArrayMapData.containsKey(Thread.currentThread().getName()) || (threadBuilderVerticalArrayMapData.get(Thread.currentThread().getName()) == null))
		{
			int[][] array = new int[5][];
			int size;
			for (int i = 0; i < 5; i++)
			{
				size = 1 << i;
				array[i] = new int[size * size * (DataPointUtil.WORLD_HEIGHT / 2 + 1)];
			}
			threadBuilderVerticalArrayMapData.put(Thread.currentThread().getName(), array);
		}
		Arrays.fill(threadBuilderVerticalArrayMapData.get(Thread.currentThread().getName())[detailLevel], 0);
		return threadBuilderVerticalArrayMapData.get(Thread.currentThread().getName())[detailLevel];
	}
	
	/** returns the array filled with 0's */
	public static byte[] getBuilderVerticalArrayFlags(int detailLevel)
	{
		if (!threadBuilderVerticalArrayMapFlags.containsKey(Thread.currentThread().getName()) || (threadBuilderVerticalArrayMapFlags.get(Thread.currentThread().getName()) == null))
		{
			byte[][] array = new byte[5][];
			int size;
			for (int i = 0; i < 5; i++)
			{
				size = 1 << i;
				array[i] = new byte[size * size * (DataPointUtil.WORLD_HEIGHT / 2 + 1)];
			}
			threadBuilderVerticalArrayMapFlags.put(Thread.currentThread().getName(), array);
		}
		Arrays.fill(threadBuilderVerticalArrayMapFlags.get(Thread.currentThread().getName())[detailLevel], (byte) 0);
		return threadBuilderVerticalArrayMapFlags.get(Thread.currentThread().getName())[detailLevel];
	}
	
	/** returns the array NOT cleared every time */
	public static byte[] getSaveContainer(int detailLevel)
	{
		if (!saveContainer.containsKey(Thread.currentThread().getName()) || (saveContainer.get(Thread.currentThread().getName()) == null))
		{
			byte[][] array = new byte[LodUtil.DETAIL_OPTIONS][];
			int size = 1;
			for (int i = LodUtil.DETAIL_OPTIONS - 1; i >= 0; i--)
			{
				array[i] = new byte[2 + 9 * size * size * DetailDistanceUtil.getMaxVerticalData(i)];
				size = size << 1;
			}
			saveContainer.put(Thread.currentThread().getName(), array);
		}
		//Arrays.fill(threadBuilderVerticalArrayMap.get(Thread.currentThread().getName())[detailLevel], 0);
		return saveContainer.get(Thread.currentThread().getName())[detailLevel];
	}
	
	
	/** returns the array filled with 0's */
	public static int[] getVerticalDataArrayColor(int arrayLength)
	{
		if (!threadVerticalAddDataMapColor.containsKey(Thread.currentThread().getName()) || (threadVerticalAddDataMapColor.get(Thread.currentThread().getName()) == null))
			threadVerticalAddDataMapColor.put(Thread.currentThread().getName(), new int[arrayLength]);
		else
			Arrays.fill(threadVerticalAddDataMapColor.get(Thread.currentThread().getName()), 0);
		return threadVerticalAddDataMapColor.get(Thread.currentThread().getName());
	}
	public static int[] getRawVerticalDataArrayColor()
	{
		return threadVerticalAddDataMapColor.get(Thread.currentThread().getName());
	}
	
	/** returns the array filled with 0's */
	public static int[] getVerticalDataArrayData(int arrayLength)
	{
		if (!threadVerticalAddDataMapData.containsKey(Thread.currentThread().getName()) || (threadVerticalAddDataMapData.get(Thread.currentThread().getName()) == null))
			threadVerticalAddDataMapData.put(Thread.currentThread().getName(), new int[arrayLength]);
		else
			Arrays.fill(threadVerticalAddDataMapData.get(Thread.currentThread().getName()), 0);
		return threadVerticalAddDataMapData.get(Thread.currentThread().getName());
	}
	public static int[] getRawVerticalDataArrayData()
	{
		return threadVerticalAddDataMapData.get(Thread.currentThread().getName());
	}
	
	/** returns the array filled with 0's */
	public static byte[] getVerticalDataArrayFlags(int arrayLength)
	{
		if (!threadVerticalAddDataMapFlags.containsKey(Thread.currentThread().getName()) || (threadVerticalAddDataMapFlags.get(Thread.currentThread().getName()) == null))
			threadVerticalAddDataMapFlags.put(Thread.currentThread().getName(), new byte[arrayLength]);
		else
			Arrays.fill(threadVerticalAddDataMapFlags.get(Thread.currentThread().getName()), (byte) 0);
		return threadVerticalAddDataMapFlags.get(Thread.currentThread().getName());
	}
	public static byte[] getRawVerticalDataArrayFlags()
	{
		return threadVerticalAddDataMapFlags.get(Thread.currentThread().getName());
	}
	
	
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
	public static int[] getVerticalUpdateArrayColor(int detailLevel)
	{
		if (!verticalUpdateColor.containsKey(Thread.currentThread().getName()) || (verticalUpdateColor.get(Thread.currentThread().getName()) == null))
		{
			int[][] array = new int[LodUtil.DETAIL_OPTIONS][];
			for (int i = 1; i < LodUtil.DETAIL_OPTIONS; i++)
				array[i] = new int[DetailDistanceUtil.getMaxVerticalData(i - 1) * 4];
			verticalUpdateColor.put(Thread.currentThread().getName(), array);
		}
		else
			Arrays.fill(verticalUpdateColor.get(Thread.currentThread().getName())[detailLevel], 0);
		return verticalUpdateColor.get(Thread.currentThread().getName())[detailLevel];
	}
	
	/** returns the array filled with 0's */
	public static int[] getVerticalUpdateArrayData(int detailLevel)
	{
		if (!verticalUpdateData.containsKey(Thread.currentThread().getName()) || (verticalUpdateData.get(Thread.currentThread().getName()) == null))
		{
			int[][] array = new int[LodUtil.DETAIL_OPTIONS][];
			for (int i = 1; i < LodUtil.DETAIL_OPTIONS; i++)
				array[i] = new int[DetailDistanceUtil.getMaxVerticalData(i - 1) * 4];
			verticalUpdateData.put(Thread.currentThread().getName(), array);
		}
		else
			Arrays.fill(verticalUpdateData.get(Thread.currentThread().getName())[detailLevel], 0);
		return verticalUpdateData.get(Thread.currentThread().getName())[detailLevel];
	}
	
	/** returns the array filled with 0's */
	public static byte[] getVerticalUpdateArrayFlags(int detailLevel)
	{
		if (!verticalUpdateFlags.containsKey(Thread.currentThread().getName()) || (verticalUpdateFlags.get(Thread.currentThread().getName()) == null))
		{
			byte[][] array = new byte[LodUtil.DETAIL_OPTIONS][];
			for (int i = 1; i < LodUtil.DETAIL_OPTIONS; i++)
				array[i] = new byte[DetailDistanceUtil.getMaxVerticalData(i - 1) * 4];
			verticalUpdateFlags.put(Thread.currentThread().getName(), array);
		}
		else
			Arrays.fill(verticalUpdateFlags.get(Thread.currentThread().getName())[detailLevel], (byte) 0);
		return verticalUpdateFlags.get(Thread.currentThread().getName())[detailLevel];
	}
	
	/** clears all arrays so they will have to be rebuilt */
	public static void clearMaps()
	{
		adjShadeDisabled.clear();
		adjDataMap.clear();
		boxMap.clear();
		threadBuilderVerticalArrayMapColor.clear();
		threadBuilderVerticalArrayMapData.clear();
		threadBuilderVerticalArrayMapFlags.clear();
		threadVerticalAddDataMapColor.clear();
		threadVerticalAddDataMapData.clear();
		threadVerticalAddDataMapFlags.clear();
		saveContainer.clear();
		projectionArrayMap.clear();
		heightAndDepthMap.clear();
		verticalUpdateColor.clear();
		verticalUpdateData.clear();
		verticalUpdateFlags.clear();
	}
	
	public static void saveDataPoint(int color, int data, byte flags)
	{
		dataPointColor = color;
		dataPointData = data;
		dataPointFlags = flags;
	}
}
