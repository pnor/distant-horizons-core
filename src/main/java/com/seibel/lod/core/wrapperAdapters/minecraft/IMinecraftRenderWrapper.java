package com.seibel.lod.core.wrapperAdapters.minecraft;

import java.util.HashSet;

import com.seibel.lod.core.objects.math.Mat4f;
import com.seibel.lod.core.objects.math.Vec3d;
import com.seibel.lod.core.objects.math.Vec3f;
import com.seibel.lod.core.wrapperAdapters.block.AbstractBlockPosWrapper;
import com.seibel.lod.core.wrapperAdapters.chunk.AbstractChunkPosWrapper;

/**
 * Contains everything related to
 * rendering in Minecraft.
 * 
 * @author James Seibel
 * @version 11-18-2021
 */
public interface IMinecraftRenderWrapper
{
	public Vec3f getLookAtVector();
	
	public AbstractBlockPosWrapper getCameraBlockPosition();
	
	public boolean playerHasBlindnessEffect();
	
	public Vec3d getCameraExactPosition();
	
	public Mat4f getDefaultProjectionMatrix(float partialTicks);
	
	public double getGamma();
	
	public double getFov(float partialTicks);
	
	/** Measured in chunks */
	public int getRenderDistance();
	
	public int getScreenWidth();
	public int getScreenHeight();
	
	/**
	 * This method returns the ChunkPos of all chunks that Minecraft
	 * is going to render this frame.
	 */
	public HashSet<AbstractChunkPosWrapper> getRenderedChunks();
}
