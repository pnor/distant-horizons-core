/*
 *    This file is part of the Distant Horizons mod (formerly the LOD Mod),
 *    licensed under the GNU LGPL v3 License.
 *
 *    Copyright (C) 2020-2022  James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.seibel.lod.core.a7.datatype.column;

import com.seibel.lod.core.a7.datatype.column.accessor.ColumnArrayView;
import com.seibel.lod.core.a7.datatype.column.accessor.IColumnDataView;
import com.seibel.lod.core.logging.SpamReducedLogger;
import com.seibel.lod.core.util.ColorUtil;

import java.util.Arrays;


/**
 * 
 * @author Leonardo Amato
 * @version ??
 */
public class ColumnFormat
{
	/*
	
	|_  |g  |g  |g  |a  |a  |a  |a  |
	|r  |r  |r  |r  |r  |r  |r  |r  |
	|g  |g  |g  |g  |g  |g  |g  |g  |
	|b  |b  |b  |b  |b  |b  |b  |b  |
	
	|h  |h  |h  |h  |h  |h  |h  |h  |
	|h  |h  |h  |h  |d  |d  |d  |d  |
	|d  |d  |d  |d  |d  |d  |d  |d  |
	|bl |bl |bl |bl |sl |sl |sl |sl |
	
	*/
	
	// Reminder: bytes have range of [-128, 127].
	// When converting to or from an int a 128 should be added or removed.
	// If there is a bug with color then it's probably caused by this.
	
	public final static int EMPTY_DATA = 0;
	public final static int MAX_WORLD_Y_SIZE = 4096;
	
	public final static int ALPHA_DOWNSIZE_SHIFT = 4;
	

	public final static int GEN_TYPE_SHIFT = 60;

	public final static int COLOR_SHIFT = 32;
	public final static int BLUE_SHIFT = COLOR_SHIFT;
	public final static int GREEN_SHIFT = BLUE_SHIFT + 8;
	public final static int RED_SHIFT = GREEN_SHIFT + 8;
	public final static int ALPHA_SHIFT = RED_SHIFT + 8;
	
	public final static int HEIGHT_SHIFT = 20;
	public final static int DEPTH_SHIFT = 8;
	public final static int BLOCK_LIGHT_SHIFT = 4;
	public final static int SKY_LIGHT_SHIFT = 0;
	
	public final static long ALPHA_MASK = 0xF;
	public final static long RED_MASK = 0xFF;
	public final static long GREEN_MASK = 0xFF;
	public final static long BLUE_MASK = 0xFF;
	public final static long COLOR_MASK = 0xFFFFFF;
	public final static long HEIGHT_MASK = 0xFFF;
	public final static long DEPTH_MASK = 0xFFF;
	public final static long HEIGHT_DEPTH_MASK = 0xFFFFFF;
	public final static long BLOCK_LIGHT_MASK = 0xF;
	public final static long SKY_LIGHT_MASK = 0xF;
	public final static long GEN_TYPE_MASK = 0b111;
	public final static long COMPARE_SHIFT = GEN_TYPE_SHIFT;
	
	public final static long HEIGHT_SHIFTED_MASK = HEIGHT_MASK << HEIGHT_SHIFT;
	public final static long DEPTH_SHIFTED_MASK = DEPTH_MASK << DEPTH_SHIFT;
	
	public final static long VOID_SETTER = HEIGHT_SHIFTED_MASK | DEPTH_SHIFTED_MASK;
	
	
	public static long createVoidDataPoint(byte generationMode)
	{
		if (generationMode == 0)
			throw new IllegalArgumentException("Trying to create void datapoint with genMode 0, which is NOT allowed in DataPoint version 10!");
		return (generationMode & GEN_TYPE_MASK) << GEN_TYPE_SHIFT;
	}
	
	public static long createDataPoint(int height, int depth, int color, int lightSky, int lightBlock, int generationMode)
	{
		return createDataPoint(
				ColorUtil.getAlpha(color),
				ColorUtil.getRed(color),
				ColorUtil.getGreen(color),
				ColorUtil.getBlue(color),
				height, depth, lightSky, lightBlock, generationMode);
	}

	public static long createDataPoint(int height, int depth, int color, byte light, int generationMode)
	{
		return createDataPoint(
				ColorUtil.getAlpha(color),
				ColorUtil.getRed(color),
				ColorUtil.getGreen(color),
				ColorUtil.getBlue(color),
				height, depth, light%16, light/16, generationMode);
	}
	
	public static long createDataPoint(int alpha, int red, int green, int blue, int height, int depth, int lightSky, int lightBlock, int generationMode)
	{
		if (generationMode == 0)
			throw new IllegalArgumentException("Trying to create datapoint with genMode 0, which is NOT allowed in DataPoint version 10!");
		if (height < 0 || height > 4096)
			throw new IllegalArgumentException("Height must be between 0 and 4096!");
		if (depth < 0 || depth > 4096)
			throw new IllegalArgumentException("Depth must be between 0 and 4096!");
		if (lightSky < 0 || lightSky > 15)
			throw new IllegalArgumentException("Sky light must be between 0 and 15!");
		if (lightBlock < 0 || lightBlock > 15)
			throw new IllegalArgumentException("Block light must be between 0 and 15!");
		if (alpha < 0 || alpha > 255)
			throw new IllegalArgumentException("Alpha must be between 0 and 255!");
		if (red < 0 || red > 255)
			throw new IllegalArgumentException("Red must be between 0 and 255!");
		if (green < 0 || green > 255)
			throw new IllegalArgumentException("Green must be between 0 and 255!");
		if (blue < 0 || blue > 255)
			throw new IllegalArgumentException("Blue must be between 0 and 255!");
		if (generationMode < 0 || generationMode > 7)
			throw new IllegalArgumentException("Generation mode must be between 0 and 7!");
		if (depth > height)
			throw new IllegalArgumentException("Depth must be less than or equal to height!");

		return (long) (alpha >>> ALPHA_DOWNSIZE_SHIFT) << ALPHA_SHIFT
			| (red & RED_MASK) << RED_SHIFT
			| (green & GREEN_MASK) << GREEN_SHIFT
			| (blue & BLUE_MASK) << BLUE_SHIFT
			| (height & HEIGHT_MASK) << HEIGHT_SHIFT
			| (depth & DEPTH_MASK) << DEPTH_SHIFT
			| (lightBlock & BLOCK_LIGHT_MASK) << BLOCK_LIGHT_SHIFT
			| (lightSky & SKY_LIGHT_MASK) << SKY_LIGHT_SHIFT
			| (generationMode & GEN_TYPE_MASK) << GEN_TYPE_SHIFT
			;
	}
	
	public static long shiftHeightAndDepth(long dataPoint, short offset) {
		long height = (dataPoint + ((long) offset << HEIGHT_SHIFT)) & HEIGHT_SHIFTED_MASK;
		long depth = (dataPoint + (offset << DEPTH_SHIFT)) & DEPTH_SHIFTED_MASK;
		return dataPoint & ~(HEIGHT_SHIFTED_MASK | DEPTH_SHIFTED_MASK) | height | depth;
	}
	
	public static long version9Reorder(long dataPoint) {
		/*
		|a  |a  |a  |a  |r  |r  |r  |r   |
		|r  |r  |r  |r  |g  |g  |g  |g  |
		|g  |g  |g  |g  |b  |b  |b  |b  |
		|b  |b  |b  |b  |h  |h  |h  |h  |
		|h  |h  |h  |h  |h  |h  |d  |d  |
		|d  |d  |d  |d  |d  |d  |d  |d  |
		|bl |bl |bl |bl |sl |sl |sl |sl |
		|l  |l  |f  |g  |g  |g  |v  |e  |
		*/
		if ((dataPoint & 1) == 0) return 0;
		
		long height = (dataPoint >>> 26) & 0x3FF;
		long depth = (dataPoint >>> 16) & 0x3FF;
		if (height == depth || (dataPoint & 0b10)==0b10) {
			return createVoidDataPoint((byte) (((dataPoint >>> 2) & 0b111) + 1));
		}
		return ((dataPoint >>> 60) & 0xF) << ALPHA_SHIFT
				| ((dataPoint >>> 52) & 0xFF) << RED_SHIFT
				| ((dataPoint >>> 44) & 0xFF) << GREEN_SHIFT
				| ((dataPoint >>> 36) & 0xFF) << BLUE_SHIFT
				| ((dataPoint >>> 26) & 0x3FF) << HEIGHT_SHIFT
				| ((dataPoint >>> 16) & 0x3FF) << DEPTH_SHIFT
				| ((dataPoint >>> 8) & 0xFF) << SKY_LIGHT_SHIFT
				| (((dataPoint >>> 2) & 0xFF) + 1) << GEN_TYPE_SHIFT;
	}
	
	public static short getHeight(long dataPoint)
	{
		return (short) ((dataPoint >>> HEIGHT_SHIFT) & HEIGHT_MASK);
	}
	
	public static short getDepth(long dataPoint)
	{
		return (short) ((dataPoint >>> DEPTH_SHIFT) & DEPTH_MASK);
	}
	
	public static short getAlpha(long dataPoint)
	{
		return (short) ((((dataPoint >>> ALPHA_SHIFT) & ALPHA_MASK) << ALPHA_DOWNSIZE_SHIFT) | 0b1111);
	}
	
	public static short getRed(long dataPoint)
	{
		return (short) ((dataPoint >>> RED_SHIFT) & RED_MASK);
	}
	
	public static short getGreen(long dataPoint)
	{
		return (short) ((dataPoint >>> GREEN_SHIFT) & GREEN_MASK);
	}
	
	public static short getBlue(long dataPoint)
	{
		return (short) ((dataPoint >>> BLUE_SHIFT) & BLUE_MASK);
	}
	
	public static byte getLightSky(long dataPoint)
	{
		return (byte) ((dataPoint >>> SKY_LIGHT_SHIFT) & SKY_LIGHT_MASK);
	}
	
	public static byte getLightBlock(long dataPoint)
	{
		return (byte) ((dataPoint >>> BLOCK_LIGHT_SHIFT) & BLOCK_LIGHT_MASK);
	}

	private static final SpamReducedLogger warnLogger = new SpamReducedLogger(1);
	
	public static byte getGenerationMode(long dataPoint)
	{
		byte genMode = (byte) ((dataPoint >>> GEN_TYPE_SHIFT) & GEN_TYPE_MASK);
		if (warnLogger.canMaybeLog() && doesItExist(dataPoint) && genMode==0) {
			warnLogger.warnInc("Existing datapoint with genMode 0 detected! This is invalid in DataPoint version 10!"
					+ " This may be caused by old data that has not been updated correctly.");
			return 1;
		}
		return genMode == 0 ? 1 : genMode;
	}

	public static boolean isVoid(long dataPoint)
	{
		return (((dataPoint >>> DEPTH_SHIFT) & HEIGHT_DEPTH_MASK) == 0);
	}
	
	public static boolean doesItExist(long dataPoint)
	{
		return dataPoint!=0;
	}
	
	public static int getColor(long dataPoint)
	{
		long alpha = getAlpha(dataPoint);
		return (int) (((dataPoint >>> COLOR_SHIFT) & COLOR_MASK) | (alpha << (ALPHA_SHIFT-COLOR_SHIFT)));
	}
	
	/** This is used to convert a dataPoint to string (useful for the print function) */
	@SuppressWarnings("unused")
	public static String toString(long dataPoint)
	{
		return getHeight(dataPoint) + " " +
				getDepth(dataPoint) + " " +
				getAlpha(dataPoint) + " " +
				getRed(dataPoint) + " " +
				getBlue(dataPoint) + " " +
				getGreen(dataPoint) + " " +
				getLightBlock(dataPoint) + " " +
				getLightSky(dataPoint) + " " +
				getGenerationMode(dataPoint) + " " +
				isVoid(dataPoint) + " " +
				doesItExist(dataPoint) + '\n';
	}


	private static void shrinkArray(short[] array, int packetSize, int start, int length, int arraySize)
	{
		start *= packetSize;
		length *= packetSize;
		arraySize *= packetSize;
		//remove comment to not leave garbage at the end
		//array[start + packetSize + i] = 0;
		if (arraySize - start >= 0) System.arraycopy(array, start + length, array, start, arraySize - start);
	}
	private static void extendArray(short[] array, int packetSize, int start, int length, int arraySize)
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


	/** Return (>0) if dataA should replace dataB, (0) if equal, (<0) if dataB should replace dataA */
	public static int compareDatapointPriority(long dataA, long dataB) {
		return (int) ((dataA >> COMPARE_SHIFT) - (dataB >> COMPARE_SHIFT));
	}
	private static final ThreadLocal<short[]> tLocalHeightAndDepth = new ThreadLocal<short[]>();
	private static final ThreadLocal<int[]> tDataIndexCache = new ThreadLocal<int[]>();
	/**
	 * This method merge column of multiple data together
	 * @param sourceData one or more columns of data
	 * @param output one column of space for the result to be written to
	 */
	public static void mergeMultiData(IColumnDataView sourceData, ColumnArrayView output)
	{
		if (output.dataCount() != 1) throw new IllegalArgumentException("output must be only reserved for one datapoint!");
		int inputVerticalSize = sourceData.verticalSize();
		int outputVerticalSize = output.verticalSize();
		output.fill(0);

		//dataCount indicate how many position we are merging in one position
		int dataCount = sourceData.dataCount();

		// We initialize the arrays that are going to be used
		int heightAndDepthLength = (MAX_WORLD_Y_SIZE / 2 + 16) * 2;
		short[] heightAndDepth = tLocalHeightAndDepth.get();
		if (heightAndDepth==null || heightAndDepth.length != heightAndDepthLength) {
			heightAndDepth = new short[heightAndDepthLength];
			tLocalHeightAndDepth.set(heightAndDepth);
		}

		byte genMode = getGenerationMode(sourceData.get(0));
		if (genMode == 0) genMode = 1; // FIXME: Hack to make the version 10 genMode never be 0.
		boolean allEmpty = true;
		boolean allVoid = true;
		boolean limited = false;
		boolean allDefault;
		long singleData;

		short depth;
		short height;
		int count = 0;
		int i;
		int ii;
		int dataIndex;

		//We collect the indexes of the data, ordered by the depth
		for (int index = 0; index < dataCount; index++)
		{
			if (index == 0)
			{
				for (dataIndex = 0; dataIndex < inputVerticalSize; dataIndex++)
				{
					singleData = sourceData.get(dataIndex);
					if (doesItExist(singleData))
					{
						//genMode = Math.min(genMode, getGenerationMode(singleData));
						allEmpty = false;
						if (!isVoid(singleData))
						{
							allVoid = false;
							count++;
							heightAndDepth[dataIndex * 2] = getHeight(singleData);
							heightAndDepth[dataIndex * 2 +1] = getDepth(singleData);
						}
					}
					else
						break;
				}
			}
			else
			{
				for (dataIndex = 0; dataIndex < inputVerticalSize; dataIndex++)
				{
					singleData = sourceData.get(index * inputVerticalSize + dataIndex);
					if (doesItExist(singleData))
					{
						//genMode = Math.min(genMode, getGenerationMode(singleData));
						allEmpty = false;
						if (!isVoid(singleData))
						{
							allVoid = false;
							depth = getDepth(singleData);
							height = getHeight(singleData);

							int botPos = -1;
							int topPos = -1;
							//values fall in between and possibly require extension of array
							boolean botExtend = false;
							boolean topExtend = false;
							for (i = 0; i < count; i++)
							{
								if (depth < heightAndDepth[i * 2] && depth >= heightAndDepth[i * 2 + 1])
								{
									botPos = i;
									break;
								}
								else if (depth < heightAndDepth[i * 2 + 1] && ((i + 1 < count && depth >= heightAndDepth[(i + 1) * 2]) || i + 1 == count))
								{
									botPos = i;
									botExtend = true;
									break;
								}
							}
							for (i = 0; i < count; i++)
							{
								if (height <= heightAndDepth[i * 2] && height > heightAndDepth[i * 2 + 1])
								{
									topPos = i;
									break;
								}
								else if (height <= heightAndDepth[i * 2 + 1] && ((i + 1 < count && height > heightAndDepth[(i + 1) * 2]) || i + 1 == count))
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
		}

		//We check if there is any data that's not empty or void
		if (allEmpty) {
			return;
		}
		if (allVoid)
		{
			output.set(0,createVoidDataPoint(genMode));
			return;
		}

		//we limit the vertical portion to maxVerticalData
		int j = 0;
		while (count > outputVerticalSize)
		{
			limited = true;
			ii = MAX_WORLD_Y_SIZE;
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

		if (!limited && dataCount == 1) // This mean source vertSize < output vertSize AND both dataCount == 1
		{
			output.copyFrom(sourceData);
		}
		else
		{

			//We want to efficiently memorize indexes
			int[] dataIndexesCache = tDataIndexCache.get();
			if (dataIndexesCache==null || dataIndexesCache.length != dataCount) {
				dataIndexesCache = new int[dataCount];
				tDataIndexCache.set(dataIndexesCache);
			}
			Arrays.fill(dataIndexesCache,0);

			//For each lod height-depth value we have found we now want to generate the rest of the data
			//by merging all lods at lower level that are contained inside the new ones
			for (j = 0; j < count; j++)
			{
				//We firstly collect height and depth data
				//this will be added to each realtive long DataPoint
				height = heightAndDepth[j * 2];
				depth = heightAndDepth[j * 2 + 1];

				//if both height and depth are at 0 then we finished
				if ((depth == 0 && height == 0) || j >= heightAndDepth.length / 2)
					break;

				//We initialize data useful for the merge
				int numberOfChildren = 0;
				allEmpty = true;
				allVoid = true;

				//We initialize all the new values that we are going to put in the dataPoint
				int tempAlpha = 0;
				int tempRed = 0;
				int tempGreen = 0;
				int tempBlue = 0;
				int tempLightBlock = 0;
				int tempLightSky = 0;
				long data = 0;

				//For each position that we want to merge
				for (int index = 0; index < dataCount; index++)
				{
					//we scan the lods in the position from top to bottom
					while(dataIndexesCache[index] < inputVerticalSize)
					{
						singleData = sourceData.get(index * inputVerticalSize + dataIndexesCache[index]);
						if (doesItExist(singleData) && !isVoid(singleData))
						{
							dataIndexesCache[index]++;
							if ((depth <= getDepth(singleData) && getDepth(singleData) < height)
									|| (depth < getHeight(singleData) && getHeight(singleData) <= height))
							{
								data = singleData;
								break;
							}
						}
						else
							break;
					}
					if (!doesItExist(data))
					{
						data = createVoidDataPoint(genMode);
					}

					if (doesItExist(data))
					{
						allEmpty = false;
						if (!isVoid(data))
						{
							numberOfChildren++;
							allVoid = false;
							tempAlpha = Math.max(getAlpha(data),tempAlpha);
							tempRed += getRed(data) * getRed(data);
							tempGreen += getGreen(data) * getGreen(data);
							tempBlue += getBlue(data) * getBlue(data);
							tempLightBlock += getLightBlock(data);
							tempLightSky += getLightSky(data);
						}
					}
				}

				if (allEmpty)
					//no child has been initialized
					output.set(j, EMPTY_DATA);
				else if (allVoid)
					//all the children are void
					output.set(j, createVoidDataPoint(genMode));
				else
				{
					//we have at least 1 child
					if (dataCount != 1)
					{
						tempRed = tempRed / numberOfChildren;
						tempGreen = tempGreen / numberOfChildren;
						tempBlue = tempBlue / numberOfChildren;
						tempLightBlock = tempLightBlock / numberOfChildren;
						tempLightSky = tempLightSky / numberOfChildren;
					}
					//data = createDataPoint(tempAlpha, tempRed, tempGreen, tempBlue, height, depth, tempLightSky, tempLightBlock, tempGenMode, allDefault);
					//if (j > 0 && getColor(data) == getColor(dataPoint[j]))
					//{
					//	add simplification at the end due to color
					//}
					output.set(j, createDataPoint((int) Math.sqrt(tempAlpha), (int) Math.sqrt(tempRed), (int) Math.sqrt(tempGreen), (int) Math.sqrt(tempBlue), height, depth, tempLightSky, tempLightBlock, genMode));
				}
			}
		}
	}
}