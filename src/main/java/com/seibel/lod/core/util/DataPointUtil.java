/*
 *    This file is part of the Distant Horizons mod (formerly the LOD Mod),
 *    licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2020-2022  James Seibel
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

import com.seibel.lod.core.api.ClientApi;
import com.seibel.lod.core.logging.SpamReducedLogger;

import java.util.Arrays;

// NOTE: me <3
import com.seibel.lod.core.api.ClientApi;
import org.apache.logging.log4j.Level;

/**
 *
 * @author Leonardo Amato
 * @version ??
 */
public class DataPointUtil {
  /*
   *
   * |_ |g |g |g |a |a |a |a | |r |r |r |r |r |r |r |r | |g |g |g |g |g |g |g |g |
   * |b |b |b |b |b |b |b |b |
   *
   * |h |h |h |h |h |h |h |h | |h |h |h |h |d |d |d |d | |d |d |d |d |d |d |d |d |
   * |bl |bl |bl |bl |sl |sl |sl |sl |
   *
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

  public static long createVoidDataPoint(byte generationMode) {
    if (generationMode == 0)
      throw new IllegalArgumentException(
          "Trying to create void datapoint with genMode 0, which is NOT allowed in DataPoint version 10!");
    return (generationMode & GEN_TYPE_MASK) << GEN_TYPE_SHIFT;
  }

  public static long createDataPoint(int height, int depth, int color, int lightSky, int lightBlock,
      int generationMode) {
    return createDataPoint(ColorUtil.getAlpha(color), ColorUtil.getRed(color), ColorUtil.getGreen(color),
        ColorUtil.getBlue(color), height, depth, lightSky, lightBlock, generationMode);
  }

  public static long createDataPoint(int alpha, int red, int green, int blue, int height, int depth, int lightSky,
      int lightBlock, int generationMode) {
    if (generationMode == 0)
      throw new IllegalArgumentException(
          "Trying to create datapoint with genMode 0, which is NOT allowed in DataPoint version 10!");
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

    return (long) (alpha >>> ALPHA_DOWNSIZE_SHIFT) << ALPHA_SHIFT | (red & RED_MASK) << RED_SHIFT
        | (green & GREEN_MASK) << GREEN_SHIFT | (blue & BLUE_MASK) << BLUE_SHIFT
        | (height & HEIGHT_MASK) << HEIGHT_SHIFT | (depth & DEPTH_MASK) << DEPTH_SHIFT
        | (lightBlock & BLOCK_LIGHT_MASK) << BLOCK_LIGHT_SHIFT | (lightSky & SKY_LIGHT_MASK) << SKY_LIGHT_SHIFT
        | (generationMode & GEN_TYPE_MASK) << GEN_TYPE_SHIFT;
  }

  public static long shiftHeightAndDepth(long dataPoint, short offset) {
    long height = (dataPoint + ((long) offset << HEIGHT_SHIFT)) & HEIGHT_SHIFTED_MASK;
    long depth = (dataPoint + (offset << DEPTH_SHIFT)) & DEPTH_SHIFTED_MASK;
    return dataPoint & ~(HEIGHT_SHIFTED_MASK | DEPTH_SHIFTED_MASK) | height | depth;
  }

  public static long version9Reorder(long dataPoint) {
    /*
     * |a |a |a |a |r |r |r |r | |r |r |r |r |g |g |g |g | |g |g |g |g |b |b |b |b |
     * |b |b |b |b |h |h |h |h | |h |h |h |h |h |h |d |d | |d |d |d |d |d |d |d |d |
     * |bl |bl |bl |bl |sl |sl |sl |sl | |l |l |f |g |g |g |v |e |
     */
    if ((dataPoint & 1) == 0)
      return 0;

    long height = (dataPoint >>> 26) & 0x3FF;
    long depth = (dataPoint >>> 16) & 0x3FF;
    if (height == depth || (dataPoint & 0b10) == 0b10) {
      return createVoidDataPoint((byte) (((dataPoint >>> 2) & 0b111) + 1));
    }
    return ((dataPoint >>> 60) & 0xF) << ALPHA_SHIFT | ((dataPoint >>> 52) & 0xFF) << RED_SHIFT
        | ((dataPoint >>> 44) & 0xFF) << GREEN_SHIFT | ((dataPoint >>> 36) & 0xFF) << BLUE_SHIFT
        | ((dataPoint >>> 26) & 0x3FF) << HEIGHT_SHIFT | ((dataPoint >>> 16) & 0x3FF) << DEPTH_SHIFT
        | ((dataPoint >>> 8) & 0xFF) << SKY_LIGHT_SHIFT | (((dataPoint >>> 2) & 0xFF) + 1) << GEN_TYPE_SHIFT;
  }

  public static short getHeight(long dataPoint) {
    return (short) ((dataPoint >>> HEIGHT_SHIFT) & HEIGHT_MASK);
  }

  public static short getDepth(long dataPoint) {
    return (short) ((dataPoint >>> DEPTH_SHIFT) & DEPTH_MASK);
  }

  public static short getAlpha(long dataPoint) {
    return (short) ((((dataPoint >>> ALPHA_SHIFT) & ALPHA_MASK) << ALPHA_DOWNSIZE_SHIFT) | 0b1111);
  }

  public static short getRed(long dataPoint) {
    return (short) ((dataPoint >>> RED_SHIFT) & RED_MASK);
  }

  public static short getGreen(long dataPoint) {
    return (short) ((dataPoint >>> GREEN_SHIFT) & GREEN_MASK);
  }

  public static short getBlue(long dataPoint) {
    return (short) ((dataPoint >>> BLUE_SHIFT) & BLUE_MASK);
  }

  public static byte getLightSky(long dataPoint) {
    return (byte) ((dataPoint >>> SKY_LIGHT_SHIFT) & SKY_LIGHT_MASK);
  }

  public static byte getLightBlock(long dataPoint) {
    return (byte) ((dataPoint >>> BLOCK_LIGHT_SHIFT) & BLOCK_LIGHT_MASK);
  }

  private static final SpamReducedLogger warnLogger = new SpamReducedLogger(1);

  public static byte getGenerationMode(long dataPoint) {
    byte genMode = (byte) ((dataPoint >>> GEN_TYPE_SHIFT) & GEN_TYPE_MASK);
    if (warnLogger.canMaybeLog() && doesItExist(dataPoint) && genMode == 0) {
      warnLogger.warnInc("Existing datapoint with genMode 0 detected! This is invalid in DataPoint version 10!"
          + " This may be caused by old data that has not been updated correctly.");
      return 1;
    }
    return genMode == 0 ? 1 : genMode;
  }

  public static boolean isVoid(long dataPoint) {
    return (((dataPoint >>> DEPTH_SHIFT) & HEIGHT_DEPTH_MASK) == 0);
  }

  public static boolean doesItExist(long dataPoint) {
    return dataPoint != 0;
  }

  public static int getColor(long dataPoint) {
    long alpha = getAlpha(dataPoint);
    return (int) (((dataPoint >>> COLOR_SHIFT) & COLOR_MASK) | (alpha << (ALPHA_SHIFT - COLOR_SHIFT)));
  }

  /**
   * This is used to convert a dataPoint to string (useful for the print function)
   */
  @SuppressWarnings("unused")
  public static String toString(long dataPoint) {
    return getHeight(dataPoint) + " " + getDepth(dataPoint) + " " + getAlpha(dataPoint) + " " + getRed(dataPoint) + " "
        + getBlue(dataPoint) + " " + getGreen(dataPoint) + " " + getLightBlock(dataPoint) + " " + getLightSky(dataPoint)
        + " " + getGenerationMode(dataPoint) + " " + isVoid(dataPoint) + " " + doesItExist(dataPoint) + '\n';
  }

  public static void shrinkArray(short[] array, int packetSize, int start, int length, int arraySize) {
    start *= packetSize;
    length *= packetSize;
    arraySize *= packetSize;
    // remove comment to not leave garbage at the end
    // array[start + packetSize + i] = 0;
    if (arraySize - start >= 0)
      System.arraycopy(array, start + length, array, start, arraySize - start);
  }

  public static void extendArray(short[] array, int packetSize, int start, int length, int arraySize) {
    start *= packetSize;
    length *= packetSize;
    arraySize *= packetSize;
    for (int i = arraySize - start - 1; i >= 0; i--) {
      array[start + length + i] = array[start + i];
      array[start + i] = 0;
    }
  }

  /**
   * Return (>0) if dataA should replace dataB, (0) if equal, (<0) if dataB should
   * replace dataA
   */
  public static int compareDatapointPriority(long dataA, long dataB) {
    return (int) ((dataA >> COMPARE_SHIFT) - (dataB >> COMPARE_SHIFT));
  }

  // Merge the newData data into the target data by comparing the Datapoint
  // Priority.
  // If target and newData are same priority, it will use the target one.
  // If target and newData size are different, it will copy up to the smallest
  // one.
  // Return whether anything changed
  public static boolean mergeTwoDataArray(long[] target, long[] newData, int verticalDataSize) {
    boolean anyChange = false;
    for (int i = 0; i < target.length && i < newData.length; i += verticalDataSize) {
      if (compareDatapointPriority(newData[i], target[i]) > 0) {
        anyChange = true;
        System.arraycopy(newData, i, target, i, verticalDataSize);
      }
    }
    return anyChange;
  }

  // Same as above, but with args for buffer offset.
  // Return whether anything changed
  public static boolean mergeTwoDataArray(long[] target, int targetOffset, long[] newData, int newDataOffset,
      int dataLength, int verticalDataSize, boolean override) {
    if (targetOffset + verticalDataSize * dataLength > target.length)
      throw new ArrayIndexOutOfBoundsException("\"target\" array index out of bounds");
    if (newDataOffset + verticalDataSize * dataLength > newData.length)
      throw new ArrayIndexOutOfBoundsException("\"newData\" array index out of bounds");
    boolean anyChange = false;
    for (int o = 0; o < (dataLength * verticalDataSize); o += verticalDataSize) {
      if (override) {
        if (compareDatapointPriority(newData[o + newDataOffset], target[o + targetOffset]) >= 0) {
          anyChange = true;
          System.arraycopy(newData, o + newDataOffset, target, o + targetOffset, verticalDataSize);
        }
      } else {
        if (compareDatapointPriority(newData[o + newDataOffset], target[o + targetOffset]) > 0) {
          anyChange = true;
          System.arraycopy(newData, o + newDataOffset, target, o + targetOffset, verticalDataSize);
        }
      }
    }
    return anyChange;
  }

  // Extract a section of data from the 2D data array
  public static long[] extractDataArray(long[] source, int inWidth, int inHeight, int outX, int outY, int outWidth,
      int outHeight) {
    int dataSetSize = source.length / inWidth / inHeight;
    if (dataSetSize * inWidth * inHeight != source.length)
      throw new ArrayIndexOutOfBoundsException("\"source\" array invalid width and height");
    if (outWidth > inWidth || outX + outWidth > inWidth)
      throw new ArrayIndexOutOfBoundsException("X index out of bounds");
    if (outHeight > inHeight || outY + outHeight > inHeight)
      throw new ArrayIndexOutOfBoundsException("Y index out of bounds");
    long[] out = new long[dataSetSize * outWidth * outHeight];
    for (int x = 0; x < outWidth; x++) {
      System.arraycopy(source, ((outX + x) * inHeight + outY) * dataSetSize, out, (x * outHeight) * dataSetSize,
          outHeight * dataSetSize);
    }
    return out;
  }

  // Extract a section of data from the 2D data array
  // WARN: if sourceVertSize == targetVertSize, it will return the source array!!!
  // Be careful about
  // this!
  public static long[] changeMaxVertSize(long[] source, int sourceVertSize, int targetVertSize) {
    if (source.length % sourceVertSize != 0)
      throw new ArrayIndexOutOfBoundsException("\"source\" array invalid vertical size or length");
    if (sourceVertSize == targetVertSize)
      return source;
    if (sourceVertSize > targetVertSize) {
      int size = source.length / sourceVertSize;
      long[] dataToMerge = new long[sourceVertSize];
      long[] newData = new long[size * targetVertSize];
      for (int i = 0; i < size; i++) {
        System.arraycopy(source, i * sourceVertSize, dataToMerge, 0, sourceVertSize);
        long[] tempBuffer = DataPointUtil.mergeMultiData(dataToMerge, sourceVertSize, targetVertSize);
        System.arraycopy(tempBuffer, 0, newData, i * targetVertSize, targetVertSize);
      }
      return newData;
    } else {
      int size = source.length / sourceVertSize;
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
   *
   * @param dataToMerge       one or more columns of data
   * @param inputVerticalData vertical size of an input data
   * @param maxVerticalData   max vertical size of the merged data
   * @return one column of correctly parsed data
   */
  // TODO: Make this operate on a out param array, to allow skipping copy array on
  // use
  public static long[] mergeMultiData(long[] dataToMerge, int inputVerticalData, int maxVerticalData) {
    int size = dataToMerge.length / inputVerticalData;

    // We initialize the arrays that are going to be used
    int heightAndDepthLength = (MAX_WORLD_Y_SIZE / 2 + 16) * 2;
    short[] heightAndDepth = tLocalHeightAndDepth.get();
    if (heightAndDepth == null || heightAndDepth.length != heightAndDepthLength) {
      heightAndDepth = new short[heightAndDepthLength];
      tLocalHeightAndDepth.set(heightAndDepth);
    }
    int dataPointLength = maxVerticalData;
    long[] dataPoint = tMaxVerticalData.get();
    if (dataPoint == null || dataPoint.length != dataPointLength) {
      dataPoint = new long[dataPointLength];
      tMaxVerticalData.set(dataPoint);
    } else
      Arrays.fill(dataPoint, 0);

    byte genMode = getGenerationMode(dataToMerge[0]);
    if (genMode == 0)
      genMode = 1; // FIXME: Hack to make the version 10 genMode never be 0.
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
    // We collect the indexes of the data, ordered by the depth
    for (int index = 0; index < size; index++) {
      if (index == 0) {
        for (dataIndex = 0; dataIndex < inputVerticalData; dataIndex++) {
          singleData = dataToMerge[dataIndex];
          if (doesItExist(singleData)) {
            // genMode = Math.min(genMode, getGenerationMode(singleData));
            allEmpty = false;
            if (!isVoid(singleData)) {
              allVoid = false;
              count++;
              heightAndDepth[dataIndex * 2] = getHeight(singleData);
              heightAndDepth[dataIndex * 2 + 1] = getDepth(singleData);
            }
          } else
            break;
        }
      } else {
        for (dataIndex = 0; dataIndex < inputVerticalData; dataIndex++) {
          singleData = dataToMerge[index * inputVerticalData + dataIndex];
          if (doesItExist(singleData)) {
            // genMode = Math.min(genMode, getGenerationMode(singleData));
            allEmpty = false;
            if (!isVoid(singleData)) {
              allVoid = false;
              depth = getDepth(singleData);
              height = getHeight(singleData);

              int botPos = -1;
              int topPos = -1;
              // values fall in between and possibly require extension of array
              boolean botExtend = false;
              boolean topExtend = false;
              for (i = 0; i < count; i++) {
                if (depth < heightAndDepth[i * 2] && depth >= heightAndDepth[i * 2 + 1]) {
                  botPos = i;
                  break;
                } else if (depth < heightAndDepth[i * 2 + 1]
                    && ((i + 1 < count && depth >= heightAndDepth[(i + 1) * 2]) || i + 1 == count)) {
                  botPos = i;
                  botExtend = true;
                  break;
                }
              }
              for (i = 0; i < count; i++) {
                if (height <= heightAndDepth[i * 2] && height > heightAndDepth[i * 2 + 1]) {
                  topPos = i;
                  break;
                } else if (height <= heightAndDepth[i * 2 + 1]
                    && ((i + 1 < count && height > heightAndDepth[(i + 1) * 2]) || i + 1 == count)) {
                  topPos = i;
                  topExtend = true;
                  break;
                }
              }

              if (topPos == -1) {
                if (botPos == -1) {
                  // whole block falls above
                  extendArray(heightAndDepth, 2, 0, 1, count);
                  heightAndDepth[0] = height;
                  heightAndDepth[1] = depth;
                  count++;
                } else if (!botExtend) {
                  // only top falls above extending it there, while bottom is inside existing
                  shrinkArray(heightAndDepth, 2, 0, botPos, count);
                  heightAndDepth[0] = height;
                  count -= botPos;
                } else {
                  // top falls between some blocks, extending those as well
                  shrinkArray(heightAndDepth, 2, 0, botPos, count);
                  heightAndDepth[0] = height;
                  heightAndDepth[1] = depth;
                  count -= botPos;
                }
              } else if (!topExtend) { // TODO here
                // if (!botExtend)
                // // both top and bottom are within some exiting blocks, possibly merging them
                // heightAndDepth[topPos * 2 + 1] = heightAndDepth[botPos * 2 + 1];
                // else
                // // top falls between some blocks, extending it there
                // heightAndDepth[topPos * 2 + 1] = depth;
                if (!botExtend) {
                  // ClientApi.logToChat(Level.INFO, "==========");
                  // ClientApi.logToChat(Level.INFO, "topPos: " + topPos);
                  // ClientApi.logToChat(Level.INFO, "topPos * 2 + 1: " + (topPos * 2 + 1));
                  // ClientApi.logToChat(Level.INFO, "heightAndDepth: " + heightAndDepth.length);
                  // ClientApi.logToChat(Level.INFO, "botPos: " + botPos);
                  // ClientApi.logToChat(Level.INFO, "botPos * 2 + 1: " + (botPos * 2 + 1));

                  if (botPos * 2 + 1 < heightAndDepth.length && topPos * 2 + 1 >= 0 && botPos * 2 + 1 >= 0) {
                    heightAndDepth[topPos * 2 + 1] = heightAndDepth[botPos * 2 + 1];
                  }
                  // heightAndDepth[topPos * 2 + 1] = heightAndDepth[botPos * 2 + 1];

                  // ClientApi.logToChat(Level.INFO, "==========");
                } else {
                  // ClientApi.logToChat(Level.INFO, "==========");
                  // ClientApi.logToChat(Level.INFO, "botPos: " + botPos);
                  // ClientApi.logToChat(Level.INFO, "botPos * 2 + 1: " + (botPos * 2 + 1));
                  // ClientApi.logToChat(Level.INFO, "heightAndDepth: " + heightAndDepth.length);
                  // ClientApi.logToChat(Level.INFO, "topPos: " + topPos);
                  // ClientApi.logToChat(Level.INFO, "topPos * 2 + 1: " + (topPos * 2 + 1));

                  // if (topPos * 2 + 1 >= heightAndDepth.length) {
                  // // ClientApi.logToChat(Level.INFO, "would be an index oob");
                  // } else {
                  // // ClientApi.logToChat(Level.INFO, "setting to depth: " + depth);
                  // // top falls between some blocks, extending it there
                  // heightAndDepth[topPos * 2 + 1] = depth;
                  // }
                  if (topPos * 2 + 1 < heightAndDepth.length && topPos * 2 + 1 >= 0) {
                    // ClientApi.logToChat(Level.INFO, "would be an index oob");
                    heightAndDepth[topPos * 2 + 1] = depth;
                  }
                  // heightAndDepth[topPos * 2 + 1] = depth;
                  // ClientApi.logToChat(Level.INFO, "==========");
                }

                shrinkArray(heightAndDepth, 2, topPos + 1, botPos - topPos, count);
                count -= botPos - topPos;
              } else {
                if (!botExtend) {
                  // only top is within some exiting block, extending it
                  topPos++; // to make it easier
                  if (topPos * 2 >= 0 && topPos * 2 < heightAndDepth.length) {
                    heightAndDepth[topPos * 2] = height;
                  }
                  if (topPos * 2 + 1 >= 0 && topPos * 2 + 1 < heightAndDepth.length && botPos * 2 + 1 >= 0 && botPos * 2 + 1 < heightAndDepth.length) {
                    heightAndDepth[topPos * 2 + 1] = heightAndDepth[botPos * 2 + 1];
                  }
                    shrinkArray(heightAndDepth, 2, topPos + 1, botPos - topPos, count);
                    count -= botPos - topPos;
                } else {
                  // both top and bottom are outside existing blocks
                  shrinkArray(heightAndDepth, 2, topPos + 1, botPos - topPos, count);
                  count -= botPos - topPos;
                  extendArray(heightAndDepth, 2, topPos + 1, 1, count);
                  count++;
                  if (topPos * 2 + 2 < heightAndDepth.length && topPos * 2 + 2 >= 0) {
                    heightAndDepth[topPos * 2 + 2] = height;
                  }
                  if (topPos * 2 + 3 < heightAndDepth.length && topPos * 2 + 3 >= 0) {
                    heightAndDepth[topPos * 2 + 3] = depth;
                  }
                }
              }
            }
          } else
            break;
        }
      }
    }

    // We check if there is any data that's not empty or void
    if (allEmpty)
      return dataPoint;
    if (allVoid) {
      dataPoint[0] = createVoidDataPoint(genMode);
      return dataPoint;
    }

    // we limit the vertical portion to maxVerticalData
    int j = 0;
    while (count > maxVerticalData) {
      limited = true;
      ii = MAX_WORLD_Y_SIZE;
      for (i = 0; i < count - 1; i++) {
        if (heightAndDepth[i * 2 + 1] - heightAndDepth[(i + 1) * 2] <= ii) {
          ii = heightAndDepth[i * 2 + 1] - heightAndDepth[(i + 1) * 2];
          j = i;
        }
      }
      heightAndDepth[j * 2 + 1] = heightAndDepth[(j + 1) * 2 + 1];
      for (i = j + 1; i < count - 1; i++) {
        heightAndDepth[i * 2] = heightAndDepth[(i + 1) * 2];
        heightAndDepth[i * 2 + 1] = heightAndDepth[(i + 1) * 2 + 1];
      }
      // System.arraycopy(heightAndDepth, j + 1, heightAndDepth, j, count - j - 1);
      count--;
    }
    // As standard the vertical lods are ordered from top to bottom
    if (!limited && size == 1) {
      for (j = 0; j < count; j++)
        dataPoint[j] = dataToMerge[j];
    } else {
      for (j = 0; j < count; j++) {
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

        for (int index = 0; index < size; index++) {
          for (dataIndex = 0; dataIndex < inputVerticalData; dataIndex++) {
            singleData = dataToMerge[index * inputVerticalData + dataIndex];
            if (doesItExist(singleData) && !isVoid(singleData)) {
              if ((depth <= getDepth(singleData) && getDepth(singleData) < height)
                  || (depth < getHeight(singleData) && getHeight(singleData) <= height)) {
                data = singleData;
                break;
              }
            } else
              break;
          }
          if (!doesItExist(data)) {
            singleData = dataToMerge[index * inputVerticalData];
            data = createVoidDataPoint(genMode);
          }

          if (doesItExist(data)) {
            allEmpty = false;
            if (!isVoid(data)) {
              numberOfChildren++;
              allVoid = false;
              tempAlpha = Math.max(getAlpha(data), tempAlpha);
              tempRed += getRed(data) * getRed(data);
              tempGreen += getGreen(data) * getGreen(data);
              tempBlue += getBlue(data) * getBlue(data);
              tempLightBlock += getLightBlock(data);
              tempLightSky += getLightSky(data);
            }
          }
        }

        if (allEmpty)
          // no child has been initialized
          dataPoint[j] = EMPTY_DATA;
        else if (allVoid)
          // all the children are void
          dataPoint[j] = createVoidDataPoint(genMode);
        else {
          // we have at least 1 child
          if (size != 1) {
            tempRed = tempRed / numberOfChildren;
            tempGreen = tempGreen / numberOfChildren;
            tempBlue = tempBlue / numberOfChildren;
            tempLightBlock = tempLightBlock / numberOfChildren;
            tempLightSky = tempLightSky / numberOfChildren;
          }
          // data = createDataPoint(tempAlpha, tempRed, tempGreen, tempBlue, height,
          // depth,
          // tempLightSky, tempLightBlock, tempGenMode, allDefault); if (j > 0 &&
          // getColor(data) ==
          // getColor(dataPoint[j]))
          // {
          // add simplification at the end due to color
          // }
          dataPoint[j] = createDataPoint((int) Math.sqrt(tempAlpha), (int) Math.sqrt(tempRed),
              (int) Math.sqrt(tempGreen), (int) Math.sqrt(tempBlue), height, depth, tempLightSky, tempLightBlock,
              genMode);
        }
      }
    }
    return dataPoint;
  }
}
