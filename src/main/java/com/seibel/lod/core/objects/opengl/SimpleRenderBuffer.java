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
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import com.seibel.lod.core.api.ClientApi;
import com.seibel.lod.core.builders.lodBuilding.bufferBuilding.LodQuadBuilder;
import com.seibel.lod.core.render.LodRenderer;
import com.seibel.lod.core.render.objects.GLVertexBuffer;
import org.lwjgl.opengl.GL32;

import com.seibel.lod.core.api.ApiShared;
import com.seibel.lod.core.builders.lodBuilding.bufferBuilding.LodBufferBuilderFactory;
import com.seibel.lod.core.enums.config.GpuUploadMethod;
import com.seibel.lod.core.handlers.dependencyInjection.SingletonHandler;
import com.seibel.lod.core.builders.lodBuilding.bufferBuilding.LodQuadBuilder.BufferFiller;
import com.seibel.lod.core.render.GLProxy;
import com.seibel.lod.core.render.LodRenderProgram;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.util.StatsMap;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton;

import static com.seibel.lod.core.render.GLProxy.GL_LOGGER;

public class SimpleRenderBuffer extends RenderBuffer
{
	private static final ILodConfigWrapperSingleton CONFIG = SingletonHandler.get(ILodConfigWrapperSingleton.class);
	private static final long MAX_BUFFER_UPLOAD_TIMEOUT_NANOSECONDS = 1_000_000;

	GLVertexBuffer[] vbos;
	
	// public void onReuse() {}
	
	public SimpleRenderBuffer() {
		vbos = new GLVertexBuffer[0];
	}
	
	@Override
	protected boolean uploadBuffers(LodQuadBuilder builder, GpuUploadMethod method)
	{
		// if (builder.getCurrentNeededVertexBuffers()>6) return false;

		if (method.useEarlyMapping) {
			_uploadBuffersMapped(builder, method);
		} else {
			_uploadBuffersDirect(builder, method);
		}
		return true;
	}
	
	// public void onSwapToFront() {}
	// public void onSwapToBack() {}

	@Override
	public boolean render(LodRenderProgram shaderProgram)
	{
		boolean hasRendered = false;
		for (GLVertexBuffer vbo : vbos) {
			if (vbo == null) continue;
			if (vbo.getVertexCount() == 0) continue;
			hasRendered = true;
			vbo.bind();
			shaderProgram.bindVertexBuffer(vbo.getId());
			if (LodRenderer.ENABLE_IBO) {
				GL32.glDrawElements(GL32.GL_TRIANGLES, (vbo.getVertexCount()/4)*6, ClientApi.renderer.quadIBO.getType(), 0);
			} else {
				GL32.glDrawArrays(GL32.GL_TRIANGLES, 0, vbo.getVertexCount());
			}
			//LodRenderer.tickLogger.info("Vertex buffer: {}", vbo);
		}
		return hasRendered;
	}
	
	@Override
	public void debugDumpStats(StatsMap statsMap)
	{
		statsMap.incStat("RenderBuffers");
		statsMap.incStat("SimpleRenderBuffers");
		for (GLVertexBuffer b : vbos) {
			if (b == null) continue;
			statsMap.incStat("VBOs");
			if (b.getSize() == LodBufferBuilderFactory.FULL_SIZED_BUFFER) {
				statsMap.incStat("FullsizedVBOs");
			}
			if (b.getSize() == 0) GL_LOGGER.warn("VBO with size 0");
			statsMap.incBytesStat("TotalUsage", b.getSize());
		}
	}
	
	@Override
	public void close()
	{
		GLProxy.getInstance().recordOpenGlCall(() -> {
			for (GLVertexBuffer b : vbos) {
				b.destroy(false);
			}
		});
	}
	
	private void _uploadBuffersDirect(LodQuadBuilder builder, GpuUploadMethod method) {
		resize(builder.getCurrentNeededVertexBufferCount());
		long remainingNS = 0;
		long BPerNS = CONFIG.client().advanced().buffers().getGpuUploadPerMegabyteInMilliseconds();
		
		int i = 0;
		Iterator<ByteBuffer> iter = builder.makeVertexBuffers();
		while (iter.hasNext()) {
			if (i >= vbos.length) {
				throw new RuntimeException("Too many vertex buffers!!");
			}
			ByteBuffer bb = iter.next();
			GLVertexBuffer vbo = getOrMakeVbo(i++, method.useBufferStorage);
			int size = bb.limit() - bb.position();
			try {
				vbo.bind();
				vbo.uploadBuffer(bb, size/LodUtil.LOD_VERTEX_FORMAT.getByteSize(), method, LodBufferBuilderFactory.FULL_SIZED_BUFFER);
			} catch (Exception e) {
				vbos[i-1] = null;
				vbo.close();
				ApiShared.LOGGER.error("Failed to upload buffer: ", e);
			}
			if (BPerNS<=0) continue;
			// upload buffers over an extended period of time
			// to hopefully prevent stuttering.
			remainingNS += size * BPerNS;
			if (remainingNS >= TimeUnit.NANOSECONDS.convert(1000 / 60, TimeUnit.MILLISECONDS)) {
				if (remainingNS > MAX_BUFFER_UPLOAD_TIMEOUT_NANOSECONDS)
					remainingNS = MAX_BUFFER_UPLOAD_TIMEOUT_NANOSECONDS;
				try {
					Thread.sleep(remainingNS / 1000000, (int) (remainingNS % 1000000));
				} catch (InterruptedException e) {
				}
				remainingNS = 0;
			}
		}
		if (i < vbos.length) {
			throw new RuntimeException("Too few vertex buffers!!");
		}
	}

	private void _uploadBuffersMapped(LodQuadBuilder builder, GpuUploadMethod method)
	{
		resize(builder.getCurrentNeededVertexBufferCount());
		for (int i=0; i<vbos.length; i++) {
			if (vbos[i]==null) vbos[i] = new GLVertexBuffer(method.useBufferStorage);
		}
		BufferFiller func = builder.makeBufferFiller(method);
		int i = 0;
		while (i < vbos.length && func.fill(vbos[i++])) {}
	}

	private GLVertexBuffer getOrMakeVbo(int iIndex, boolean useBuffStorage) {
		if (vbos[iIndex] == null) {
			vbos[iIndex] = new GLVertexBuffer(useBuffStorage);
		}
		return vbos[iIndex];
	}
	
	private void resize(int size) {
		if (vbos.length != size) {
			GLVertexBuffer[] newVbos = new GLVertexBuffer[size];
			if (vbos.length > size) {
				for (int i=size; i<vbos.length; i++) {
					if (vbos[i]!=null) vbos[i].close();
					vbos[i] = null;
				}
			}
			for (int i=0; i<newVbos.length && i<vbos.length; i++) {
				newVbos[i] = vbos[i];
				vbos[i] = null;
			}
			for (GLVertexBuffer b : vbos) {
				if (b != null) throw new RuntimeException("LEAKING VBO!");
			}
			vbos = newVbos;
		}
	}
}
