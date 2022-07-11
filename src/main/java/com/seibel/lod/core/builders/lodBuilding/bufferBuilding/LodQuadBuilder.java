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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;

import com.seibel.lod.core.builders.lodBuilding.LodBuilder;
import com.seibel.lod.core.enums.LodDirection;
import com.seibel.lod.core.enums.LodDirection.Axis;
import com.seibel.lod.core.enums.config.GpuUploadMethod;
import com.seibel.lod.core.handlers.dependencyInjection.SingletonHandler;
import com.seibel.lod.core.render.LodRenderer;
import com.seibel.lod.core.render.objects.GLVertexBuffer;
import com.seibel.lod.core.util.ColorUtil;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton;

import static com.seibel.lod.core.render.LodRenderer.EVENT_LOGGER;

/**
 * Used to create the quads before they are converted to renderable buffers.
 *
 * @version 2022-4-9
 */
public class LodQuadBuilder
{
	static final ILodConfigWrapperSingleton CONFIG = SingletonHandler.get(ILodConfigWrapperSingleton.class);
	
	public final boolean skipQuadsWithZeroSkylight;
	public final short skyLightCullingBelow;
	
	final ArrayList<BufferQuad>[] quads = (ArrayList<BufferQuad>[]) new ArrayList[6];
	
	public static final int[][][] DIRECTION_VERTEX_IBO_QUAD = new int[][][]
		{
			// X,Z //
			{ // UP
				{ 1, 0 }, // 0
				{ 1, 1 }, // 1
				{ 0, 1 }, // 2
				{ 0, 0 }, // 3
			},
			{ // DOWN
				{ 0, 0 }, // 0
				{ 0, 1 }, // 1
				{ 1, 1 }, // 2
				{ 1, 0 }, // 3
			},
			
			// X,Y //
			{ // NORTH
				{ 0, 0 }, // 0
				{ 0, 1 }, // 1
				{ 1, 1 }, // 2

				{ 1, 0 }, // 3
			},
			{ // SOUTH
				{ 1, 0 }, // 0
				{ 1, 1 }, // 1
				{ 0, 1 }, // 2

				{ 0, 0 }, // 3
			},
			
			// Z,Y //
			{ // WEST
				{ 0, 0 }, // 0
				{ 1, 0 }, // 1
				{ 1, 1 }, // 2

				{ 0, 1 }, // 3
			},
			{ // EAST
				{ 0, 1 }, // 0
				{ 1, 1 }, // 1
				{ 1, 0 }, // 2

				{ 0, 0 }, // 3
			},
		};
	public static final int[][][] DIRECTION_VERTEX_QUAD = new int[][][]
			{
					// X,Z //
					{ // UP
							{ 1, 0 }, // 0
							{ 1, 1 }, // 1
							{ 0, 1 }, // 2

							{ 1, 0 }, // 0
							{ 0, 1 }, // 2
							{ 0, 0 }, // 3
					},
					{ // DOWN
							{ 0, 0 }, // 0
							{ 0, 1 }, // 1
							{ 1, 1 }, // 2

							{ 0, 0 }, // 0
							{ 1, 1 }, // 2
							{ 1, 0 }, // 3
					},

					// X,Y //
					{ // NORTH
							{ 0, 0 }, // 0
							{ 0, 1 }, // 1
							{ 1, 1 }, // 2

							{ 0, 0 }, // 0
							{ 1, 1 }, // 2
							{ 1, 0 }, // 3
					},
					{ // SOUTH
							{ 1, 0 }, // 0
							{ 1, 1 }, // 1
							{ 0, 1 }, // 2

							{ 1, 0 }, // 0
							{ 0, 1 }, // 2
							{ 0, 0 }, // 3
					},

					// Z,Y //
					{ // WEST
							{ 0, 0 }, // 0
							{ 1, 0 }, // 1
							{ 1, 1 }, // 2

							{ 0, 0 }, // 0
							{ 1, 1 }, // 2
							{ 0, 1 }, // 3
					},
					{ // EAST
							{ 0, 1 }, // 0
							{ 1, 1 }, // 1
							{ 1, 0 }, // 2

							{ 0, 1 }, // 0
							{ 1, 0 }, // 2
							{ 0, 0 }, // 3
					},
			};
	
	
	public LodQuadBuilder(boolean enableSkylightCulling, int skyLightCullingBelow)
	{
		for (int i = 0; i < 6; i++)
			quads[i] = new ArrayList<>();
		
		this.skipQuadsWithZeroSkylight = enableSkylightCulling;
		// this.skyLightCullingBelow = (short) (skyLightCullingBelow - LodBuilder.MIN_WORLD_HEIGHT);
		this.skyLightCullingBelow = (short) (skyLightCullingBelow - -64);
	}
	
	
	
	
	public void addQuadAdj(LodDirection dir, short x, short y, short z,
			short widthEastWest, short widthNorthSouthOrUpDown,
			int color, byte skylight, byte blocklight)
	{
		if (dir.ordinal() <= LodDirection.DOWN.ordinal())
			throw new IllegalArgumentException("addQuadAdj() is only for adj direction! Not UP or Down!");
		if (skipQuadsWithZeroSkylight && skylight == 0 && y < skyLightCullingBelow)
			return;
		quads[dir.ordinal()].add(new BufferQuad(x, y, z, widthEastWest, widthNorthSouthOrUpDown, color, skylight, blocklight, dir));
	}
	
	// XZ
	public void addQuadUp(short x, short y, short z, short width, short wz, int color, byte skylight, byte blocklight)
	{
		if (skipQuadsWithZeroSkylight && skylight == 0 && y < skyLightCullingBelow)
			return;
		quads[LodDirection.UP.ordinal()].add(new BufferQuad(x, y, z, width, wz, color, skylight, blocklight, LodDirection.UP));
	}
	
	public void addQuadDown(short x, short y, short z, short width, short wz, int color, byte skylight, byte blocklight)
	{
		if (skipQuadsWithZeroSkylight && skylight == 0 && y < skyLightCullingBelow)
			return;
		quads[LodDirection.DOWN.ordinal()].add(new BufferQuad(x, y, z, width, wz, color, skylight, blocklight, LodDirection.DOWN));
	}
	
	// XY
	public void addQuadN(short x, short y, short z, short width, short wy, int color, byte skylight, byte blocklight)
	{
		if (skipQuadsWithZeroSkylight && skylight == 0 && y < skyLightCullingBelow)
			return;
		quads[LodDirection.NORTH.ordinal()].add(new BufferQuad(x, y, z, width, wy, color, skylight, blocklight, LodDirection.NORTH));
	}
	
	public void addQuadS(short x, short y, short z, short width, short wy, int color, byte skylight, byte blocklight)
	{
		if (skipQuadsWithZeroSkylight && skylight == 0 && y < skyLightCullingBelow)
			return;
		quads[LodDirection.SOUTH.ordinal()].add(new BufferQuad(x, y, z, width, wy, color, skylight, blocklight, LodDirection.SOUTH));
	}
	
	// ZY
	public void addQuadW(short x, short y, short z, short width, short wy, int color, byte skylight, byte blocklight)
	{
		if (skipQuadsWithZeroSkylight && skylight == 0 && y < skyLightCullingBelow)
			return;
		quads[LodDirection.WEST.ordinal()].add(new BufferQuad(x, y, z, width, wy, color, skylight, blocklight, LodDirection.WEST));
	}
	
	public void addQuadE(short x, short y, short z, short width, short wy, int color, byte skylight, byte blocklight)
	{
		if (skipQuadsWithZeroSkylight && skylight == 0 && y < skyLightCullingBelow)
			return;
		quads[LodDirection.EAST.ordinal()].add(new BufferQuad(x, y, z, width, wy, color, skylight, blocklight, LodDirection.EAST));
	}
	
	
	
	private static void putVertex(ByteBuffer bb, short x, short y, short z, int color, byte skylight, byte blocklight, int mx, int my, int mz)
	{
		skylight %= 16;
		blocklight %= 16;
		
		bb.putShort(x);
		bb.putShort(y);
		bb.putShort(z);

		short meta = 0;
		meta |= (skylight | (blocklight << 4));
		byte mirco = 0;
		// mirco offset which is a xyz 2bit value
		// 0b00 = no offset
		// 0b01 = positive offset
		// 0b11 = negative offset
		// format is: 0b00zzyyxx
		if (mx != 0) mirco |= mx > 0 ? 0b01 : 0b11;
		if (my != 0) mirco |= my > 0 ? 0b0100 : 0b1100;
		if (mz != 0) mirco |= mz > 0 ? 0b010000 : 0b110000;
		meta |= mirco << 8;

		bb.putShort(meta);
		byte r = (byte) ColorUtil.getRed(color);
		byte g = (byte) ColorUtil.getGreen(color);
		byte b = (byte) ColorUtil.getBlue(color);
		byte a = (byte) ColorUtil.getAlpha(color);
		bb.put(r);
		bb.put(g);
		bb.put(b);
		bb.put(a);
	}
	
	private static void putQuad(ByteBuffer bb, BufferQuad quad)
	{
		int[][] quadBase = LodRenderer.ENABLE_IBO ? DIRECTION_VERTEX_IBO_QUAD[quad.direction.ordinal()] : DIRECTION_VERTEX_QUAD[quad.direction.ordinal()];
		short widthEastWest = quad.widthEastWest;
		short widthNorthSouth = quad.widthNorthSouthOrUpDown;
		Axis axis = quad.direction.getAxis();
		for (int i = 0; i < quadBase.length; i++)
		{
			short dx, dy, dz;
			int mx, my, mz;
			switch (axis)
			{
			case X: // ZY
				dx = 0;
				dy = quadBase[i][1] == 1 ? widthNorthSouth : 0;
				dz = quadBase[i][0] == 1 ? widthEastWest : 0;
				mx = 0;
				my = quadBase[i][1] == 1 ? 1 : -1;
				mz = quadBase[i][0] == 1 ? 1 : -1;
				break;
			case Y: // XZ
				dx = quadBase[i][0] == 1 ? widthEastWest : 0;
				dy = 0;
				dz = quadBase[i][1] == 1 ? widthNorthSouth : 0;
				mx = quadBase[i][0] == 1 ? 1 : -1;
				my = 0;
				mz = quadBase[i][1] == 1 ? 1 : -1;
				break;
			case Z: // XY
				dx = quadBase[i][0] == 1 ? widthEastWest : 0;
				dy = quadBase[i][1] == 1 ? widthNorthSouth : 0;
				dz = 0;
				mx = quadBase[i][0] == 1 ? 1 : -1;
				my = quadBase[i][1] == 1 ? 1 : -1;
				mz = 0;
				break;
			default:
				throw new IllegalArgumentException("Invalid Axis enum: " + axis);
			}
			putVertex(bb, (short) (quad.x + dx), (short) (quad.y + dy), (short) (quad.z + dz),
					quad.hasError ? ColorUtil.RED : quad.color,
					quad.hasError ? 15 : quad.skyLight,
					quad.hasError ? 15 : quad.blockLight,
					mx, my, mz);
		}
	}
	
	/** Uses Greedy meshing to merge this builder's Quads. */
	public void mergeQuads()
	{
		long mergeCount = 0;
		long preQuadsCount = getCurrentQuadsCount();
		if (preQuadsCount <= 1)
			return;
		
		for (int directionIndex = 0; directionIndex < 6; directionIndex++)
		{
			mergeCount += mergeQuadsInternal(directionIndex, BufferMergeDirectionEnum.EastWest);
			
			// only merge after the top has been merged
			if (directionIndex == 1)
			{
				long pass2 = mergeQuadsInternal(directionIndex, BufferMergeDirectionEnum.NorthSouthOrUpDown);
				mergeCount += pass2;
			}
		}
		long postQuadsCount = getCurrentQuadsCount();
		//if (mergeCount != 0)
		EVENT_LOGGER.debug("Merged {}/{}({}) quads", mergeCount, preQuadsCount, mergeCount / (double) preQuadsCount);
	}
	/** Merges all of this builder's quads for the given directionIndex (up, down, left, etc.) in the given direction */
	private long mergeQuadsInternal(int directionIndex, BufferMergeDirectionEnum mergeDirection)
	{
		if (quads[directionIndex].size() <= 1)
			return 0;
		
		quads[directionIndex].sort( (objOne, objTwo) -> objOne.compare(objTwo, mergeDirection) );
		
		long mergeCount = 0;
		ListIterator<BufferQuad> iter = quads[directionIndex].listIterator();
		BufferQuad currentQuad = iter.next();
		while (iter.hasNext())
		{
			BufferQuad nextQuad = iter.next();
			
			if (currentQuad.tryMerge(nextQuad, mergeDirection))
			{
				// merge successful, attempt to merge the next quad
				mergeCount++;
				iter.set(null);
			}
			else
			{
				// merge fail, move on to the next quad
				currentQuad = nextQuad;
			}
		}
		quads[directionIndex].removeIf(o -> o == null);
		return mergeCount;
	}
	
	
	
	public Iterator<ByteBuffer> makeVertexBuffers()
	{
		return new Iterator<ByteBuffer>()
		{
			final ByteBuffer bb = ByteBuffer.allocateDirect(LodBufferBuilderFactory.FULL_SIZED_BUFFER)
					.order(ByteOrder.nativeOrder());
			int dir = skipEmpty(0);
			int quad = 0;
			
			private int skipEmpty(int d)
			{
				while (d < 6 && quads[d].isEmpty())
					d++;
				return d;
			}
			
			@Override
			public boolean hasNext()
			{
				return dir < 6;
			}
			
			@Override
			public ByteBuffer next()
			{
				if (dir >= 6)
				{
					return null;
				}
				bb.clear();
				bb.limit(LodBufferBuilderFactory.FULL_SIZED_BUFFER);
				while (bb.hasRemaining() && dir < 6)
				{
					writeData();
				}
				bb.limit(bb.position());
				bb.rewind();
				return bb;
			}
			
			private void writeData()
			{
				int i = quad;
				for (; i < quads[dir].size(); i++)
				{
					if (!bb.hasRemaining())
					{
						break;
					}
					putQuad(bb, quads[dir].get(i));
				}
				
				if (i >= quads[dir].size())
				{
					quad = 0;
					dir++;
					dir = skipEmpty(dir);
				}
				else
				{
					quad = i;
				}
			}
		};
	}
	
	public interface BufferFiller
	{
		/** If true: more data needs to be filled */
		boolean fill(GLVertexBuffer vbo);
	}
	
	public BufferFiller makeBufferFiller(GpuUploadMethod method)
	{
		return new BufferFiller()
		{
			int dir = 0;
			int quad = 0;
			
			public boolean fill(GLVertexBuffer vbo)
			{
				if (dir >= 6)
				{
					vbo.setVertexCount(0);
					return false;
				}
				
				int numOfQuads = _countRemainingQuads();
				if (numOfQuads > LodBufferBuilderFactory.MAX_QUADS_PER_BUFFER)
					numOfQuads = LodBufferBuilderFactory.MAX_QUADS_PER_BUFFER;
				if (numOfQuads == 0)
				{
					vbo.setVertexCount(0);
					return false;
				}
				ByteBuffer bb = vbo.mapBuffer(numOfQuads * LodBufferBuilderFactory.QUADS_BYTE_SIZE, method,
						LodBufferBuilderFactory.FULL_SIZED_BUFFER);
				if (bb == null)
					throw new NullPointerException("mapBuffer returned null");
				bb.clear();
				bb.limit(numOfQuads * LodBufferBuilderFactory.QUADS_BYTE_SIZE);
				while (bb.hasRemaining() && dir < 6)
				{
					writeData(bb);
				}
				bb.rewind();
				vbo.unmapBuffer();
				vbo.setVertexCount(LodRenderer.ENABLE_IBO ? numOfQuads*4 : numOfQuads*6);
				return dir < 6;
			}
			
			private int _countRemainingQuads()
			{
				int a = quads[dir].size() - quad;
				for (int i = dir + 1; i < quads.length; i++)
				{
					a += quads[i].size();
				}
				return a;
			}
			
			private void writeData(ByteBuffer bb)
			{
				int startQ = quad;
				
				int i = startQ;
				for (i = startQ; i < quads[dir].size(); i++)
				{
					if (!bb.hasRemaining())
					{
						break;
					}
					putQuad(bb, quads[dir].get(i));
				}
				
				if (i >= quads[dir].size())
				{
					quad = 0;
					dir++;
					while (dir < 6 && quads[dir].isEmpty())
						dir++;
				}
				else
				{
					quad = i;
				}
			}
		};
	}
	
	
	
	public int getCurrentQuadsCount()
	{
		int i = 0;
		for (ArrayList<BufferQuad> qs : quads)
			i += qs.size();
		return i;
	}

	/** Returns how many Buffers will be needed to render everything in this builder. */
	public int getCurrentNeededVertexBufferCount()
	{
		return LodUtil.ceilDiv(getCurrentQuadsCount(), LodBufferBuilderFactory.MAX_QUADS_PER_BUFFER);
	}

}
