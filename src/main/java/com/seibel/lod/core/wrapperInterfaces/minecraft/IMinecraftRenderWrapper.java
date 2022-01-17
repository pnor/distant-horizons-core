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

package com.seibel.lod.core.wrapperInterfaces.minecraft;

import java.awt.Color;
import java.util.HashSet;

import com.seibel.lod.core.api.ModAccessorApi;
import com.seibel.lod.core.objects.math.Mat4f;
import com.seibel.lod.core.objects.math.Vec3d;
import com.seibel.lod.core.objects.math.Vec3f;
import com.seibel.lod.core.util.SingletonHandler;
import com.seibel.lod.core.wrapperInterfaces.IWrapperFactory;
import com.seibel.lod.core.wrapperInterfaces.block.AbstractBlockPosWrapper;
import com.seibel.lod.core.wrapperInterfaces.chunk.AbstractChunkPosWrapper;
import com.seibel.lod.core.wrapperInterfaces.modAccessor.ISodiumAccessor;

/**
 * Contains everything related to
 * rendering in Minecraft.
 * 
 * @author James Seibel
 * @version 12-14-2021
 */
public interface IMinecraftRenderWrapper
{
	Vec3f getLookAtVector();
	
	AbstractBlockPosWrapper getCameraBlockPosition();
	
	boolean playerHasBlindnessEffect();
	
	Vec3d getCameraExactPosition();
	
	Mat4f getDefaultProjectionMatrix(float partialTicks);
	
	double getGamma();
	
	Color getFogColor(float partialTicks);
	
	public default Color getUnderWaterFogColor(float partialTicks) {return getFogColor(partialTicks);}

	public default boolean isFogStateInUnderWater() {
		return false;
	}
	
	Color getSkyColor();
	
	double getFov(float partialTicks);
	
	/** Measured in chunks */
	int getRenderDistance();
	
	int getScreenWidth();
	int getScreenHeight();
	
	/**
	 * This method returns the ChunkPos of all chunks that Minecraft
	 * is going to render this frame.
	 * <br>
	 * If not implemented this calls {@link #getMaximumRenderedChunks()}.
	 */
	public default HashSet<AbstractChunkPosWrapper> getVanillaRenderedChunks()
	{
		ISodiumAccessor sodium = ModAccessorApi.get(ISodiumAccessor.class);
		return sodium==null ? getMaximumRenderedChunks() : sodium.getNormalRenderedChunks();
	}
	
	/**
	 * <strong>Doesn't need to be implemented.</strong> <br>
	 * Returns every chunk position within the vanilla render distance.
	 */
	public default HashSet<AbstractChunkPosWrapper> getMaximumRenderedChunks()
	{
		IMinecraftWrapper mcWrapper = SingletonHandler.get(IMinecraftWrapper.class);
		IWrapperFactory factory = SingletonHandler.get(IWrapperFactory.class);
		
		int chunkRenderDist = this.getRenderDistance();
		
		AbstractChunkPosWrapper centerChunkPos = mcWrapper.getPlayerChunkPos();
		int startChunkX = centerChunkPos.getX() - chunkRenderDist;
		int startChunkZ = centerChunkPos.getZ() - chunkRenderDist;
		
		// add every position within render distance
		HashSet<AbstractChunkPosWrapper> renderedPos = new HashSet<AbstractChunkPosWrapper>();
		for (int chunkX = 0; chunkX < (chunkRenderDist * 2+1); chunkX++)
		{
			for(int chunkZ = 0; chunkZ < (chunkRenderDist * 2+1); chunkZ++)
			{
				renderedPos.add(factory.createChunkPos(startChunkX + chunkX, startChunkZ + chunkZ));
			}
		}
		
		return renderedPos;	
	}
	
	/** @returns null if there was a issue getting the lightmap */
	int[] getLightmapPixels();

	/** @returns -1 if there was an issue getting the lightmap */
	int getLightmapTextureHeight();
	/** @returns -1 if there was an issue getting the lightmap */
	int getLightmapTextureWidth();
	/** @returns -1 if there was an issue getting the lightmap */
	public int getLightmapGLFormat();
	
	// Try and disable vanilla fog. Return true if successful, or false if not able to.
	public boolean tryDisableVanillaFog();
	
}
