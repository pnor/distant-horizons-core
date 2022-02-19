package com.seibel.lod.core.objects.opengl;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import org.lwjgl.opengl.GL32;

import com.seibel.lod.core.builders.bufferBuilding.LodBufferBuilderFactory;
import com.seibel.lod.core.enums.config.GpuUploadMethod;
import com.seibel.lod.core.objects.RenderRegion;
import com.seibel.lod.core.objects.lod.RegionPos;
import com.seibel.lod.core.objects.opengl.LodQuadBuilder.BufferFiller;
import com.seibel.lod.core.render.LodRenderProgram;
import com.seibel.lod.core.render.RenderUtil;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.util.SingletonHandler;
import com.seibel.lod.core.util.StatsMap;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftRenderWrapper;

public class SimpleRenderRegion extends RenderRegion {
	LodVertexBuffer[] vbos;
	final RegionPos regPos;
	private static final ILodConfigWrapperSingleton CONFIG = SingletonHandler.get(ILodConfigWrapperSingleton.class);
	private static final int FULL_SIZED_BUFFERS =
			LodBufferBuilderFactory.MAX_TRIANGLES_PER_BUFFER * LodUtil.LOD_VERTEX_FORMAT.getByteSize() * 3;
	
	public SimpleRenderRegion(int size, RegionPos pos) {
		vbos = new LodVertexBuffer[size];
		regPos = pos;
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
	
	public LodVertexBuffer[] debugGetBuffers() {
		return vbos;
	} 

	@Override
	public void close() {
		for (LodVertexBuffer b : vbos) {
			if (b != null) b.close();
		}
		vbos = new LodVertexBuffer[0];
	}

	private LodVertexBuffer getOrMakeVbo(int iIndex, boolean useBuffStorage) {
		if (vbos[iIndex] == null) {
			vbos[iIndex] = new LodVertexBuffer(useBuffStorage);
		}
		return vbos[iIndex];
	}
	
	private void uploadBuffersViaMapping(LodQuadBuilder builder, GpuUploadMethod uploadMethod)
	{
		resize(builder.getCurrentNeededVertexBuffers());
		for (int i=0; i<vbos.length; i++) {
			if (vbos[i]==null) vbos[i] = new LodVertexBuffer(uploadMethod.useBufferStorage);
		}
		
		BufferFiller func = builder.makeBufferFiller(uploadMethod);
		int i = 0;
		while (i < vbos.length && func.fill(vbos[i++])) {}
	}

	@Override
	public void uploadBuffers(LodQuadBuilder builder, GpuUploadMethod uploadMethod)
	{
		if (uploadMethod.useEarlyMapping) {
			uploadBuffersViaMapping(builder, uploadMethod);
			return;
		}
		resize(builder.getCurrentNeededVertexBuffers());
		long remainingNS = 0;
		long BPerNS = CONFIG.client().advanced().buffers().getGpuUploadPerMegabyteInMilliseconds();
		
		int i = 0;
		Iterator<ByteBuffer> iter = builder.makeVertexBuffers();
		while (iter.hasNext()) {
			ByteBuffer bb = iter.next();
			LodVertexBuffer vbo = getOrMakeVbo(i++, uploadMethod.useBufferStorage);
			int size = bb.limit() - bb.position();
			vbo.uploadBuffer(bb, size/LodUtil.LOD_VERTEX_FORMAT.getByteSize(), uploadMethod, FULL_SIZED_BUFFERS);
			// upload buffers over an extended period of time
			// to hopefully prevent stuttering.
			remainingNS += size * BPerNS;
			if (remainingNS >= TimeUnit.NANOSECONDS.convert(1000 / 60, TimeUnit.MILLISECONDS)) {
				if (remainingNS > LodBufferBuilderFactory.MAX_BUFFER_UPLOAD_TIMEOUT_NANOSECONDS)
					remainingNS = LodBufferBuilderFactory.MAX_BUFFER_UPLOAD_TIMEOUT_NANOSECONDS;
				try {
					Thread.sleep(remainingNS / 1000000, (int) (remainingNS % 1000000));
				} catch (InterruptedException e) {
				}
				remainingNS = 0;
			}
		}
	}
	
	@Override
	public boolean shouldRender(IMinecraftRenderWrapper renderer, boolean enableDirectionalCulling) {
		if (enableDirectionalCulling && !RenderUtil.isRegionInViewFrustum(renderer.getCameraBlockPosition(),
				renderer.getLookAtVector(), regPos.x, regPos.z)) return false;
		return true;
	}

	@Override
	public void render(LodRenderProgram shaderProgram)
	{
		for (LodVertexBuffer vbo : vbos) {
			if (vbo == null) continue;
			if (vbo.vertexCount == 0) continue;
			GL32.glBindBuffer(GL32.GL_ARRAY_BUFFER, vbo.id);
			shaderProgram.bindVertexBuffer(vbo.id);
			GL32.glDrawArrays(GL32.GL_TRIANGLES, 0, vbo.vertexCount);
		}
		
	}

	@Override
	public void debugDumpStats(StatsMap statsMap)
	{
		statsMap.incStat("RegionRegions");
		statsMap.incStat("SimpleRegionRegions");
		for (LodVertexBuffer b : vbos) {
			if (b == null) continue;
			statsMap.incStat("Buffers");
			if (b.size == FULL_SIZED_BUFFERS) {
				statsMap.incStat("FullsizedBuffers");
			}
			statsMap.incBytesStat("TotalUsage", b.size);
		}
	}

}
