package com.seibel.lod.objects.opengl;

import org.lwjgl.opengl.GL15;

import com.seibel.lod.enums.rendering.GlProxyContext;
import com.seibel.lod.render.GlProxy;

/**
 * a (almost) exact copy of MC's
 * VertexBuffer object.
 * 
 * @author James Seibel
 * @version 11-13-2021
 */
public class LodVertexBuffer implements AutoCloseable
{
	public int id;
	public int vertexCount;
	
	public LodVertexBuffer()
	{
		if (GlProxy.getInstance().getGlContext() == GlProxyContext.NONE)
			throw new IllegalStateException("Thread [" +Thread.currentThread().getName() + "] tried to create a [" + LodVertexBuffer.class.getSimpleName() + "] outside a OpenGL contex.");
		
		this.id = GL15.glGenBuffers();
	}
	
	
	@Override
	public void close()
	{
		if (this.id >= 0)
		{
			if (GlProxy.getInstance().getGlContext() == GlProxyContext.NONE)
				throw new IllegalStateException("Thread [" +Thread.currentThread().getName() + "] tried to close the [" + LodVertexBuffer.class.getSimpleName() + "] with id [" + this.id + "] outside a OpenGL contex.");
			
			GL15.glDeleteBuffers(this.id);
			this.id = -1;
		}
	}
}