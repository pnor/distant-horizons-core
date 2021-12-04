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

import static com.seibel.lod.core.builders.bufferBuilding.LodBufferBuilderFactory.skyLightPlayer;

import com.seibel.lod.core.enums.config.DistanceGenerationMode;

/**
 * 
 * @author Leonardo Amato
 * @version ??
 */
public class DataPointUtil
{
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
	
	// Reminder: bytes have range of [-128, 127].
	// When converting to or from an int a 128 should be added or removed.
	// If there is a bug with color then it's probably caused by this.
	
	//To be used in the future for negative value
	//public final static int MIN_DEPTH = -64;
	//public final static int MIN_HEIGHT = -64;
	public final static byte EMPTY_DATA = 0;
	public static final short VERTICAL_OFFSET = -2048;
	public static int WORLD_HEIGHT = 4096;
	
	public final static int ALPHA_DOWNSIZE_SHIFT = 4;
	
	//public final static int BLUE_COLOR_SHIFT = 0;
	//public final static int GREEN_COLOR_SHIFT = 8;
	//public final static int RED_COLOR_SHIFT = 16;
	//public final static int ALPHA_COLOR_SHIFT = 24;
	
	public final static byte BLUE_SHIFT = 0;
	public final static byte GREEN_SHIFT = 8;
	public final static byte RED_SHIFT = 16;
	public final static byte ALPHA_SHIFT = 24;
	
	//public final static byte COLOR_SHIFT = 36;
	
	public final static byte HEIGHT_SHIFT = 20;
	public final static byte DEPTH_SHIFT = 8;
	public final static byte BLOCK_LIGHT_SHIFT = 4;
	public final static byte SKY_LIGHT_SHIFT = 0;
	//public final static byte LIGHTS_SHIFT = SKY_LIGHT_SHIFT;
	//public final static byte VERTICAL_INDEX_SHIFT = 6;
	public final static byte FLAG_SHIFT = 5;
	public final static byte GEN_TYPE_SHIFT = 2;
	public final static byte VOID_SHIFT = 1;
	public final static byte EXISTENCE_SHIFT = 0;
	
	public final static int ALPHA_MASK = 0xFF;
	public final static int RED_MASK = 0xFF;
	public final static int GREEN_MASK = 0xFF;
	public final static int BLUE_MASK = 0xFF;
	public final static int COLOR_MASK = 0xFFFFFFFF;
	public final static int HEIGHT_MASK = 0xFFF;
	public final static int DEPTH_MASK = 0xFFF;
	public final static int LIGHTS_MASK = 0xFF;
	public final static int BLOCK_LIGHT_MASK = 0xF;
	public final static int SKY_LIGHT_MASK = 0xF;
	public final static int VERTICAL_INDEX_MASK = 0x3;
	public final static byte FLAG_MASK = 0x1;
	public final static byte GEN_TYPE_MASK = 0x7;
	public final static byte VOID_MASK = 1;
	public final static byte EXISTENCE_MASK = 1;
	
	/** Returns the Flags byte */
	public static byte createVoidDataPoint(byte generationMode)
	{
		generationMode = (byte) ((generationMode & GEN_TYPE_MASK) << GEN_TYPE_SHIFT);
		generationMode |= VOID_MASK << VOID_SHIFT;
		generationMode |= EXISTENCE_MASK << EXISTENCE_SHIFT;
		return generationMode;
	}
	
	/** Returned datapoint is in ThreadMapUtil */
	public static void createDataPoint(int height, int depth, int color, int lightSky, int lightBlock, int generationMode, boolean flag)
	{
		int data = (height & HEIGHT_MASK) << HEIGHT_SHIFT;
		data += (depth & DEPTH_MASK) << DEPTH_SHIFT;
		data += (lightBlock & BLOCK_LIGHT_MASK) << BLOCK_LIGHT_SHIFT;
		data += (lightSky & SKY_LIGHT_MASK) << SKY_LIGHT_SHIFT;
		byte flags = (byte) ((generationMode & GEN_TYPE_MASK) << GEN_TYPE_SHIFT);
		if (flag) flags += FLAG_MASK << FLAG_SHIFT;
		flags += EXISTENCE_MASK << EXISTENCE_SHIFT;
		ThreadMapUtil.saveDataPoint(color, data, flags);
	}
	
	public static void createDataPoint(int alpha, int red, int green, int blue, int height, int depth, int lightSky, int lightBlock, int generationMode, boolean flag)
	{
		createDataPoint(
				height, depth,
				(alpha << ALPHA_SHIFT) | (red << RED_SHIFT) | (green << GREEN_SHIFT) | blue,
				lightSky, lightBlock, generationMode, flag);
	}
	
	public static short getHeight(int data)
	{
		return (short) ((data >>> HEIGHT_SHIFT) & HEIGHT_MASK);
	}
	
	public static short getDepth(int data)
	{
		return (short) ((data >>> DEPTH_SHIFT) & DEPTH_MASK);
	}
	
	public static short getAlpha(int color)
	{
		return (short) ((color >>> ALPHA_SHIFT) & ALPHA_MASK);
	}
	
	public static short getRed(int color)
	{
		return (short) ((color >>> RED_SHIFT) & RED_MASK);
	}
	
	public static short getGreen(int color)
	{
		return (short) ((color >>> GREEN_SHIFT) & GREEN_MASK);
	}
	
	public static short getBlue(int color)
	{
		return (short) ((color >>> BLUE_SHIFT) & BLUE_MASK);
	}
	
	public static byte getLightSky(int data)
	{
		return (byte) ((data >>> SKY_LIGHT_SHIFT) & SKY_LIGHT_MASK);
	}
	
	public static byte getLightSkyAlt(int data, byte flags)
	{
		if (skyLightPlayer == 0 && ((flags >>> FLAG_SHIFT) & FLAG_MASK) == 1)
			return 0;
		else
			return (byte) ((data >>> SKY_LIGHT_SHIFT) & SKY_LIGHT_MASK);
	}
	
	public static byte getLightBlock(int data)
	{
		return (byte) ((data >>> BLOCK_LIGHT_SHIFT) & BLOCK_LIGHT_MASK);
	}
	
	public static boolean getFlag(byte flags)
	{
		return ((flags >>> FLAG_SHIFT) & FLAG_MASK) == 1;
	}
	
	public static byte getGenerationMode(byte flags)
	{
		return (byte) ((flags >>> GEN_TYPE_SHIFT) & GEN_TYPE_MASK);
	}
	
	public static boolean isVoid(byte flags)
	{
		return (((flags >>> VOID_SHIFT) & VOID_MASK) == 1);
	}
	
	public static boolean doesItExist(byte flags)
	{
		return ((flags >>> EXISTENCE_SHIFT) & EXISTENCE_MASK) == 1;
	}
	
	@Deprecated
	public static int getColor(int color)
	{
		return color;
	}
	
	/** This is used to convert a dataPoint to string (useful for the print function) */
	@SuppressWarnings("unused")
	public static String toString(int color, int data, byte flags)
	{
		return getHeight(data) + " " +
				getDepth(data) + " " +
				getAlpha(color) + " " +
				getRed(color) + " " +
				getBlue(color) + " " +
				getGreen(color) + " " +
				getLightBlock(data) + " " +
				getLightSky(data) + " " +
				getGenerationMode(flags) + " " +
				isVoid(flags) + " " +
				doesItExist(flags) + '\n';
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
	 * Returned datapoint is in ThreadMapUtil
	 * @param dataToMergeColor colors of one or more columns of data
	 * @param dataToMergeData data of one or more columns of data
	 * @param dataToMergeFlags flags of one or more columns of data
	 * @param inputVerticalData vertical size of an input data
	 * @param maxVerticalData max vertical size of the merged data
	 */
	public static void mergeMultiData(int[] dataToMergeColor, int[] dataToMergeData, byte[] dataToMergeFlags, int inputVerticalData, int maxVerticalData)
	{
		int size = dataToMergeData.length / inputVerticalData;
		
		// We initialize the arrays that are going to be used
		short[] heightAndDepth = ThreadMapUtil.getHeightAndDepth((WORLD_HEIGHT / 2 + 1) * 2);
		int[] dataPointColor = ThreadMapUtil.getVerticalDataArrayColor(DetailDistanceUtil.getMaxVerticalData(0));
		int[] dataPointData = ThreadMapUtil.getVerticalDataArrayData(DetailDistanceUtil.getMaxVerticalData(0));
		byte[] dataPointFlags = ThreadMapUtil.getVerticalDataArrayFlags(DetailDistanceUtil.getMaxVerticalData(0));
		
		
		byte genMode = DistanceGenerationMode.FULL.complexity;
		boolean allEmpty = true;
		boolean allVoid = true;
		boolean allDefault;
		int singleDataData;
		byte singleDataFlags;
		
		
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
				singleDataData = dataToMergeData[index * inputVerticalData + dataIndex];
				singleDataFlags = dataToMergeFlags[index * inputVerticalData + dataIndex];
				if (doesItExist(singleDataFlags))
				{
					genMode = (byte) Math.min(genMode, getGenerationMode(singleDataFlags));
					allEmpty = false;
					if (!isVoid(singleDataFlags))
					{
						allVoid = false;
						depth = getDepth(singleDataData);
						height = getHeight(singleDataData);
						
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
			dataPointFlags[0] = createVoidDataPoint(genMode);
			return;
		}
		
		//we limit the vertical portion to maxVerticalData
		int j = 0;
		while (count > maxVerticalData)
		{
			ii = WORLD_HEIGHT - VERTICAL_OFFSET;
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
			int singleDataColor;
			int data = EMPTY_DATA;
			int color = EMPTY_DATA;
			byte flags = EMPTY_DATA;
			
			for (int index = 0; index < size; index++)
			{
				for (dataIndex = 0; dataIndex < inputVerticalData; dataIndex++)
				{
					singleDataColor = dataToMergeColor[index * inputVerticalData + dataIndex];
					singleDataData = dataToMergeData[index * inputVerticalData + dataIndex];
					singleDataFlags = dataToMergeFlags[index * inputVerticalData + dataIndex];
					if (doesItExist(singleDataFlags) && !isVoid(singleDataFlags))
					{
						if ((depth <= getDepth(singleDataData) && getDepth(singleDataData) <= height)
								|| (depth <= getHeight(singleDataData) && getHeight(singleDataData) <= height))
						{
							if (getHeight(singleDataData) > getHeight(data))
							{
								color = singleDataColor;
								data = singleDataData;
								flags = singleDataFlags;
							}
						}
					}
					else
						break;
				}
				if (!doesItExist(flags))
				{
					singleDataFlags = dataToMergeFlags[index * inputVerticalData];
					if (doesItExist(singleDataFlags))
						flags = createVoidDataPoint(getGenerationMode(singleDataFlags));
					else
						flags = createVoidDataPoint((byte) 0);
					data = EMPTY_DATA;
					color = EMPTY_DATA;
				}
				
				if (doesItExist(flags))
				{
					allEmpty = false;
					if (!isVoid(flags))
					{
						numberOfChildren++;
						allVoid = false;
						tempAlpha += getAlpha(color);
						tempRed += getRed(color);
						tempGreen += getGreen(color);
						tempBlue += getBlue(color);
						tempLightBlock += getLightBlock(data);
						tempLightSky += getLightSky(data);
						if (!getFlag(flags))
							allDefault = false;
					}
					tempGenMode = (byte) Math.min(tempGenMode, getGenerationMode(flags));
				}
				else
					tempGenMode = (byte) Math.min(tempGenMode, DistanceGenerationMode.NONE.complexity);
			}
			
			if (!allEmpty)
			{
				//child has been initialized
				if (allVoid)
				{
					//all the children are void
					dataPointFlags[j] = createVoidDataPoint(tempGenMode);
				}
				else
				{
					//we have at least 1 child
					tempAlpha = tempAlpha / numberOfChildren;
					tempRed = tempRed / numberOfChildren;
					tempGreen = tempGreen / numberOfChildren;
					tempBlue = tempBlue / numberOfChildren;
					tempLightBlock = tempLightBlock / numberOfChildren;
					tempLightSky = tempLightSky / numberOfChildren;
					createDataPoint(tempAlpha, tempRed, tempGreen, tempBlue, height, depth, tempLightSky, tempLightBlock, tempGenMode, allDefault);
					dataPointColor[j] = ThreadMapUtil.dataPointColor;
					dataPointData[j] = ThreadMapUtil.dataPointData;
					dataPointFlags[j] = ThreadMapUtil.dataPointFlags;
				}
			}
		}
	}
}