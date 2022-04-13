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

import java.nio.ByteBuffer;

import com.seibel.lod.core.api.ApiShared;
import com.seibel.lod.core.util.UnitBytes;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL44;

import com.seibel.lod.core.enums.config.GpuUploadMethod;
import com.seibel.lod.core.enums.rendering.GLProxyContext;
import com.seibel.lod.core.render.GLProxy;

/**
 * This is a container for a OpenGL
 * VBO (Vertex Buffer Object).
 * 
 * @author James Seibel
 * @version 11-20-2021
 */
public class LodVertexBuffer implements AutoCloseable
{
	/**
	 * When uploading to a buffer that is too small, recreate it this many times
	 * bigger than the upload payload
	 */
	public static final double BUFFER_EXPANSION_MULTIPLIER = 1.3;
	public static int count = 0;
	public int id;
	public int vertexCount;
	public boolean isBufferStorage;
	public long size = 0;
	
	public LodVertexBuffer(boolean isBufferStorage)
	{
		_create(isBufferStorage);
	}
	
	private void _create(boolean asBufferStorage) {
		if (GLProxy.getInstance().getGlContext() == GLProxyContext.NONE)
			throw new IllegalStateException("Thread [" +Thread.currentThread().getName() + "] tried to create a [" + LodVertexBuffer.class.getSimpleName() + "] outside a OpenGL contex.");
		this.id = GL32.glGenBuffers();
		this.isBufferStorage = asBufferStorage;
		count++;
	}
	
	private void _destroy() {
		if (GLProxy.getInstance().getGlContext() == GLProxyContext.PROXY_WORKER) {
			 GL32.glDeleteBuffers(this.id);
		} else {
			final int id = this.id;
			GLProxy.getInstance().recordOpenGlCall(() -> GL32.glDeleteBuffers(id));
		}
		this.id = -1;
		size = 0;
		vertexCount = 0;
		count--;
	}
	
	private void _uploadBufferStorage(ByteBuffer bb) {
		if (!isBufferStorage) throw new IllegalStateException("Buffer is not bufferStorage but its trying to use bufferStorage upload method!");
		int bbSize = bb.limit() - bb.position();
		GL32.glDeleteBuffers(id);
		id = GL32.glGenBuffers();
		GL32.glBindBuffer(GL32.GL_ARRAY_BUFFER, id);
		GL44.glBufferStorage(GL32.GL_ARRAY_BUFFER, bb, 0);
		size = bbSize;
	}

	// bufferData
	// simplest/most compatible
	private void _uploadData(ByteBuffer bb) {
		if (isBufferStorage) throw new IllegalStateException("Buffer is bufferStorage but its trying to use Data upload method!");
		int bbSize = bb.limit() - bb.position();
		GL32.glBindBuffer(GL32.GL_ARRAY_BUFFER, id);
		GL32.glBufferData(GL32.GL_ARRAY_BUFFER, bb, GL32.GL_STATIC_DRAW);
		size = bbSize;
	}

	// bufferSubData
	// less stutter, low GPU usage?
	private void _uploadSubData(ByteBuffer bb, int maxExpensionSize) {
		if (isBufferStorage) throw new IllegalStateException("Buffer is bufferStorage but its trying to use SubData upload method!");
		int bbSize = bb.limit() - bb.position();
		GL32.glBindBuffer(GL32.GL_ARRAY_BUFFER, id);
		if (size < bbSize || size > bbSize * BUFFER_EXPANSION_MULTIPLIER * BUFFER_EXPANSION_MULTIPLIER) {
			int newSize = (int) (bbSize * BUFFER_EXPANSION_MULTIPLIER);
			if (newSize > maxExpensionSize) newSize = maxExpensionSize;
			GL32.glBufferData(GL32.GL_ARRAY_BUFFER, newSize, GL32.GL_STATIC_DRAW);
			size = newSize;
		}
		GL32.glBufferSubData(GL32.GL_ARRAY_BUFFER, 0, bb);
	}
	
	public void uploadBuffer(ByteBuffer bb, int vertCount, GpuUploadMethod uploadMethod, int maxExpensionSize) {
		if (vertCount < 0) throw new IllegalArgumentException("VertCount is negative!");
		if (uploadMethod.useEarlyMapping)
			throw new IllegalArgumentException("UploadMethod signal that this should use Mapping instead of uploadBuffer!");
		vertexCount = vertCount;
		int bbSize = bb.limit()-bb.position();
		if (bbSize > maxExpensionSize)
			throw new IllegalArgumentException("maxExpensionSize is "+maxExpensionSize+" but buffer size is "+bbSize+"!");
		GLProxy.GL_LOGGER.debug("Uploading {} buffer with {} vertices.", new UnitBytes(bbSize), vertCount);
		// If size is zero, just ignore it.
		if (bbSize == 0) return;
		boolean useBuffStorage = uploadMethod.useBufferStorage;
		if (useBuffStorage != isBufferStorage) {
			_destroy();
			_create(useBuffStorage);
		}
		switch (uploadMethod) {
		case AUTO:
			throw new IllegalArgumentException("GpuUploadMethod AUTO must be resolved before call to uploadBuffer()!");
		case BUFFER_STORAGE:
			_uploadBufferStorage(bb);
			break;
		case DATA:
			_uploadData(bb);
			break;
		case SUB_DATA:
			_uploadSubData(bb, maxExpensionSize);
			break;
		default:
			throw new IllegalArgumentException("Invalid GpuUploadMethod enum");
		}
	}
	
	
	@Override
	public void close()
	{
		if (this.id >= 0)
		{
			_destroy();
			if (count==0) ApiShared.LOGGER.info("All LodVerrtexBuffer is freed.");
		} else {
			ApiShared.LOGGER.error("LodVertexBuffer double close!");
			
		}
	}
	private boolean isMapped = false;

	public ByteBuffer mapBuffer(int targetSize, GpuUploadMethod uploadMethod, int maxExpensionSize)
	{
		if (targetSize == 0) throw new IllegalArgumentException("MapBuffer targetSize is 0!");
		if (!uploadMethod.useEarlyMapping) throw new IllegalStateException("Upload method must be one that use mappings in order to call mapBuffer!");
		if (isMapped) throw new IllegalStateException("Map Buffer called but buffer is already mapped!");
		boolean useBuffStorage = uploadMethod.useBufferStorage;
		if (useBuffStorage != isBufferStorage) {
			_destroy();
			_create(useBuffStorage);
		}
		
		ByteBuffer vboBuffer;

		GL32.glBindBuffer(GL32.GL_ARRAY_BUFFER, id);

		if (size < targetSize || size > targetSize * BUFFER_EXPANSION_MULTIPLIER * BUFFER_EXPANSION_MULTIPLIER) {
			int newSize = (int) (targetSize * BUFFER_EXPANSION_MULTIPLIER);
			if (newSize > maxExpensionSize) newSize = maxExpensionSize;
			size = newSize;
			if (uploadMethod.useBufferStorage) {
				GL32.glDeleteBuffers(id);
				id = GL32.glGenBuffers();
				GL32.glBindBuffer(GL32.GL_ARRAY_BUFFER, id);
				GL44.glBufferStorage(GL32.GL_ARRAY_BUFFER, newSize, GL44.GL_MAP_WRITE_BIT);
			} else {
				GL32.glBufferData(GL32.GL_ARRAY_BUFFER, newSize, GL32.GL_STATIC_DRAW);
			}
		}
		
		vboBuffer = GL32.glMapBufferRange(GL32.GL_ARRAY_BUFFER, 0, targetSize,
				GL32.GL_MAP_WRITE_BIT | GL32.GL_MAP_UNSYNCHRONIZED_BIT | GL32.GL_MAP_INVALIDATE_BUFFER_BIT);
		isMapped = true;
		return vboBuffer;
	}

	public void unmapBuffer(GpuUploadMethod uploadMethod)
	{
		if (!uploadMethod.useEarlyMapping) throw new IllegalStateException("Upload method must be one that use mappings in order to call unmapBuffer!");
		if (!isMapped) throw new IllegalStateException("Unmap Buffer called but buffer is already not mapped!");
		GL32.glBindBuffer(GL32.GL_ARRAY_BUFFER, id);
		GL32.glUnmapBuffer(GL32.GL_ARRAY_BUFFER);
		isMapped = false;
	}

	@Override
	public String toString() {
		return (isBufferStorage ? "VertexBufferStorage" : "BufferStorage")+
				"[vboId:"+id+", size:"+size+", vertCount:"+vertexCount+(isMapped?", MAPPED" : "")+"]";
	}
}