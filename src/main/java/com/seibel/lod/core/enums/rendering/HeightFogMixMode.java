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
 
package com.seibel.lod.core.enums.rendering;

/**
 * basic                        <br>
 * Ignore_Height                <br>
 * Addition                     <br>
 * Max                          <br>
 * Multiply                     <br>
 * Inverse_Multiply             <br>
 * Limited_Addition             <br>
 * Multiply_Addition            <br>
 * Inverse_Multiply_Addition    <br>
 * Average                      <br>
 *
 * @author Leetom
 * @version 2022-4-14
 */
public enum HeightFogMixMode
{
    BASIC,
    IGNORE_HEIGHT,
    ADDITION,
    MAX,
    MULTIPLY,
    INVERSE_MULTIPLY,
    LIMITED_ADDITION,
    MULTIPLY_ADDITION,
    INVERSE_MULTIPLY_ADDITION,
    AVERAGE,
}
