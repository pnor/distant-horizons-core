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

package com.seibel.lod.core.objects.opengl;

import com.seibel.lod.core.api.ApiShared;
import org.lwjgl.system.MemoryUtil;

import static org.lwjgl.opengl.GL11C.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL31.GL_COPY_WRITE_BUFFER;

/**
 * Represents a OpenGL Index Buffer Object.
 *
 * @author Cotex
 * @version 4-13-2022
 */
public class QuadIBO
{
	/**
	 * Datatype of the stored indices
	 * (can be GL_UNSIGNED_INT, GL_UNSIGNED_SHORT, GL_UNSIGNED_BYTE)
	 */
	public int type;
	
	/** OpenGL ID of this IBO object */
	int id;
	
	/** Current capacity (in quads) */
	int currentQuadCapacity;
	
	/** Global IBO object, used for sharing the IBO for any draw calls */
	public static QuadIBO GLOBAL = new QuadIBO();
	
	
	
	public QuadIBO()
	{
		id = glGenBuffers();
	}
	
	
	/** Should only be called on a thread that has a OpenGL context. */
	public void resizeIfNecessary(int newQuadCapacity)
	{
		//If the requested capacity is less than or equal to current capacity, ignore
		if (newQuadCapacity <= currentQuadCapacity)
			return;
		
		// create the new capacity bigger than necessary to prevent constant updates
		newQuadCapacity *= 1.5;
		ApiShared.LOGGER.info("Quad IBO Resizing from [" + currentQuadCapacity + "] to [" + newQuadCapacity + "]");
		
		currentQuadCapacity = newQuadCapacity;
		
		//TODO: DO DYNAMIC TYPES, just makes things more efficient
		type = GL_UNSIGNED_INT;
		int DT_SIZE = 4; //Datatype size (int: 4, short: 2, byte: 1)
		
		glBindBuffer(GL_COPY_WRITE_BUFFER, id);
		//Resize the buffer
		glBufferData(GL_COPY_WRITE_BUFFER, (long) DT_SIZE * 6 * newQuadCapacity, GL_STATIC_DRAW);// 4L is datatype
		//Map and write the index data to the buffer
		long arrayPointer = nglMapBuffer(GL_COPY_WRITE_BUFFER, GL_WRITE_ONLY);
		for (int base = 0; base < newQuadCapacity; base++)
		{
			// Add the new quad's indices
			MemoryUtil.memPutInt(arrayPointer + (base * 6L * DT_SIZE + DT_SIZE * 0), (int) (base * 4 + 0));
			MemoryUtil.memPutInt(arrayPointer + (base * 6L * DT_SIZE + DT_SIZE * 1), (int) (base * 4 + 1));
			MemoryUtil.memPutInt(arrayPointer + (base * 6L * DT_SIZE + DT_SIZE * 2), (int) (base * 4 + 2));
			MemoryUtil.memPutInt(arrayPointer + (base * 6L * DT_SIZE + DT_SIZE * 3), (int) (base * 4 + 2));
			MemoryUtil.memPutInt(arrayPointer + (base * 6L * DT_SIZE + DT_SIZE * 4), (int) (base * 4 + 3));
			MemoryUtil.memPutInt(arrayPointer + (base * 6L * DT_SIZE + DT_SIZE * 5), (int) (base * 4 + 0));
		}
		glUnmapBuffer(GL_COPY_WRITE_BUFFER);
	}
	
	/** Binds the IBO */
	public void bind()
	{
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, id);
	}
	
	/** Unbinds the IBO */
	public void unbind()
	{
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
	}
}
