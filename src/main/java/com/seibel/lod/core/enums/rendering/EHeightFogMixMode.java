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
 * BASIC                        <br>
 * IGNORE_HEIGHT                <br>
 * ADDITION                     <br>
 * MAX                          <br>
 * MULTIPLY                     <br>
 * INVERSE_MULTIPLY             <br>
 * LIMITED_ADDITION             <br>
 * MULTIPLY_ADDITION            <br>
 * INVERSE_MULTIPLY_ADDITION    <br>
 * AVERAGE                      <br>
 *
 * @author Leetom
 * @version 2022-4-14
 */
public enum EHeightFogMixMode
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
