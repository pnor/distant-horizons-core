package com.seibel.lod.core.objects;

import com.seibel.lod.core.enums.config.GpuUploadMethod;
import com.seibel.lod.core.objects.lod.RegionPos;
import com.seibel.lod.core.objects.opengl.ComplexRenderRegion;
import com.seibel.lod.core.objects.opengl.LodQuadBuilder;
import com.seibel.lod.core.objects.opengl.SimpleRenderRegion;
import com.seibel.lod.core.render.LodRenderProgram;
import com.seibel.lod.core.util.StatsMap;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftRenderWrapper;

public abstract class RenderRegion implements AutoCloseable
{
	// target can be Null.
	// If return null, means all status updated without switching objects.
	@SuppressWarnings("resource")
	public static RenderRegion updateStatus(RenderRegion target, LodQuadBuilder builder, RegionPos regPos) {
		boolean useSimpleRegion = (builder.getCurrentNeededVertexBuffers() <= 6) || true;
		if ((target instanceof SimpleRenderRegion && !useSimpleRegion) ||
			target instanceof ComplexRenderRegion && useSimpleRegion) {
			target.close();
			target = null;
		}
		if (target == null) {
			return useSimpleRegion ?
					new SimpleRenderRegion(builder.getCurrentNeededVertexBuffers(), regPos)
					: new ComplexRenderRegion(regPos);
		}
		return null;
	}
	
	public abstract void uploadBuffers(LodQuadBuilder builder, GpuUploadMethod uploadMethod);
	public abstract boolean shouldRender(IMinecraftRenderWrapper renderer, boolean enableDirectionalCulling);
	public abstract void render(LodRenderProgram shaderProgram);
	public abstract void debugDumpStats(StatsMap statsMap);
	
	@Override
	public abstract void close();

	
	
	
}
