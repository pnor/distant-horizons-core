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
import com.seibel.lod.core.objects.lod.DataPoint;

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
	public final static DataPoint EMPTY_DATA = new DataPoint();
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
	
	
	public static DataPoint createVoidDataPoint(int generationMode)
	{
		byte flags = (byte) ((generationMode & GEN_TYPE_MASK) << GEN_TYPE_SHIFT);
		flags += VOID_MASK << VOID_SHIFT;
		flags += EXISTENCE_MASK << EXISTENCE_SHIFT;
		return new DataPoint(0, 0, flags);
	}
	
	public static DataPoint createDataPoint(int height, int depth, int color, int lightSky, int lightBlock, int generationMode, boolean flag)
	{
		int data = (height & HEIGHT_MASK) << HEIGHT_SHIFT;
		data += (depth & DEPTH_MASK) << DEPTH_SHIFT;
		data += (lightBlock & BLOCK_LIGHT_MASK) << BLOCK_LIGHT_SHIFT;
		data += (lightSky & SKY_LIGHT_MASK) << SKY_LIGHT_SHIFT;
		byte flags = (byte) ((generationMode & GEN_TYPE_MASK) << GEN_TYPE_SHIFT);
		if (flag) flags += FLAG_MASK << FLAG_SHIFT;
		flags += EXISTENCE_MASK << EXISTENCE_SHIFT;
		return new DataPoint(color, data, flags);
	}
	
	public static DataPoint createDataPoint(int alpha, int red, int green, int blue, int height, int depth, int lightSky, int lightBlock, int generationMode, boolean flag)
	{
		return createDataPoint(
				height, depth,
				(alpha << ALPHA_SHIFT) | (red << RED_SHIFT) | (green << GREEN_SHIFT) | blue,
				lightSky, lightBlock, generationMode, flag);
	}
	
	public static short getHeight(DataPoint dataPoint)
	{
		return (short) ((dataPoint.data >>> HEIGHT_SHIFT) & HEIGHT_MASK);
	}
	
	public static short getDepth(DataPoint dataPoint)
	{
		return (short) ((dataPoint.data >>> DEPTH_SHIFT) & DEPTH_MASK);
	}
	
	public static short getAlpha(DataPoint dataPoint)
	{
		return (short) ((dataPoint.color >>> ALPHA_SHIFT) & ALPHA_MASK);
	}
	
	public static short getRed(DataPoint dataPoint)
	{
		return (short) ((dataPoint.color >>> RED_SHIFT) & RED_MASK);
	}
	
	public static short getGreen(DataPoint dataPoint)
	{
		return (short) ((dataPoint.color >>> GREEN_SHIFT) & GREEN_MASK);
	}
	
	public static short getBlue(DataPoint dataPoint)
	{
		return (short) ((dataPoint.color >>> BLUE_SHIFT) & BLUE_MASK);
	}
	
	public static byte getLightSky(DataPoint dataPoint)
	{
		return (byte) ((dataPoint.data >>> SKY_LIGHT_SHIFT) & SKY_LIGHT_MASK);
	}
	
	public static byte getLightSkyAlt(DataPoint dataPoint)
	{
		if (skyLightPlayer == 0 && ((dataPoint.flags >>> FLAG_SHIFT) & FLAG_MASK) == 1)
			return 0;
		else
			return (byte) ((dataPoint.data >>> SKY_LIGHT_SHIFT) & SKY_LIGHT_MASK);
	}
	
	public static byte getLightBlock(DataPoint dataPoint)
	{
		return (byte) ((dataPoint.data >>> BLOCK_LIGHT_SHIFT) & BLOCK_LIGHT_MASK);
	}
	
	public static boolean getFlag(DataPoint dataPoint)
	{
		return ((dataPoint.flags >>> FLAG_SHIFT) & FLAG_MASK) == 1;
	}
	
	public static byte getGenerationMode(DataPoint dataPoint)
	{
		return (byte) ((dataPoint.flags >>> GEN_TYPE_SHIFT) & GEN_TYPE_MASK);
	}
	
	public static boolean isVoid(DataPoint dataPoint)
	{
		return (((dataPoint.flags >>> VOID_SHIFT) & VOID_MASK) == 1);
	}
	
	public static boolean doesItExist(DataPoint dataPoint)
	{
		return (dataPoint != null) && (((dataPoint.flags >>> EXISTENCE_SHIFT) & EXISTENCE_MASK) == 1);
	}
	
	public static int getColor(DataPoint dataPoint)
	{
		return dataPoint.color;
	}
	
	/** This is used to convert a dataPoint to string (useful for the print function) */
	@SuppressWarnings("unused")
	public static String toString(DataPoint dataPoint)
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
	 * @return one column of correctly parsed data
	 */
	public static DataPoint[] mergeMultiData(DataPoint[] dataToMerge, int inputVerticalData, int maxVerticalData)
	{
		int size = dataToMerge.length / inputVerticalData;
		
		// We initialize the arrays that are going to be used
		short[] heightAndDepth = ThreadMapUtil.getHeightAndDepth((WORLD_HEIGHT / 2 + 1) * 2);
		DataPoint[] dataPoint = ThreadMapUtil.getVerticalDataArray(DetailDistanceUtil.getMaxVerticalData(0));
		
		
		int genMode = DistanceGenerationMode.FULL.complexity;
		boolean allEmpty = true;
		boolean allVoid = true;
		boolean allDefault;
		DataPoint singleData;
		
		
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
				if (singleData != null && doesItExist(singleData))
				{
					genMode = Math.min(genMode, getGenerationMode(singleData));
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
			return dataPoint;
		if (allVoid)
		{
			dataPoint[0] = createVoidDataPoint(genMode);
			return dataPoint;
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
			DataPoint data = EMPTY_DATA;
			
			for (int index = 0; index < size; index++)
			{
				for (dataIndex = 0; dataIndex < inputVerticalData; dataIndex++)
				{
					singleData = dataToMerge[index * inputVerticalData + dataIndex];
					if (doesItExist(singleData) && !isVoid(singleData))
					{
						if ((depth <= getDepth(singleData) && getDepth(singleData) <= height)
								|| (depth <= getHeight(singleData) && getHeight(singleData) <= height))
						{
							if (getHeight(singleData) > getHeight(data))
								data = singleData;
						}
					}
					else
						break;
				}
				if (!doesItExist(data))
				{
					singleData = dataToMerge[index * inputVerticalData];
					if (doesItExist(singleData))
						data = createVoidDataPoint(getGenerationMode(singleData));
					else
						data = createVoidDataPoint(0);
				}
				
				if (doesItExist(data))
				{
					allEmpty = false;
					if (!isVoid(data))
					{
						numberOfChildren++;
						allVoid = false;
						tempAlpha += getAlpha(data);
						tempRed += getRed(data);
						tempGreen += getGreen(data);
						tempBlue += getBlue(data);
						tempLightBlock += getLightBlock(data);
						tempLightSky += getLightSky(data);
						if (!getFlag(data)) allDefault = false;
					}
					tempGenMode = (byte) Math.min(tempGenMode, getGenerationMode(data));
				}
				else
					tempGenMode = (byte) Math.min(tempGenMode, DistanceGenerationMode.NONE.complexity);
			}
			
			if (allEmpty)
				//no child has been initialized
				dataPoint[j] = EMPTY_DATA;
			else if (allVoid)
				//all the children are void
				dataPoint[j] = createVoidDataPoint(tempGenMode);
			else
			{
				//we have at least 1 child
				tempAlpha = tempAlpha / numberOfChildren;
				tempRed = tempRed / numberOfChildren;
				tempGreen = tempGreen / numberOfChildren;
				tempBlue = tempBlue / numberOfChildren;
				tempLightBlock = tempLightBlock / numberOfChildren;
				tempLightSky = tempLightSky / numberOfChildren;
				dataPoint[j] = createDataPoint(tempAlpha, tempRed, tempGreen, tempBlue, height, depth, tempLightSky, tempLightBlock, tempGenMode, allDefault);
			}
		}
		return dataPoint;
	}
}