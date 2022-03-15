package com.seibel.lod.core.objects.opengl;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import com.seibel.lod.core.render.LodRenderer;
import com.seibel.lod.core.util.SpamReducedLogger;
import org.lwjgl.opengl.GL32;

import com.seibel.lod.core.api.ApiShared;
import com.seibel.lod.core.builders.bufferBuilding.LodBufferBuilderFactory;
import com.seibel.lod.core.enums.config.GpuUploadMethod;
import com.seibel.lod.core.handlers.dependencyInjection.SingletonHandler;
import com.seibel.lod.core.objects.opengl.LodQuadBuilder.BufferFiller;
import com.seibel.lod.core.render.GLProxy;
import com.seibel.lod.core.render.LodRenderProgram;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.util.StatsMap;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton;

public class SimpleRenderBuffer extends RenderBuffer
{
	private static final ILodConfigWrapperSingleton CONFIG = SingletonHandler.get(ILodConfigWrapperSingleton.class);
	private static final int FULL_SIZED_BUFFERS =
			LodBufferBuilderFactory.MAX_TRIANGLES_PER_BUFFER * LodUtil.LOD_VERTEX_FORMAT.getByteSize() * 3;
	private static final long MAX_BUFFER_UPLOAD_TIMEOUT_NANOSECONDS = 1_000_000;
	
	LodVertexBuffer[] vbos;
	
	// public void onReuse() {}
	
	public SimpleRenderBuffer() {
		vbos = new LodVertexBuffer[0];
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
		for (LodVertexBuffer vbo : vbos) {
			if (vbo == null) continue;
			if (vbo.vertexCount == 0) continue;
			hasRendered = true;
			GL32.glBindBuffer(GL32.GL_ARRAY_BUFFER, vbo.id);
			shaderProgram.bindVertexBuffer(vbo.id);
			GL32.glDrawArrays(GL32.GL_TRIANGLES, 0, vbo.vertexCount);
			LodRenderer.tickLogger.info("Vertex buffer: {}", vbo);
		}
		return hasRendered;
	}
	
	@Override
	public void debugDumpStats(StatsMap statsMap)
	{
		statsMap.incStat("RenderBuffers");
		statsMap.incStat("SimpleRenderBuffers");
		for (LodVertexBuffer b : vbos) {
			if (b == null) continue;
			statsMap.incStat("VBOs");
			if (b.size == FULL_SIZED_BUFFERS) {
				statsMap.incStat("FullsizedVBOs");
			}
			statsMap.incBytesStat("TotalUsage", b.size);
		}
	}
	
	@Override
	public void close()
	{
		GLProxy.getInstance().recordOpenGlCall(() -> {
			for (LodVertexBuffer b : vbos) {
				b.close();
			}
		});
	}
	
	private void _uploadBuffersDirect(LodQuadBuilder builder, GpuUploadMethod method) {
		resize(builder.getCurrentNeededVertexBuffers());
		long remainingNS = 0;
		long BPerNS = CONFIG.client().advanced().buffers().getGpuUploadPerMegabyteInMilliseconds();
		
		int i = 0;
		Iterator<ByteBuffer> iter = builder.makeVertexBuffers();
		while (iter.hasNext()) {
			ByteBuffer bb = iter.next();
			LodVertexBuffer vbo = getOrMakeVbo(i++, method.useBufferStorage);
			int size = bb.limit() - bb.position();
			try {
				vbo.uploadBuffer(bb, size/LodUtil.LOD_VERTEX_FORMAT.getByteSize(), method, FULL_SIZED_BUFFERS);
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
	}

	private void _uploadBuffersMapped(LodQuadBuilder builder, GpuUploadMethod method)
	{
		resize(builder.getCurrentNeededVertexBuffers());
		for (int i=0; i<vbos.length; i++) {
			if (vbos[i]==null) vbos[i] = new LodVertexBuffer(method.useBufferStorage);
		}
		BufferFiller func = builder.makeBufferFiller(method);
		int i = 0;
		while (i < vbos.length && func.fill(vbos[i++])) {}
	}

	private LodVertexBuffer getOrMakeVbo(int iIndex, boolean useBuffStorage) {
		if (vbos[iIndex] == null) {
			vbos[iIndex] = new LodVertexBuffer(useBuffStorage);
		}
		return vbos[iIndex];
	}
	
	private void resize(int size) {
		if (vbos.length != size) {
			LodVertexBuffer[] newVbos = new LodVertexBuffer[size];
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
			for (LodVertexBuffer b : vbos) {
				if (b != null) throw new RuntimeException("LEAKING VBO!");
			}
			vbos = newVbos;
		}
	}
}
