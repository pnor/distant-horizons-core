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

package com.seibel.lod.core.objects.opengl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.google.common.collect.ImmutableList;

/**
 * A (almost) exact copy of Minecraft's
 * BufferBuilder object. <br>
 * Which allows for creating and filling
 * OpenGL buffers.
 * 
 * @author James Seibel
 * @version 12-9-2021
 */
public class LodBufferBuilder
{
	public ByteBuffer buffer;
	
	private int nextElementByte = 0;
	private int vertices;
	private LodVertexFormatElement currentElement;
	private int elementIndex;
	private LodVertexFormat format;
	private boolean building;
	
	public LodBufferBuilder(int bufferSizeInBytes)
	{
		this.buffer = allocateByteBuffer(bufferSizeInBytes);
	}
	
	public int getMemUsage() {return buffer.capacity();}
	
	/** originally from MC's GLAllocation class */
	private ByteBuffer allocateByteBuffer(int bufferSizeInBytes)
	{
		return ByteBuffer.allocateDirect(bufferSizeInBytes).order(ByteOrder.nativeOrder());
	}

	
	/** make sure the buffer doesn't overflow when inserting new elements */
	private void ensureVertexCapacity()
	{
		this.ensureCapacity(this.format.getByteSize());
	}
	private void ensureCapacity(int vertexSizeInBytes)
	{
		if (this.nextElementByte + vertexSizeInBytes > this.buffer.capacity())
		{
			int i = this.buffer.capacity();
			int j = roundUp((int) ((i + vertexSizeInBytes)*2));
			//LOGGER.debug("Needed to grow BufferBuilder buffer: Old size {} bytes, new size {} bytes.", i, j);
			ByteBuffer bytebuffer = allocateByteBuffer(j);
			this.buffer.position(0);
			bytebuffer.put(this.buffer);
			bytebuffer.rewind();
			this.buffer = bytebuffer;
		}
	}
	private void packBuffer() {
		int cap = this.buffer.capacity();
		int filled = this.format.getByteSize() * this.vertices;
		this.buffer.position(0);
		this.buffer.limit(filled);
		if (cap - filled > 4096) {
			ByteBuffer bytebuffer = allocateByteBuffer(filled);
			bytebuffer.put(this.buffer);
			bytebuffer.rewind();
			this.buffer = bytebuffer;
		}
		
	}
	
	private static int roundUp(int vertexSizeInBytes)
	{
		int i = 4096; // 4 KB (1 page)
		if (vertexSizeInBytes == 0)
		{
			return i;
		}
		else
		{
			if (vertexSizeInBytes < 0)
			{
				i *= -1;
			}
			
			int j = vertexSizeInBytes % i;
			return j == 0 ? vertexSizeInBytes : vertexSizeInBytes + i - j;
		}
	}

	private void switchFormat(LodVertexFormat newFormat)
	{
		format = newFormat;
	}
	
	
	
	
	//========================================//
	// methods for actually building a buffer //
	//========================================//
	
	/**
	 * @param openGlLodVertexFormat GL11.GL_QUADS, GL11.GL_TRIANGLES, etc.
	 * @param LodVertexFormat
	 */
	public void begin(int openGlLodVertexFormat, LodVertexFormat LodVertexFormat)
	{
		if (this.building)
		{
			throw new IllegalStateException("Already building!");
		}
		else
		{
			this.building = true;
			this.switchFormat(LodVertexFormat);
			this.currentElement = LodVertexFormat.getElements().get(0);
			this.elementIndex = 0;
			this.buffer.clear();
			this.vertices = 0;
		}
	}
	
	public void end()
	{
		if (!this.building)
		{
			return;
		} else {
			this.building = false;
			this.currentElement = null;
			this.elementIndex = 0;
			packBuffer();
		}
	}
	
	public void putByte(int index, byte newByte)
	{
		this.buffer.put(this.nextElementByte + index, newByte);
	}
	
	public void putShort(int index, short newShort)
	{
		this.buffer.putShort(this.nextElementByte + index, newShort);
	}
	
	public void putFloat(int index, float newFloat)
	{
		this.buffer.putFloat(this.nextElementByte + index, newFloat);
	}
	
	public void endVertex()
	{
		if (this.elementIndex != 0)
		{
			throw new IllegalStateException("Not filled all elements of the vertex");
		}
		else
		{
			++this.vertices;
			this.ensureVertexCapacity();
		}
	}
	
	public void nextElement()
	{
		ImmutableList<LodVertexFormatElement> immutablelist = this.format.getElements();
		this.elementIndex = (this.elementIndex + 1) % immutablelist.size();
		this.nextElementByte += this.currentElement.getByteSize();
		this.currentElement = immutablelist.get(this.elementIndex);
		if (currentElement.getIsPadding())
		{
			this.nextElement();
		}
		
//		if (this.defaultColorSet && this.currentElement.getUsage() == LodVertexFormatElement.Usage.COLOR)
//		{
//			color(this.defaultR, this.defaultG, this.defaultB, this.defaultA);
//		}
		
	}
	
	public LodBufferBuilder color(int red, int green, int blue, int alpha)
	{
		LodVertexFormatElement LodVertexFormatelement = this.currentElement();
		if (LodVertexFormatelement.getType() != LodVertexFormatElement.DataType.UBYTE)
		{
			throw new IllegalStateException("Color must be stored as a UBYTE");
		}
		else
		{
			this.putByte(0, (byte) red);
			this.putByte(1, (byte) green);
			this.putByte(2, (byte) blue);
			this.putByte(3, (byte) alpha);
			this.nextElement();
			return this;
		}
	}
	
	public LodBufferBuilder minecraftLightValue(byte lightValue)
	{
		LodVertexFormatElement LodVertexFormatelement = this.currentElement();
		if (LodVertexFormatelement.getType() != LodVertexFormatElement.DataType.UBYTE)
		{
			throw new IllegalStateException("Light Color must be stored as a UBYTE");
		}
		else
		{
			this.putByte(0, lightValue);
			this.nextElement();
			return this;
		}
	}
	
	public LodBufferBuilder position(float x, float y, float z)
	{
		if (this.currentElement().getType() != LodVertexFormatElement.DataType.FLOAT)
		{
			throw new IllegalStateException("Position verticies must be stored as a FLOAT");
		}
		else
		{
			this.putFloat(0, x);
			this.putFloat(4, y);
			this.putFloat(8, z);
			this.nextElement();
			return this;
		}
	}
	
	public ByteBuffer getCleanedByteBuffer()
	{
		return this.buffer;
	}
	
	public void reset()
	{
		this.nextElementByte = 0;
		this.vertices = 0;
		this.buffer.clear();
	}
	
	public LodVertexFormatElement currentElement()
	{
		if (this.currentElement == null)
		{
			throw new IllegalStateException("BufferBuilder not started");
		}
		else
		{
			return this.currentElement;
		}
	}
	
	public boolean building()
	{
		return this.building;
	}
	
	//==================//
	// internal classes //
	//==================//
	
	public static final class DrawState
	{
		private final LodVertexFormat format;
		private final int vertexCount;
		private final int mode;
		
		private DrawState(LodVertexFormat p_i225905_1_, int p_i225905_2_, int p_i225905_3_)
		{
			this.format = p_i225905_1_;
			this.vertexCount = p_i225905_2_;
			this.mode = p_i225905_3_;
		}
		
		public LodVertexFormat format()
		{
			return this.format;
		}
		
		public int vertexCount()
		{
			return this.vertexCount;
		}
		
		public int mode()
		{
			return this.mode;
		}
	}
	
	public LodVertexFormat getLodVertexFormat()
	{
		return this.format;
	}
}