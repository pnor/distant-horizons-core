/*
 *    This file is part of the Distant Horizon mod (formerly the LOD Mod),
 *    licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2022 Tom Lee (TomTheFurry)
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

package com.seibel.lod.core.enums.config;

/**
 * AUTO <br>
 * NAME_ONLY <br>
 * NAME_IP <br>
 * NAME_IP_PORT <br>
 * <br>
 * Determines how the multiplayer folders should be named.
 * 
 * @author James Seibel
 * @version 3-7-2022
 */
public enum ServerFolderNameMode
{
	/**
	 * NAME_IP for LAN connections <Br>
	 * NAME_IP_PORT for all others
	 */
	AUTO,
	
	/** Only use the server name */
	NAME_ONLY,
	
	/** 
	 * {SERVER_NAME} IP {IP} <br>
	 * Minecraft Server IP 192.168.1.40 
	 */
	NAME_IP,
	
	/** 
	 * {SERVER_NAME} IP {IP} <br>
	 * Minecraft Server IP 192.168.1.40:25565
	 */
	NAME_IP_PORT,
	
	/** 
	 * {SERVER_NAME} IP {IP} <br>
	 * Minecraft Server IP 192.168.1.40:25565 GameVersion 1.16.5 <Br> <br>
	 * 
	 * Not normally recommended, since the game version can change if the
	 * server installs paper or some other jar. <br>
	 * This is just here to provide backwards compatibility.
	 * 
	 * TODO add this to config desc
	 */
	NAME_IP_PORT_MC_VERSION;
	
}
