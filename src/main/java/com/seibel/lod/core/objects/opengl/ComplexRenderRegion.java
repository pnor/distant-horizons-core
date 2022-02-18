package com.seibel.lod.core.objects.opengl;

import java.util.TreeMap;

import org.lwjgl.opengl.GL32;

import com.seibel.lod.core.builders.bufferBuilding.LodBufferBuilderFactory;
import com.seibel.lod.core.builders.lodBuilding.LodBuilder;
import com.seibel.lod.core.enums.config.GpuUploadMethod;
import com.seibel.lod.core.objects.RenderRegion;
import com.seibel.lod.core.objects.lod.RegionPos;
import com.seibel.lod.core.objects.math.Mat4f;
import com.seibel.lod.core.objects.math.Vec3d;
import com.seibel.lod.core.render.LodRenderProgram;
import com.seibel.lod.core.render.RenderUtil;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.util.StatsMap;
import com.seibel.lod.core.util.UnitBytes;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftRenderWrapper;

public class ComplexRenderRegion extends RenderRegion {
	LodVertexBuffer[] vbos;
	final RegionPos regPos;
	private static final float FULL_SIZED_BUFFERS =
			LodBufferBuilderFactory.MAX_TRIANGLES_PER_BUFFER * LodUtil.LOD_VERTEX_FORMAT.getByteSize();
	
	public ComplexRenderRegion(RegionPos pos) {
		vbos = new LodVertexBuffer[1];
		regPos = pos;
	}
	
	public void resize(int size) {
		if (vbos.length != size) {
			LodVertexBuffer[] newVbos = new LodVertexBuffer[size];
			if (vbos.length > size) {
				for (int i=size; i<vbos.length; i++) {
					vbos[i].close();
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
		
		
	}

	public LodVertexBuffer getOrMakeVbo(int iIndex, boolean useBuffStorage) {
		if (vbos[iIndex] == null) {
			vbos[iIndex] = new LodVertexBuffer(useBuffStorage);
		} else if (vbos[iIndex].isBufferStorage != useBuffStorage) {
			vbos[iIndex].close();
			vbos[iIndex] = new LodVertexBuffer(useBuffStorage);
		}
		return vbos[iIndex];
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

	@Override
	public void uploadBuffers(LodQuadBuilder builder, GpuUploadMethod uploadMethod)
	{
		// TODO Auto-generated method stub
		
	}

}
