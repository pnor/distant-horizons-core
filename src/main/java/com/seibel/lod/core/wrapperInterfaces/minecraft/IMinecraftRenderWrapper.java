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

package com.seibel.lod.core.wrapperInterfaces.minecraft;

import java.awt.Color;
import java.util.HashSet;

import com.seibel.lod.core.handlers.dependencyInjection.IBindable;
import com.seibel.lod.core.handlers.dependencyInjection.ModAccessorHandler;
import com.seibel.lod.core.handlers.dependencyInjection.SingletonHandler;
import com.seibel.lod.core.objects.math.Mat4f;
import com.seibel.lod.core.objects.math.Vec3d;
import com.seibel.lod.core.objects.math.Vec3f;
import com.seibel.lod.core.wrapperInterfaces.IVersionConstants;
import com.seibel.lod.core.wrapperInterfaces.IWrapperFactory;
import com.seibel.lod.core.wrapperInterfaces.block.AbstractBlockPosWrapper;
import com.seibel.lod.core.wrapperInterfaces.chunk.AbstractChunkPosWrapper;
import com.seibel.lod.core.wrapperInterfaces.misc.ILightMapWrapper;
import com.seibel.lod.core.wrapperInterfaces.modAccessor.ISodiumAccessor;
import com.seibel.lod.core.wrapperInterfaces.world.IWorldWrapper;

/**
 * Contains everything related to
 * rendering in Minecraft.
 * 
 * @author James Seibel
 * @version 3-5-2022
 */
public interface IMinecraftRenderWrapper extends IBindable
{
	Vec3f getLookAtVector();
	
	AbstractBlockPosWrapper getCameraBlockPosition();
	
	boolean playerHasBlindnessEffect();
	
	Vec3d getCameraExactPosition();
	
	Mat4f getDefaultProjectionMatrix(float partialTicks);
	
	double getGamma();
	
	Color getFogColor(float partialTicks);
	
	default Color getSpecialFogColor(float partialTicks) {return getFogColor(partialTicks);}

	boolean isFogStateSpecial();
	
	Color getSkyColor();
	
	double getFov(float partialTicks);
	
	/** Measured in chunks */
	int getRenderDistance();
	
	int getScreenWidth();
	int getScreenHeight();

	int getTargetFrameBuffer();
	int getTargetFrameBufferViewportWidth();
	int getTargetFrameBufferViewportHeight();
	
	/**
	 * This method returns the ChunkPos of all chunks that Minecraft
	 * is going to render this frame.
	 * <br>
	 * If not implemented this calls {@link #getMaximumRenderedChunks()}.
	 */
	default HashSet<AbstractChunkPosWrapper> getVanillaRenderedChunks()
	{
		ISodiumAccessor sodium = ModAccessorHandler.get(ISodiumAccessor.class);
		return sodium==null ? getMaximumRenderedChunks() : sodium.getNormalRenderedChunks();
	}

	static boolean correctedCheckRadius(int dx, int dz, int radius2Mul4) {
		dx = dx*2;// + (dx < 0 ? -1 : 1);
		dz = dz*2;// + (dz < 0 ? -1 : 1);
		return (dx*dx + dz*dz <= radius2Mul4);
	}

	/**
	 * <strong>Doesn't need to be implemented.</strong> <br>
	 * Returns every chunk position within the vanilla render distance.
	 */
	default HashSet<AbstractChunkPosWrapper> getMaximumRenderedChunks()
	{
		IMinecraftClientWrapper mcWrapper = SingletonHandler.get(IMinecraftClientWrapper.class);
		IWrapperFactory factory = SingletonHandler.get(IWrapperFactory.class);
		IVersionConstants versionConstants = SingletonHandler.get(IVersionConstants.class);
		IMinecraftClientWrapper minecraft = SingletonHandler.get(IMinecraftClientWrapper.class);
		IWorldWrapper clientWorld = minecraft.getWrappedClientWorld();

		int chunkDist = this.getRenderDistance() + 1; // For some reason having '+1' is actually closer to real value
		
		AbstractChunkPosWrapper centerChunkPos = mcWrapper.getPlayerChunkPos();
		int centerChunkX = centerChunkPos.getX();
		int centerChunkZ = centerChunkPos.getZ();
		int chunkDist2Mul4 = chunkDist*chunkDist*4;
		
		// add every position within render distance
		HashSet<AbstractChunkPosWrapper> renderedPos = new HashSet<AbstractChunkPosWrapper>();
		for (int deltaChunkX = -chunkDist; deltaChunkX <= chunkDist; deltaChunkX++)
		{
			for(int deltaChunkZ = -chunkDist; deltaChunkZ <= chunkDist; deltaChunkZ++)
			{
				if (!versionConstants.isVanillaRenderedChunkSquare() &&
						!correctedCheckRadius(deltaChunkX,deltaChunkZ,chunkDist2Mul4)) {
					continue;
				}
				if (!clientWorld.hasChunkLoaded(centerChunkX + deltaChunkX, centerChunkZ + deltaChunkZ)) continue;
				renderedPos.add(factory.createChunkPos(centerChunkX + deltaChunkX, centerChunkZ + deltaChunkZ));
			}
		}
		return renderedPos;	
	}
	
	/** @returns null if there was a issue getting the lightmap */
	@Deprecated
	int[] getLightmapPixels();

	ILightMapWrapper getLightmapWrapper();

	/** @returns -1 if there was an issue getting the lightmap */
	@Deprecated
	int getLightmapTextureHeight();
	/** @returns -1 if there was an issue getting the lightmap */
	@Deprecated
	int getLightmapTextureWidth();
	/** @returns -1 if there was an issue getting the lightmap */
	@Deprecated
	int getLightmapGLFormat();
	
	// Try and disable vanilla fog. Return true if successful, or false if not able to.
	boolean tryDisableVanillaFog();

	
}
