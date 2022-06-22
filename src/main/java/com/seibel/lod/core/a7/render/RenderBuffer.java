/*
 *    This file is part of the Distant Horizons mod (formerly the LOD Mod),
 *    licensed under the GNU LGPL v3 License.
 *
 *    Copyright (C) 2020-2022  James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
 
package com.seibel.lod.core.a7.render;

import com.seibel.lod.core.render.LodRenderProgram;
import com.seibel.lod.core.util.StatsMap;

public abstract class RenderBuffer implements AutoCloseable
{
	// ======================================================================
	// ====================== Methods for implementations ===================
	// ======================================================================

	// ========== Called by render thread ==========
	/* Called on... well... rendering.
	 * Return false if nothing rendered. (Optional) */
	public abstract boolean render(LodRenderProgram shaderProgram);

	// ========== Called by any thread. (thread safe) ==========
	
	/* Called by anyone. This method is allowed to throw exceptions, but
	 * are never allowed to modify any values. This should behave the same
	 * to other methods as if the method have never been called.
	 * Note: This method is PURELY for debug or stats logging ONLY! */
	public abstract void debugDumpStats(StatsMap statsMap);
	
	// ========= Called only when 1 thread is using it =======
	/* This method is called when object is no longer in use.
	 * Called either after uploadBuffers() returned false (On buffer Upload
	 * thread), or by others when the object is not being used. (not in build,
	 * upload, or render state). */
	public abstract void close();
	
	
}
