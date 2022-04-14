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

package com.seibel.lod.core.builders.lodBuilding.bufferBuilding;

import com.seibel.lod.core.enums.LodDirection;
import com.seibel.lod.core.util.ColorUtil;

import static com.seibel.lod.core.render.LodRenderer.EVENT_LOGGER;

/**
 * Represents a renderable quad.
 *
 * @author James Seibel
 * @author ?
 * @version 4-9-2022
 */
public class BufferQuad
{
	final short x;
	final short y;
	final short z;
	short widthEastWest;
	/** This is both North/South and Up/Down since the merging logic is the same either way */
	short widthNorthSouthOrUpDown;
	int color;
	final byte skyLight;
	final byte blockLight;
	final LodDirection direction;
	
	
	BufferQuad(short x, short y, short z, short widthEastWest, short widthNorthSouthOrUpDown,
			int color, byte skylight, byte blocklight,
			LodDirection direction)
	{
		if (widthEastWest == 0 || widthNorthSouthOrUpDown == 0)
			throw new IllegalArgumentException("Size 0 quad!");
		if (widthEastWest < 0 || widthNorthSouthOrUpDown < 0)
			throw new IllegalArgumentException("Negative sized quad!");
		
		this.x = x;
		this.y = y;
		this.z = z;
		this.widthEastWest = widthEastWest;
		this.widthNorthSouthOrUpDown = widthNorthSouthOrUpDown;
		this.color = color;
		this.skyLight = skylight;
		this.blockLight = blocklight;
		this.direction = direction;
	}
	
	
	
	/** a rough but fast calculation */
	double calculateDistance(double relativeX, double relativeY, double relativeZ)
	{
		return Math.pow(relativeX - x, 2) + Math.pow(relativeY - y, 2) + Math.pow(relativeZ - z, 2);
	}
	
	
	/** compares this quad's position to the given quad */
	public int compare(BufferQuad quad, BufferMergeDirectionEnum compareDirection)
	{
		if (direction != quad.direction)
			throw new IllegalArgumentException("The other quad is not in the same direction: " + quad.direction + " vs " + direction);
		
		if (compareDirection == BufferMergeDirectionEnum.EastWest)
		{
			switch (direction.getAxis())
			{
			case X:
				return threeDimensionalCompare(x, y, z, quad.x, quad.y, quad.z);
			case Y:
				return threeDimensionalCompare(y, z, x, quad.y, quad.z, quad.x);
			case Z:
				return threeDimensionalCompare(z, y, x, quad.z, quad.y, quad.x);
				
			default:
				throw new IllegalArgumentException("Invalid Axis enum: " + direction.getAxis());
			}
		}
		else // if ()
		{
			switch (direction.getAxis())
			{
			case X:
				return threeDimensionalCompare(x, z, y, quad.x, quad.z, quad.y);
			case Y:
				return threeDimensionalCompare(y, x, z, quad.y, quad.x, quad.z);
			case Z:
				return threeDimensionalCompare(z, x, y, quad.z, quad.x, quad.y);
			
			default:
				throw new IllegalArgumentException("Invalid Axis enum: " + direction.getAxis());
			}
		}
	}
	/**
	 * Compares two 3D points A and B. <br>
	 * The X, Y, and Z coordinates can be passed into parameters 0, 1, and 2 in any order
	 * provided they are in the same order for both A and B. <br>
	 * With the 0th parameter being the most significant when comparing.
	 */
	private static int threeDimensionalCompare(short a0, short a1, short a2, short b0, short b1, short b2)
	{
		long a = (long) a0 << 48 | (long) a1 << 32 | (long) a2 << 16;
		long b = (long) b0 << 48 | (long) b1 << 32 | (long) b2 << 16;
		return Long.compare(a, b);
	}
	
	
	/**
	 * Attempts to merge the given quad into this one.
	 * @returns true if the quads were merged, false otherwise.
	 */
	public boolean tryMerge(BufferQuad quad, BufferMergeDirectionEnum mergeDirection)
	{
		// only merge quads that are in the same direction
		if (direction != quad.direction)
			return false;
		
		// make sure these quads share the same perpendicular axis
		if ((mergeDirection == BufferMergeDirectionEnum.EastWest && this.y != quad.y) ||
				(mergeDirection == BufferMergeDirectionEnum.NorthSouthOrUpDown && this.x != quad.x))
		{
			return false;
		}
		
		
		// get the position of each quad to compare against
		short thisPerpendicularCompareStartPos; // edge perpendicular to the merge direction
		short thisParallelCompareStartPos; // edge parallel to the merge direction
		short otherPerpendicularCompareStartPos;
		short otherParallelCompareStartPos;
		switch (this.direction.getAxis())
		{
		default: // shouldn't normally happen, just here to make the compiler happy
		case X:
			if (mergeDirection == BufferMergeDirectionEnum.EastWest)
			{
				thisPerpendicularCompareStartPos = this.z;
				thisParallelCompareStartPos = this.x;
				
				otherPerpendicularCompareStartPos = quad.z;
				otherParallelCompareStartPos = quad.x;
			}
			else //if (mergeDirection == MergeDirection.NorthSouthOrUpDown)
			{
				thisPerpendicularCompareStartPos = this.y;
				thisParallelCompareStartPos = this.z;
				
				otherPerpendicularCompareStartPos = quad.y;
				otherParallelCompareStartPos = quad.z;
			}
			break;
		
		case Y:
			if (mergeDirection == BufferMergeDirectionEnum.EastWest)
			{
				thisPerpendicularCompareStartPos = this.x;
				thisParallelCompareStartPos = this.z;
				
				otherPerpendicularCompareStartPos = quad.x;
				otherParallelCompareStartPos = quad.z;
			}
			else //if (mergeDirection == MergeDirection.NorthSouthOrUpDown)
			{
				thisPerpendicularCompareStartPos = this.z;
				thisParallelCompareStartPos = this.y;
				
				otherPerpendicularCompareStartPos = quad.z;
				otherParallelCompareStartPos = quad.y;
			}
			break;
		
		case Z:
			if (mergeDirection == BufferMergeDirectionEnum.EastWest)
			{
				thisPerpendicularCompareStartPos = this.x;
				thisParallelCompareStartPos = this.z;
				
				otherPerpendicularCompareStartPos = quad.x;
				otherParallelCompareStartPos = quad.z;
			}
			else //if (mergeDirection == MergeDirection.NorthSouthOrUpDown)
			{
				thisPerpendicularCompareStartPos = this.y;
				thisParallelCompareStartPos = this.z;
				
				otherPerpendicularCompareStartPos = quad.y;
				otherParallelCompareStartPos = quad.z;
			}
			break;
		}
		
		// get the width of this quad in the relevant axis
		short thisPerpendicularCompareWidth;
		short thisParallelCompareWidth;
		short otherParallelCompareWidth;
		if (mergeDirection == BufferMergeDirectionEnum.EastWest)
		{
			thisPerpendicularCompareWidth = this.widthEastWest;
			
			thisParallelCompareWidth = this.widthNorthSouthOrUpDown;
			otherParallelCompareWidth = quad.widthNorthSouthOrUpDown;
		}
		else
		{
			thisPerpendicularCompareWidth = this.widthNorthSouthOrUpDown;
			
			thisParallelCompareWidth = this.widthEastWest;
			otherParallelCompareWidth = quad.widthEastWest;
		}
		
		
		
		// check if these quads are adjacent
		if (thisPerpendicularCompareStartPos + thisPerpendicularCompareWidth < otherPerpendicularCompareStartPos ||
			thisParallelCompareStartPos != otherParallelCompareStartPos)
		{
			// these quads aren't adjacent, they can't be merged
			return false;
		}
		else if (thisPerpendicularCompareStartPos + thisPerpendicularCompareWidth > otherPerpendicularCompareStartPos)
		{
			// these quads are overlapping, they can't be merged
			EVENT_LOGGER.warn("Overlapping quads detected!");
			quad.color = ColorUtil.rgbToInt(255, 0, 0);
			return false;
		}
		
		
		// only merge quads that have the same width edges
		if (thisParallelCompareWidth != otherParallelCompareWidth)
		{
			return false;
		}
		
		// do the quads' color, light, etc. match?
		if (color != quad.color ||
			skyLight != quad.skyLight ||
			blockLight != quad.blockLight)
		{
			// we can only merge identically colored/lit quads
			return false;
		}
		
		// merge the two quads
		if (mergeDirection == BufferMergeDirectionEnum.NorthSouthOrUpDown)
		{
			widthNorthSouthOrUpDown += quad.widthNorthSouthOrUpDown;
		}
		else // if (mergeDirection == MergeDirection.EastWest)
		{
			widthEastWest += quad.widthEastWest;
		}
		
		// merge successful
		return true;
	}
	
}