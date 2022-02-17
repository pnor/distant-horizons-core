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
	public final static int EMPTY_DATA = 0;
	public static final short VERTICAL_OFFSET = -64;
	public static int WORLD_HEIGHT = 1024;
	
	public final static int ALPHA_DOWNSIZE_SHIFT = 4;
	
	//public final static int BLUE_COLOR_SHIFT = 0;
	//public final static int GREEN_COLOR_SHIFT = 8;
	//public final static int RED_COLOR_SHIFT = 16;
	//public final static int ALPHA_COLOR_SHIFT = 24;
	
	public final static int BLUE_SHIFT = 36;
	public final static int GREEN_SHIFT = BLUE_SHIFT + 8;
	public final static int RED_SHIFT = BLUE_SHIFT + 16;
	public final static int ALPHA_SHIFT = BLUE_SHIFT + 24;
	
	public final static int COLOR_SHIFT = 36;
	
	public final static int HEIGHT_SHIFT = 26;
	public final static int DEPTH_SHIFT = 16;
	public final static int BLOCK_LIGHT_SHIFT = 12;
	public final static int SKY_LIGHT_SHIFT = 8;
	//public final static int LIGHTS_SHIFT = SKY_LIGHT_SHIFT;
	//public final static int VERTICAL_INDEX_SHIFT = 6;
	public final static int FLAG_SHIFT = 5;
	public final static int GEN_TYPE_SHIFT = 2;
	public final static int VOID_SHIFT = 1;
	public final static int EXISTENCE_SHIFT = 0;
	
	public final static long ALPHA_MASK = 0b1111;
	public final static long RED_MASK = 0b1111_1111;
	public final static long GREEN_MASK = 0b1111_1111;
	public final static long BLUE_MASK = 0b1111_1111;
	public final static long COLOR_MASK = 0b11111111_11111111_11111111;
	public final static long HEIGHT_MASK = 0b11_1111_1111;
	public final static long DEPTH_MASK = 0b11_1111_1111;
	//public final static long LIGHTS_MASK = 0b1111_1111;
	public final static long BLOCK_LIGHT_MASK = 0b1111;
	public final static long SKY_LIGHT_MASK = 0b1111;
	//public final static long VERTICAL_INDEX_MASK = 0b11;
	public final static long FLAG_MASK = 0b1;
	public final static long GEN_TYPE_MASK = 0b111;
	public final static long VOID_MASK = 1;
	public final static long EXISTENCE_MASK = 1;
	
	public final static long HEIGHT_SHIFTED_MASK = HEIGHT_MASK << HEIGHT_SHIFT;
	public final static long DEPTH_SHIFTED_MASK = DEPTH_MASK << DEPTH_SHIFT;
	
	
	public static long createVoidDataPoint(int generationMode)
	{
		long dataPoint = 0;
		dataPoint |= (generationMode & GEN_TYPE_MASK) << GEN_TYPE_SHIFT;
		dataPoint |= VOID_MASK << VOID_SHIFT;
		dataPoint |= EXISTENCE_MASK << EXISTENCE_SHIFT;
		return dataPoint;
	}
	
	public static long createDataPoint(int height, int depth, int color, int lightSky, int lightBlock, int generationMode, boolean flag)
	{
		return createDataPoint(
				ColorUtil.getAlpha(color),
				ColorUtil.getRed(color),
				ColorUtil.getGreen(color),
				ColorUtil.getBlue(color),
				height, depth, lightSky, lightBlock, generationMode, flag);
	}
	
	public static long createDataPoint(int alpha, int red, int green, int blue, int height, int depth, int lightSky, int lightBlock, int generationMode, boolean flag)
	{
		long dataPoint = 0;
		dataPoint |= (long) (alpha >>> ALPHA_DOWNSIZE_SHIFT) << ALPHA_SHIFT;
		dataPoint |= (red & RED_MASK) << RED_SHIFT;
		dataPoint |= (green & GREEN_MASK) << GREEN_SHIFT;
		dataPoint |= (blue & BLUE_MASK) << BLUE_SHIFT;
		dataPoint |= (height & HEIGHT_MASK) << HEIGHT_SHIFT;
		dataPoint |= (depth & DEPTH_MASK) << DEPTH_SHIFT;
		dataPoint |= (lightBlock & BLOCK_LIGHT_MASK) << BLOCK_LIGHT_SHIFT;
		dataPoint |= (lightSky & SKY_LIGHT_MASK) << SKY_LIGHT_SHIFT;
		dataPoint |= (generationMode & GEN_TYPE_MASK) << GEN_TYPE_SHIFT;
		if (flag) dataPoint |= FLAG_MASK << FLAG_SHIFT;
		dataPoint |= EXISTENCE_MASK << EXISTENCE_SHIFT;
		return dataPoint;
	}
	
	public static long shiftHeightAndDepth(long dataPoint, short offset) {
		long height = (dataPoint + (offset << HEIGHT_SHIFT)) & HEIGHT_SHIFTED_MASK;
		long depth =  (dataPoint + (offset << DEPTH_SHIFT)) & DEPTH_SHIFTED_MASK;
		return dataPoint & ~(HEIGHT_SHIFTED_MASK | DEPTH_SHIFTED_MASK) | height | depth;
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
	
	public static boolean getFlag(long dataPoint)
	{
		return ((dataPoint >>> FLAG_SHIFT) & FLAG_MASK) == 1;
	}
	
	public static byte getGenerationMode(long dataPoint)
	{
		return (byte) ((dataPoint >>> GEN_TYPE_SHIFT) & GEN_TYPE_MASK);
	}
	
	
	public static boolean isVoid(long dataPoint)
	{
		return (((dataPoint >>> VOID_SHIFT) & VOID_MASK) == 1);
	}
	
	public static boolean doesItExist(long dataPoint)
	{
		return (((dataPoint >>> EXISTENCE_SHIFT) & EXISTENCE_MASK) == 1);
	}
	
	public static int getColor(long dataPoint)
	{
		// TODO re-add transparency by replacing the color 255 with what is in comment
		return (int) (((dataPoint >>> COLOR_SHIFT) & COLOR_MASK) | ((((dataPoint >>> ALPHA_SHIFT) & ALPHA_MASK) << ALPHA_DOWNSIZE_SHIFT) | 0b1111) << 24);
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
	
	/** Return (>0) if dataA should replace dataB, (0) if equal, (<0) if dataB should replace dataA */
	public static int compareDatapointPriority(long dataA, long dataB) {
		if (!doesItExist(dataA) || !doesItExist(dataB)) {
			if (doesItExist(dataA)) return Integer.MAX_VALUE;
			if (doesItExist(dataB)) return Integer.MIN_VALUE;
		}
		int genA = (int)getGenerationMode(dataA);
		int genB = (int)getGenerationMode(dataB);
		return genA-genB;
	}
	
	// Merge the newData data into the target data by comparing the Datapoint Priority.
	// If target and newData are same priority, it will use the target one.
	// If target and newData size are different, it will copy up to the smallest one.
	// Return whether anything changed
	public static boolean mergeTwoDataArray(long[] target, long[] newData, int verticalDataSize) {
		boolean anyChange = false;
		for (int i=0; i<target.length&&i<newData.length; i+=verticalDataSize) {
			if (compareDatapointPriority(newData[i], target[i])>0) {
				anyChange = true;
				System.arraycopy(newData, i, target, i, verticalDataSize);
			}
		}
		return anyChange;
	}
	
	// Same as above, but with args for buffer offset.
	// Return whether anything changed
	public static boolean mergeTwoDataArray(long[] target, int targetOffset, long[] newData, int newDataOffset, int dataLength, int verticalDataSize, boolean override) {
		if (targetOffset + verticalDataSize*dataLength>target.length)
			throw new ArrayIndexOutOfBoundsException("\"target\" array index out of bounds");
		if (newDataOffset + verticalDataSize*dataLength>newData.length)
			throw new ArrayIndexOutOfBoundsException("\"newData\" array index out of bounds");
		boolean anyChange = false;
		for (int o=0; o<(dataLength*verticalDataSize); o+=verticalDataSize) {
			if (override) {
				if (compareDatapointPriority(newData[o+newDataOffset], target[o+targetOffset])>=0) {
					anyChange = true;
					System.arraycopy(newData, o+newDataOffset, target, o+targetOffset, verticalDataSize);
				}
			} else {
				if (compareDatapointPriority(newData[o+newDataOffset], target[o+targetOffset])>0) {
					anyChange = true;
					System.arraycopy(newData, o+newDataOffset, target, o+targetOffset, verticalDataSize);
				}
			}
		}
		return anyChange;
	}
	
	// Extract a section of data from the 2D data array
	public static long[] extractDataArray(long[] source, int inWidth, int inHeight, int outX, int outY, int outWidth, int outHeight) {
		int dataSetSize = source.length/inWidth/inHeight;
		if (dataSetSize*inWidth*inHeight != source.length)
			throw new ArrayIndexOutOfBoundsException("\"source\" array invalid width and height");
		if (outWidth > inWidth || outX + outWidth > inWidth)
			throw new ArrayIndexOutOfBoundsException("X index out of bounds");
		if (outHeight > inHeight || outY + outHeight > inHeight)
			throw new ArrayIndexOutOfBoundsException("Y index out of bounds");
		long[] out = new long[dataSetSize*outWidth*outHeight];
		for (int x=0; x<outWidth; x++) {
			System.arraycopy(source, ((outX+x)*inHeight+outY)*dataSetSize,
					out, (x*outHeight)*dataSetSize,
					outHeight*dataSetSize);
		}
		return out;
	}
	
	// Extract a section of data from the 2D data array
	// WARN: if sourceVertSize == targetVertSize, it will return the source array!!! Be careful about this!
	public static long[] changeMaxVertSize(long[] source, int sourceVertSize, int targetVertSize) {
		if (source.length%sourceVertSize != 0)
			throw new ArrayIndexOutOfBoundsException("\"source\" array invalid vertical size or length");
		if (sourceVertSize == targetVertSize) return source;
		if (sourceVertSize > targetVertSize) {
			int size = source.length/sourceVertSize;
			long[] dataToMerge = new long[sourceVertSize];
			long[] newData = new long[size * targetVertSize];
			for (int i = 0; i < size; i++)
			{
				System.arraycopy(source, i * sourceVertSize, dataToMerge, 0, sourceVertSize);
				long[] tempBuffer = DataPointUtil.mergeMultiData(dataToMerge, sourceVertSize, targetVertSize);
				System.arraycopy(tempBuffer, 0, newData, i * targetVertSize, targetVertSize);
			}
			return newData;
		} else {
			int size = source.length/sourceVertSize;
			long[] newData = new long[size * targetVertSize];
			for (int i = 0; i < size; i++) {
				System.arraycopy(source, i * sourceVertSize, newData, i * targetVertSize, sourceVertSize);
			}
			return newData;
		}
	}

	private static final ThreadLocal<short[]> tLocalHeightAndDepth = new ThreadLocal<short[]>();
	private static final ThreadLocal<long[]> tMaxVerticalData = new ThreadLocal<long[]>();
	/**
	 * This method merge column of multiple data together
	 * @param dataToMerge one or more columns of data
	 * @param inputVerticalData vertical size of an input data
	 * @param maxVerticalData max vertical size of the merged data
	 * @return one column of correctly parsed data
	 */
	// TODO: Make this operate on a out param array, to allow skipping copy array on use
	public static long[] mergeMultiData(long[] dataToMerge, int inputVerticalData, int maxVerticalData)
	{
		int size = dataToMerge.length / inputVerticalData;

		// We initialize the arrays that are going to be used
		int heightAndDepthLength = (WORLD_HEIGHT / 2 + 16) * 2;
		short[] heightAndDepth = tLocalHeightAndDepth.get();
		if (heightAndDepth==null || heightAndDepth.length != heightAndDepthLength) {
			heightAndDepth = new short[heightAndDepthLength];
			tLocalHeightAndDepth.set(heightAndDepth);
		}
		int dataPointLength = maxVerticalData;
		long[] dataPoint = tMaxVerticalData.get();
		if (dataPoint==null || dataPoint.length != dataPointLength) {
			dataPoint = new long[dataPointLength];
			tMaxVerticalData.set(dataPoint);
		} else Arrays.fill(dataPoint, 0);
		
		int genMode = getGenerationMode(dataToMerge[0]);
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
		for (int index = 0; index < size; index++)
		{
			if (index == 0)
			{
				for (dataIndex = 0; dataIndex < inputVerticalData; dataIndex++)
				{
					singleData = dataToMerge[dataIndex];
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
				for (dataIndex = 0; dataIndex < inputVerticalData; dataIndex++)
				{
					singleData = dataToMerge[index * inputVerticalData + dataIndex];
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
			limited = true;
			ii = WORLD_HEIGHT;
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
		if (!limited && size == 1)
		{
			for (j = 0; j < count; j++)
				dataPoint[j] = dataToMerge[j];
		}
		else
		{
			for (j = 0; j < count; j++)
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
				allEmpty = true;
				allVoid = true;
				long data = 0;
				
				for (int index = 0; index < size; index++)
				{
					for (dataIndex = 0; dataIndex < inputVerticalData; dataIndex++)
					{
						singleData = dataToMerge[index * inputVerticalData + dataIndex];
						if (doesItExist(singleData) && !isVoid(singleData))
						{
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
						singleData = dataToMerge[index * inputVerticalData];
						data = createVoidDataPoint(getGenerationMode(singleData));
					}
					
					if (doesItExist(data))
					{
						allEmpty = false;
						if (!isVoid(data))
						{
							numberOfChildren++;
							allVoid = false;
							tempAlpha += getAlpha(data) * getAlpha(data);
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
					dataPoint[j] = EMPTY_DATA;
				else if (allVoid)
					//all the children are void
					dataPoint[j] = createVoidDataPoint(genMode);
				else
				{
					//we have at least 1 child
					if (size != 1)
					{
						tempAlpha = tempAlpha / numberOfChildren;
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
					dataPoint[j] = createDataPoint((int) Math.sqrt(tempAlpha), (int) Math.sqrt(tempRed), (int) Math.sqrt(tempGreen), (int) Math.sqrt(tempBlue), height, depth, tempLightSky, tempLightBlock, genMode, false);
				}
			}
		}
		return dataPoint;
	}
}