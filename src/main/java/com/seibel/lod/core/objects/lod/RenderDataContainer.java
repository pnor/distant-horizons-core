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

package com.seibel.lod.core.objects.lod;

/**
 * A level container is a quad tree level
 */
public interface RenderDataContainer
{
	public void setChildToRendered(int posX, int posZ, boolean newValue);
	public void setRenderedChild(int posX, int posZ, boolean newValue);
	public void setToBeRendered(int posX, int posZ, boolean newValue);
	public void setRendered(int posX, int posZ, boolean newValue);
	
	public boolean isChildToRendered(int posX, int posZ);
	public boolean isChildRendered(int posX, int posZ);
	public boolean isToBeRendered(int posX, int posZ);
	public boolean isRendered(int posX, int posZ);
}
