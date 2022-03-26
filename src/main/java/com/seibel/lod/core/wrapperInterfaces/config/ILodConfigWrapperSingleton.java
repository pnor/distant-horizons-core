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

package com.seibel.lod.core.wrapperInterfaces.config;

import com.seibel.lod.core.enums.config.*;
import com.seibel.lod.core.enums.rendering.*;
import com.seibel.lod.core.handlers.dependencyInjection.IBindable;
import com.seibel.lod.core.handlers.dependencyInjection.SingletonHandler;
import com.seibel.lod.core.objects.MinDefaultMax;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.wrapperInterfaces.IVersionConstants;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;

/**
 * This holds the config defaults, setters/getters
 * that should be hooked into the host mod loader (Fabric, Forge, etc.), and
 * the options that should be implemented in a configWrapperSingleton.
 * 
 * @author James Seibel
 * @version 3-7-2022
 */
public interface ILodConfigWrapperSingleton extends IBindable
{
	IClient client();
	
	
	interface IClient
	{
		IGraphics graphics();
		IWorldGenerator worldGenerator();
		IMultiplayer multiplayer();
		IAdvanced advanced();


		boolean OPTIONS_BUTTON_DEFAULT = true;
		String OPTIONS_BUTTON_DESC = ""
				+ " Show the lod button in the options screen next to fov";
		boolean getOptionsButton();
		void setOptionsButton(boolean newOptionsButton);
		
		
		//==================//
		// Graphics Configs //
		//==================//
		interface IGraphics
		{
			String DESC = "These settings control how the mod will look in game";
			
			IQuality quality();
			IFogQuality fogQuality();
//			ICloudQuality cloudQuality();
			IAdvancedGraphics advancedGraphics();
			
			
			interface IQuality
			{
				String DESC = "These settings control how detailed the fake chunks will be.";
				
				HorizontalResolution DRAW_RESOLUTION_DEFAULT = HorizontalResolution.BLOCK;
				String DRAW_RESOLUTION_DESC = ""
						+ " What is the maximum detail fake chunks should be drawn at? \n"
						+ " This setting will only affect closer chunks.\n"
						+ " Higher settings will increase memory and GPU usage. \n"
						+ "\n"
						+ " " + HorizontalResolution.CHUNK + ": render 1 LOD for each Chunk. \n"
						+ " " + HorizontalResolution.HALF_CHUNK + ": render 4 LODs for each Chunk. \n"
						+ " " + HorizontalResolution.FOUR_BLOCKS + ": render 16 LODs for each Chunk. \n"
						+ " " + HorizontalResolution.TWO_BLOCKS + ": render 64 LODs for each Chunk. \n"
						+ " " + HorizontalResolution.BLOCK + ": render 256 LODs for each Chunk (width of one block). \n"
						+ "\n"
						+ " Lowest Quality: " + HorizontalResolution.CHUNK
						+ " Highest Quality: " + HorizontalResolution.BLOCK;
				HorizontalResolution getDrawResolution();
				void setDrawResolution(HorizontalResolution newHorizontalResolution);
				
				MinDefaultMax<Integer> LOD_CHUNK_RENDER_DISTANCE_MIN_DEFAULT_MAX = new MinDefaultMax<Integer>(16, 64, 2048);
				String LOD_CHUNK_RENDER_DISTANCE_DESC = ""
						+ " The radius of the mod's render distance. (measured in chunks) \n";
				int getLodChunkRenderDistance();
				void setLodChunkRenderDistance(int newLodChunkRenderDistance);
				
				VerticalQuality VERTICAL_QUALITY_DEFAULT = VerticalQuality.MEDIUM;
				String VERTICAL_QUALITY_DESC = ""
						+ " This indicates how detailed fake chunks will represent \n"
						+ " overhangs, caves, floating islands, ect. \n"
						+ " Higher options will make the world more accurate, but"
						+ " will increase memory and GPU usage. \n"
						+ "\n"
						+ " " + VerticalQuality.LOW + ": uses at max 2 columns per position. \n"
						+ " " + VerticalQuality.MEDIUM + ": uses at max 4 columns per position. \n"
						+ " " + VerticalQuality.HIGH + ": uses at max 8 columns per position. \n"
						+ "\n"
						+ " Lowest Quality: " + VerticalQuality.LOW
						+ " Highest Quality: " + VerticalQuality.HIGH;
				VerticalQuality getVerticalQuality();
				void setVerticalQuality(VerticalQuality newVerticalQuality);
				
				MinDefaultMax<Integer> HORIZONTAL_SCALE_MIN_DEFAULT_MAX = new MinDefaultMax<Integer>(2, 8, 32);
				String HORIZONTAL_SCALE_DESC = ""
						+ " This indicates how quickly fake chunks decrease in quality the further away they are. \n"
						+ " Higher settings will render higher quality fake chunks farther away, \n"
						+ " but will increase memory and GPU usage.";
				int getHorizontalScale();
				void setHorizontalScale(int newHorizontalScale);
				
				HorizontalQuality HORIZONTAL_QUALITY_DEFAULT = HorizontalQuality.MEDIUM;
				String HORIZONTAL_QUALITY_DESC = ""
						+ " This indicates how much farther away each drop in quality is. \n"
						+ "\n"
						+ " " + HorizontalQuality.LOWEST + ": each drop in quality is the same distance away. \n"
						+ " " + HorizontalQuality.LOW + ": each drop in quality is " + HorizontalQuality.LOW.quadraticBase + " times farther away. \n"
						+ " " + HorizontalQuality.MEDIUM + ": each drop in quality is " + HorizontalQuality.MEDIUM.quadraticBase + " times farther away. \n"
						+ " " + HorizontalQuality.HIGH + ": each drop in quality is " + HorizontalQuality.HIGH.quadraticBase + " times farther away. \n"
						+ "\n"
						+ " Lowest Quality: " + HorizontalQuality.LOWEST
						+ " Highest Quality: " + HorizontalQuality.HIGH;
				HorizontalQuality getHorizontalQuality();
				void setHorizontalQuality(HorizontalQuality newHorizontalQuality);
				
				DropoffQuality DROPOFF_QUALITY_DEFAULT = DropoffQuality.AUTO;
				String DROPOFF_QUALITY_DESC = ""
						+ " This determines how lod level drop off will be done. \n"
						+ "\n"
						+ " " + DropoffQuality.SMOOTH_DROPOFF + ": \n"
						+ "     The lod level is calculated for each point, making the drop off a smooth circle. \n"
						+ " " + DropoffQuality.PERFORMANCE_FOCUSED + ": \n"
						+ "     One detail level for an entire region. Minimize CPU usage and \n"
						+ "     improve terrain refresh delay, especially for high Lod render distance. \n"
						+ " " + DropoffQuality.AUTO + ": \n"
						+ "     Use "+ DropoffQuality.SMOOTH_DROPOFF + " for less then 128 Lod render distance, \n"
						+ "     or "+ DropoffQuality.PERFORMANCE_FOCUSED +" otherwise. \n";
				DropoffQuality getDropoffQuality();
				void setDropoffQuality(DropoffQuality newDropoffQuality);
				default DropoffQuality getResolvedDropoffQuality() {
					DropoffQuality dropoffQuality = getDropoffQuality();
					if (dropoffQuality == DropoffQuality.AUTO)
						dropoffQuality = getLodChunkRenderDistance() < 128 ?
								DropoffQuality.SMOOTH_DROPOFF : DropoffQuality.PERFORMANCE_FOCUSED;
					return dropoffQuality;
				}

				MinDefaultMax<Integer> LOD_BIOME_BLENDING_MIN_DEFAULT_MAX = new MinDefaultMax<Integer>(0,1,7);
				String LOD_BIOME_BLENDING_DESC = ""
						+ " This is the same as vanilla Biome Blending settings for Lod area. \n" +
						"     Note that anything other than '0' will greatly effect Lod building time \n" +
						"     and increase triangle count. The cost on chunk generation speed is also \n" +
						"     quite large if set too high.\n" +
						"\n" +
						"     '0' equals to Vanilla Biome Blending of '1x1' or 'OFF', \n" +
						"     '1' equals to Vanilla Biome Blending of '3x3', \n" +
						"     '2' equals to Vanilla Biome Blending of '5x5'... \n";
				int getLodBiomeBlending();
				void setLodBiomeBlending(int newLodBiomeBlending);
			}
			
			interface IFogQuality
			{
				String DESC = "These settings control the fog quality.";	
				
				FogDistance FOG_DISTANCE_DEFAULT = FogDistance.FAR;
				String FOG_DISTANCE_DESC = ""
						+ " At what distance should Fog be drawn on the fake chunks? \n"
						+ "\n"
						+ " This setting shouldn't affect performance.";
				FogDistance getFogDistance();
				void setFogDistance(FogDistance newFogDistance);

				FogDrawMode FOG_DRAW_MODE_DEFAULT = FogDrawMode.FOG_ENABLED;
				String FOG_DRAW_MODE_DESC = ""
						+ " When should fog be drawn? \n"
						+ "\n"
						+ " " + FogDrawMode.USE_OPTIFINE_SETTING + ": Use whatever Fog setting Optifine is using.\n"
						+ " If Optifine isn't installed this defaults to " + FogDrawMode.FOG_ENABLED + ". \n"
						+ " " + FogDrawMode.FOG_ENABLED + ": Never draw fog on the LODs \n"
						+ " " + FogDrawMode.FOG_DISABLED + ": Always draw fast fog on the LODs \n"
						+ "\n"
						+ " Disabling fog will improve GPU performance.";
				FogDrawMode getFogDrawMode();
				void setFogDrawMode(FogDrawMode newFogDrawMode);
				
				FogColorMode FOG_COLOR_MODE_DEFAULT = FogColorMode.USE_WORLD_FOG_COLOR;
				String FOG_COLOR_MODE_DESC = ""
						+ " What color should fog use? \n"
						+ "\n"
						+ " " + FogColorMode.USE_WORLD_FOG_COLOR + ": Use the world's fog color. \n"
						+ " " + FogColorMode.USE_SKY_COLOR + ": Use the sky's color. \n"
						+ "\n"
						+ " This setting doesn't affect performance.";
				FogColorMode getFogColorMode();
				void setFogColorMode(FogColorMode newFogColorMode);
				
				boolean DISABLE_VANILLA_FOG_DEFAULT = true;
				String DISABLE_VANILLA_FOG_DESC = ""
						+ " If true disable Minecraft's fog. \n"
						+ "\n"
						+ " Experimental! Mod support is not guarantee.";
				boolean getDisableVanillaFog();
				void setDisableVanillaFog(boolean newDisableVanillaFog);

				IAdvancedFog advancedFog();

				interface IAdvancedFog {
					String DESC = "Advanced settings for fog rendering. Has no effect if Far Fog is not drawn \n"
							+ "See https://www.desmos.com/calculator/?????? for how setting effect the curve.";

					MinDefaultMax<Double> FOG_RANGE = new MinDefaultMax<>(0.0,1.0, Math.sqrt(2.0));

					MinDefaultMax<Double> FAR_FOG_START_MIN_DEFAULT_MAX = new MinDefaultMax<>(FOG_RANGE.minValue,0.0, FOG_RANGE.maxValue);
					String FAR_FOG_START_DESC = ""
							+ " Where should the far fog start? \n"
							+ "\n"
							+ "   '0.0': Fog start at player's position.\n"
							+ "   '1.0': The fog-start's circle fit just in the lod render distance square.\n"
							+ " '1.414': The lod render distance square fit just in the fog-start's circle.\n";
					double getFarFogStart();
					void setFarFogStart(double newFarFogStart);

					MinDefaultMax<Double> FAR_FOG_END_MIN_DEFAULT_MAX = new MinDefaultMax<>(FOG_RANGE.minValue,1.0, FOG_RANGE.maxValue);
					String FAR_FOG_END_DESC = ""
							+ " Where should the far fog end? \n"
							+ "\n"
							+ "   '0.0': Fog end at player's position.\n"
							+ "   '1.0': The fog-end's circle fit just in the lod render distance square.\n"
							+ " '1.414': The lod render distance square fit just in the fog-end's circle.\n";
					double getFarFogEnd();
					void setFarFogEnd(double newFarFogEnd);

					MinDefaultMax<Double> FAR_FOG_MIN_MIN_DEFAULT_MAX = new MinDefaultMax<>(-5.0,0.0, FOG_RANGE.maxValue);
					String FAR_FOG_MIN_DESC = ""
							+ " What is the minimum fog thickness? \n"
							+ "\n"
							+ "   '0.0': No fog at all.\n"
							+ "   '1.0': Fully fog color.\n";
					double getFarFogMin();
					void setFarFogMin(double newFarFogMin);

					MinDefaultMax<Double> FAR_FOG_MAX_MIN_DEFAULT_MAX = new MinDefaultMax<>(FOG_RANGE.minValue,1.0, 5.0);
					String FAR_FOG_MAX_DESC = ""
							+ " What is the maximum fog thickness? \n"
							+ "\n"
							+ "   '0.0': No fog at all.\n"
							+ "   '1.0': Fully fog color.\n";
					double getFarFogMax();
					void setFarFogMax(double newFarFogMax);

					FogSetting.FogType FAR_FOG_TYPE_DEFAULT = FogSetting.FogType.EXPONENTIAL_SQUARED;
					String FAR_FOG_TYPE_DESC = ""
							+ " How the fog thickness should be calculated from distance? \n"
							+ "\n"
							+ " "+ FogSetting.FogType.LINEAR + ": Linear based on distance (will ignore 'density')\n"
							+ " "+ FogSetting.FogType.EXPONENTIAL + ": 1/(e^(distance*density)) \n"
							+ " "+ FogSetting.FogType.EXPONENTIAL_SQUARED + ": 1/(e^((distance*density)^2) \n";
							//+ " "+ FogSetting.Type.TEXTURE_BASED + ": Use a provided 1D texture mapping (will ignore 'density', 'min', and 'max')\n";
					FogSetting.FogType getFarFogType();
					void setFarFogType(FogSetting.FogType newFarFogType);

					MinDefaultMax<Double> FAR_FOG_DENSITY_MIN_DEFAULT_MAX = new MinDefaultMax<>(0.01,2.5, 50.0);
					String FAR_FOG_DENSITY_DESC = ""
							+ " What is the fog density? \n";
					double getFarFogDensity();
					void setFarFogDensity(double newFarFogDensity);

					IHeightFog heightFog();
					interface IHeightFog {
						String DESC = "Advanced settings for how far fog interacts with height. Has no effect if Far Fog is not drawn \n"
								+ "See https://www.desmos.com/calculator/drzzlfmur9 for how setting effect the curve.";

						HeightFogMixMode HEIGHT_FOG_MIX_MODE_DEFAULT = HeightFogMixMode.BASIC;
						String HEIGHT_FOG_MIX_MODE_DESC = ""
								+ " How the height should effect the fog thickness combined with the normal function? \n"
								+ "\n"
								+ " " + HeightFogMixMode.BASIC + ": No special height fog effect. Fog is calculated based on camera distance \n"
								+ " " + HeightFogMixMode.IGNORE_HEIGHT + ": Ignore height completely. Fog is calculated based on horizontal distance \n"
								+ " " + HeightFogMixMode.ADDITION + ": heightFog + farFog \n"
								+ " " + HeightFogMixMode.MAX + ": max(heightFog, farFog) \n"
								+ " " + HeightFogMixMode.MULTIPLY + ": heightFog * farFog \n"
								+ " " + HeightFogMixMode.INVERSE_MULTIPLY + ": 1 - (1-heightFog) * (1-farFog) \n"
								+ " " + HeightFogMixMode.LIMITED_ADDITION + ": farFog + max(farFog, heightFog) \n"
								+ " " + HeightFogMixMode.MULTIPLY_ADDITION + ": farFog + farFog * heightFog \n"
								+ " " + HeightFogMixMode.INVERSE_MULTIPLY_ADDITION + ": farFog + 1 - (1-heightFog) * (1-farFog) \n"
								+ " " + HeightFogMixMode.AVERAGE + ": farFog*0.5 + heightFog*0.5 \n"
								+ "\n"
								+ " Note that for 'BASIC' mode and 'IGNORE_HEIGHT' mode, fog settings for height fog has no effect.\n";
						HeightFogMixMode getHeightFogMixMode();
						void setHeightFogMixMode(HeightFogMixMode newHeightFogMixMode);

						HeightFogMode HEIGHT_FOG_MODE_DEFAULT = HeightFogMode.ABOVE_AND_BELOW_CAMERA;
						String HEIGHT_FOG_MODE_DESC = ""
								+ " Where should the height fog be located? \n"
								+ "\n"
								+ " " + HeightFogMode.ABOVE_CAMERA + ": Height fog starts from camera to the sky \n"
								+ " " + HeightFogMode.BELOW_CAMERA + ": Height fog starts from camera to the void \n"
								+ " " + HeightFogMode.ABOVE_AND_BELOW_CAMERA + ": Height fog starts from camera to both the sky and the void \n"
								+ " " + HeightFogMode.ABOVE_SET_HEIGHT + ": Height fog starts from a set height to the sky \n"
								+ " " + HeightFogMode.BELOW_SET_HEIGHT + ": Height fog starts from a set height to the void \n"
								+ " " + HeightFogMode.ABOVE_AND_BELOW_SET_HEIGHT + ": Height fog starts from a set height to both the sky and the void \n"
								+ "\n";
						HeightFogMode getHeightFogMode();
						void setHeightFogMode(HeightFogMode newHeightFogMode);

						MinDefaultMax<Double> HEIGHT_FOG_HEIGHT_MIN_DEFAULT_MAX = new MinDefaultMax<>(-4096., 70., 4096.);
						String HEIGHT_FOG_HEIGHT_DESC = ""
								+ " If the height fog is calculated around a set height, what is that height position? \n"
								+ "\n";
						double getHeightFogHeight();
						void setHeightFogHeight(double newHeightFogHeight);

						MinDefaultMax<Double> HEIGHT_FOG_START_MIN_DEFAULT_MAX = new MinDefaultMax<>(FOG_RANGE.minValue, 0.0, FOG_RANGE.maxValue);
						String HEIGHT_FOG_START_DESC = ""
								+ " How far the start of height fog should offset? \n"
								+ "\n"
								+ "   '0.0': Fog start with no offset.\n"
								+ "   '1.0': Fog start with offset of the entire world's height. (Include depth)\n";
						double getHeightFogStart();
						void setHeightFogStart(double newHeightFogStart);

						MinDefaultMax<Double> HEIGHT_FOG_END_MIN_DEFAULT_MAX = new MinDefaultMax<>(FOG_RANGE.minValue, 1.0, FOG_RANGE.maxValue);
						String HEIGHT_FOG_END_DESC = ""
								+ " How far the end of height fog should offset? \n"
								+ "\n"
								+ "   '0.0': Fog end with no offset.\n"
								+ "   '1.0': Fog end with offset of the entire world's height. (Include depth)\n";
						double getHeightFogEnd();
						void setHeightFogEnd(double newHeightFogEnd);

						MinDefaultMax<Double> HEIGHT_FOG_MIN_MIN_DEFAULT_MAX = new MinDefaultMax<>(-5.0, 0.0, FOG_RANGE.maxValue);
						String HEIGHT_FOG_MIN_DESC = ""
								+ " What is the minimum fog thickness? \n"
								+ "\n"
								+ "   '0.0': No fog at all.\n"
								+ "   '1.0': Fully fog color.\n";
						double getHeightFogMin();
						void setHeightFogMin(double newHeightFogMin);

						MinDefaultMax<Double> HEIGHT_FOG_MAX_MIN_DEFAULT_MAX = new MinDefaultMax<>(FOG_RANGE.minValue, 1.0, 5.0);
						String HEIGHT_FOG_MAX_DESC = ""
								+ " What is the maximum fog thickness? \n"
								+ "\n"
								+ "   '0.0': No fog at all.\n"
								+ "   '1.0': Fully fog color.\n";
						double getHeightFogMax();
						void setHeightFogMax(double newHeightFogMax);

						FogSetting.FogType HEIGHT_FOG_TYPE_DEFAULT = FogSetting.FogType.EXPONENTIAL_SQUARED;
						String HEIGHT_FOG_TYPE_DESC = ""
								+ " How the fog thickness should be calculated from height? \n"
								+ "\n"
								+ " " + FogSetting.FogType.LINEAR + ": Linear based on height (will ignore 'density')\n"
								+ " " + FogSetting.FogType.EXPONENTIAL + ": 1/(e^(height*density)) \n"
								+ " " + FogSetting.FogType.EXPONENTIAL_SQUARED + ": 1/(e^((height*density)^2) \n";
						//+ " "+ FogSetting.Type.TEXTURE_BASED + ": Use a provided 1D texture mapping (will ignore 'density', 'min', and 'max')\n";
						FogSetting.FogType getHeightFogType();
						void setHeightFogType(FogSetting.FogType newFarFogType);

						MinDefaultMax<Double> HEIGHT_FOG_DENSITY_MIN_DEFAULT_MAX = new MinDefaultMax<>(0.01, 2.5, 50.0);
						String HEIGHT_FOG_DENSITY_DESC = ""
								+ " What is the fog density? \n";
						double getHeightFogDensity();
						void setHeightFogDensity(double newHeightFogDensity);

						default FogSetting computeHeightFogSetting() {
							return new FogSetting(
									getHeightFogStart(),
									getHeightFogEnd(),
									getHeightFogMin(),
									getHeightFogMax(),
									getHeightFogDensity(),
									getHeightFogType()
							);
						}
					}
					default FogSetting computeFarFogSetting() {
						return new FogSetting(
								getFarFogStart(),
								getFarFogEnd(),
								getFarFogMin(),
								getFarFogMax(),
								getFarFogDensity(),
								getFarFogType()
						);
					}
				}
			}

			/*
			interface ICloudQuality
			{
				String DESC = "These settings control the clouds.";

				boolean CUSTOM_CLOUDS_DEFAULT = false;
				String CUSTOM_CLOUDS_DESC = ""
						+ " Should we use our own method for rendering clouds \n"
						+ "\n"
						+ " If you disable this then the rest of the cloud configs wont work. \n";
				boolean getCustomClouds();
				void setCustomClouds(boolean newCustomClouds);

				boolean COOL_CLOUDS_DEFAULT = false;
				String COOL_CLOUDS_DESC = ""
						+ " THIS IS NOT IMPLEMENTED YET SO DONT USE \n"
						+ " A complete rework on how clouds work \n"
						+ " Rather than getting from a texure and rendering that \n"
						+ " It gets the terrain height and decides how much cloud to put \n"
						+ " This idea came from this 11 year old(as of when this is being written) \n"
						+ "reddit post https://www.reddit.com/r/Minecraft/comments/e7xol/this_is_how_clouds_should_work_gif_simulation/ \n";
				boolean getCoolClouds();
				void setCoolClouds(boolean newFabulousClouds);

				boolean EXTEND_CLOUDS_DEFAULT = true;
				String EXTEND_CLOUDS_DESC = ""
						+ " Extends how far the clouds render \n"
						+ " to the lod render distance \n";
				boolean getExtendClouds();
				void setExtendClouds(boolean newExtendClouds);

				MinDefaultMax<Double> CLOUD_HEIGHT_MIN_DEFAULT_MAX = new MinDefaultMax<Double>(Double.MIN_VALUE, 0., Double.MAX_VALUE); // make it get minecraft cloud height
				String CLOUD_HEIGHT_DESC = ""
						+ " What y level to render the clouds at \n";
				double getCloudHeight();
				void setCloudHeight(double newCloudHeight);
			}
			 */

			interface IAdvancedGraphics
			{
				String DESC = "Graphics options that are a bit more technical.";
				
				boolean DISABLE_DIRECTIONAL_CULLING_DEFAULT = false;
				String DISABLE_DIRECTIONAL_CULLING_DESC = ""
						+ " If false fake chunks behind the player's camera \n"
						+ " aren't drawn, increasing GPU performance. \n"
						+ "\n"
						+ " If true all LODs are drawn, even those behind \n"
						+ " the player's camera, decreasing GPU performance. \n"
						+ "\n"
						+ " Disable this if you see LODs disappearing at the corners of your vision. \n";
				boolean getDisableDirectionalCulling();
				void setDisableDirectionalCulling(boolean newDisableDirectionalCulling);

				VanillaOverdraw VANILLA_OVERDRAW_DEFAULT = VanillaOverdraw.DYNAMIC;
				String VANILLA_OVERDRAW_DESC = ""
						+ " How often should LODs be drawn on top of regular chunks? \n"
						+ " HALF and ALWAYS will prevent holes in the world, \n"
						+ " but may look odd for transparent blocks or in caves. \n"
						+ "\n"
						+ " " + VanillaOverdraw.NEVER + ": \n"
						+ "     LODs won't render on top of vanilla chunks. Use Overdraw offset to change the border offset. \n"
						+ " " + VanillaOverdraw.DYNAMIC + ": \n"
						+ "     LODs will render on top of distant vanilla chunks to hide delayed loading. \n"
						+ "     Will dynamically decide the border offset based on vanilla render distance. \n"
						+ " " + VanillaOverdraw.ALWAYS + ": \n"
						+ "     LODs will render on all vanilla chunks preventing all holes in the world. \n"
						+ "\n"
						+ " This setting shouldn't affect performance. \n";
				VanillaOverdraw getVanillaOverdraw();
				void setVanillaOverdraw(VanillaOverdraw newVanillaOverdraw);


				MinDefaultMax<Integer> OVERDRAW_OFFSET_MIN_DEFAULT_MAX = new MinDefaultMax<Integer>(-16, 0, 16);
				String OVERDRAW_OFFSET_DESC = ""
						+ " If on Vanilla Overdraw mode of NEVER, how much should should the border be offset? \n"
						+ "\n"
						+ "  '1': The start of lods will be shifted inwards by 1 chunk, causing 1 chunk of overdraw. \n"
						+ " '-1': The start fo lods will be shifted outwards by 1 chunk, causing 1 chunk of gap. \n"
						+ "\n"
						+ " This setting can be used to deal with gaps due to our vanilla rendered chunk \n"
						+ "   detection not being perfect. \n";
				int getOverdrawOffset();
				void setOverdrawOffset(int newOverdrawOffset);


				/* Disabled for now due to implementation issues.
				MinDefaultMax<Integer> VANILLA_CULLING_RANGE_MIN_DEFAULT_MAX = new MinDefaultMax<Integer>(0, 32, 512);
				String VANILLA_CULLING_RANGE_DESC = ""
						+ " This indicates the minimum range where back sides of blocks start get get culled. \n"
						+ " Higher settings will make terrain look good when looking backwards \n"
						+ " when changing speeds quickly, but will increase upload times and GPU usage.";
				int getBacksideCullingRange();
				void setBacksideCullingRange(int newBacksideCullingRange);*/
				
				boolean USE_EXTENDED_NEAR_CLIP_PLANE_DEFAULT = true;
				String USE_EXTENDED_NEAR_CLIP_PLANE_DESC = ""
						+ " Will prevent some overdraw issues, but may cause nearby fake chunks to render incorrectly \n"
						+ " especially when in/near an ocean. \n"
						+ "\n"
						+ " This setting shouldn't affect performance. \n";
				boolean getUseExtendedNearClipPlane();
				void setUseExtendedNearClipPlane(boolean newUseExtendedNearClipPlane);
				
				double BRIGHTNESS_MULTIPLIER_DEFAULT = 1.0;
				String BRIGHTNESS_MULTIPLIER_DESC = ""
						+ " How bright fake chunk colors are. \n"
						+ "\n"
						+ " 0 = black \n"
						+ " 1 = normal \n"
						+ " 2 = near white \n";
				double getBrightnessMultiplier();
				void setBrightnessMultiplier(double newBrightnessMultiplier);
				
				double SATURATION_MULTIPLIER_DEFAULT = 1.0;
				String SATURATION_MULTIPLIER_DESC = ""
						+ " How saturated fake chunk colors are. \n"
						+ "\n"
						+ " 0 = black and white \n"
						+ " 1 = normal \n"
						+ " 2 = very saturated \n";
				double getSaturationMultiplier();
				void setSaturationMultiplier(double newSaturationMultiplier);

				boolean ENABLE_CAVE_CULLING_DEFAULT = false;
				String ENABLE_CAVE_CULLING_DESC = ""
						+ " If enabled caves will be culled \n"
						+ "\n"
						+ " NOTE: This feature is under development and \n"
						+ "  it is VERY experimental! Please don't report \n"
						+ " any issues related to this feature. \n"
						+ "\n"
						+ " Additional Info: Currently this cull all faces \n"
						+ "  with skylight value of 0 in dimensions that \n"
						+ "  does not have a ceiling. \n";
				boolean getEnableCaveCulling();
				void setEnableCaveCulling(boolean newEnableCaveCulling);

				MinDefaultMax<Integer> CAVE_CULLING_HEIGHT_MIN_DEFAULT_MAX = new MinDefaultMax<>(-4096,40,4096);
				String CAVE_CULLING_HEIGHT_DESC = ""
						+ " At what Y value should cave culling start? \n";
				int getCaveCullingHeight();
				void setCaveCullingHeight(int newCaveCullingHeight);
			}
		}
		
		
		
		
		//=====================//
		// Multiplayer Configs //
		//=====================//
		interface IMultiplayer
		{
			String DESC = "These settings control how different systems work when connected to a multiplayer world.";
			
			ServerFolderNameMode SERVER_FOLDER_NAME_MODE_DEFAULT = ServerFolderNameMode.AUTO;
			String SERVER_FOLDER_NAME_MODE_DESC = ""
					+ " What multiplayer save folders should be named. \n"
					+ "\n"
					+ " " + ServerFolderNameMode.AUTO.toString() + ": " + ServerFolderNameMode.NAME_IP.toString() + " for LAN connections, " + ServerFolderNameMode.NAME_IP_PORT.toString() + " for all others. \n"
					+ " " + ServerFolderNameMode.NAME_ONLY.toString() + ": Example: \"Minecraft Server\" \n"
					+ " " + ServerFolderNameMode.NAME_IP.toString() + ": Example: \"Minecraft Server IP 192.168.1.40\" \n"
					+ " " + ServerFolderNameMode.NAME_IP_PORT.toString() + ": Example: \"Minecraft Server IP 192.168.1.40:25565\" \n"
					+ "\n";
			ServerFolderNameMode getServerFolderNameMode();
			void setServerFolderNameMode(ServerFolderNameMode newServerFolderNameMode);
			
			double MULTI_DIMENSION_REQUIRED_SIMILARITY_DEFAULT = 0.8;
			String MULTI_DIMENSION_REQUIRED_SIMILARITY_DESC = ""
					+ " When matching worlds of the same dimension type the \n"
					+ " chunks tested must be at least this percent the same \n"
					+ " in order to be considered the same world. \n"
					+ "\n"
					+ " 1 (100%) means the chunks must be identical. \n"
					+ " 0.5 (50%) means the chunks must be half the same. \n"
					+ " 0 (0%) means almost any world will match. \n"
					+ "\n";
			double getMultiDimensionRequiredSimilarity();
			void setMultiDimensionRequiredSimilarity(double newMultiDimensionMinimumSimilarityPercent);
		}
		
		
		
		
		
		//========================//
		// WorldGenerator Configs //
		//========================//
		interface IWorldGenerator
		{
			String DESC = "These settings control how fake chunks outside your normal view range are generated.";

			boolean ENABLE_DISTANT_GENERATION_DEFAULT = true;
			String ENABLE_DISTANT_GENERATION_DESC = ""
					+ " Whether to enable Distant chunks generator? \n"
					+ "\n"
					+ " Turning this on allows Distant Horizons to make lods for chunks \n"
					+ " that are outside of vanilla view distance. \n"
					+ "\n"
					+ " Note that in server, distant generation is always off. \n";
			boolean getEnableDistantGeneration();
			void setEnableDistantGeneration(boolean newEnableDistantGeneration);

			DistanceGenerationMode DISTANCE_GENERATION_MODE_DEFAULT = DistanceGenerationMode.FEATURES;
			static String getDistanceGenerationModeDesc(IVersionConstants versionConstants)
			{		
				return ""
					+ " How detailed should fake chunks be generated outside the vanilla render distance? \n"
					+ "\n"
					+ " The times are the amount of time it took one of the developer's PC to generate \n"
					+ " one chunk in Minecraft 1.16.5 and may be inaccurate for different Minecraft versions. \n"
					+ " They are included to give a rough estimate as to how the different options \n"
					+ " may perform in comparison to each other. \n"
					+ " (Note that all modes will load in already existing chunks) \n"
					+ "\n"
					+ " " + DistanceGenerationMode.NONE + " \n"
					+ " Only run the Generator to load in already existing chunks. \n"
					+ "\n"
					+ " " + DistanceGenerationMode.BIOME_ONLY + " \n"
					+ " Only generate the biomes and use the biome's \n"
					+ " grass color, water color, or snow color. \n"
					+ " Doesn't generate height, everything is shown at sea level. \n"
					+ "  - Fastest (2-5 ms) \n"
					+ "\n"
					+ " " + DistanceGenerationMode.BIOME_ONLY_SIMULATE_HEIGHT + " \n"
					+ " Same as " + DistanceGenerationMode.BIOME_ONLY + ", except instead \n"
					+ " of always using sea level as the LOD height \n"
					+ " different biome types (mountain, ocean, forest, etc.) \n"
					+ " use predetermined heights to simulate having height data. \n"
					+ "  - Fastest (2-5 ms) \n"
					+ "\n"
					+ " " + DistanceGenerationMode.SURFACE + " \n"
					+ " Generate the world surface, \n"
					+ " this does NOT include trees, \n"
					+ " or structures. \n"
					+ "  - Faster (10-20 ms) \n"
					+ "\n"
					+ " " + DistanceGenerationMode.FEATURES + " \n"
					+ " Generate everything except structures. \n"
					+ " WARNING: This may cause world generation bugs or instability! \n"
					+ "  - Fast (15-20 ms) \n"
					+ "\n"
					+ " " + DistanceGenerationMode.FULL + " \n"
					+ " Ask the local server to generate/load each chunk. \n"
					+ " This will show player made structures, which can \n"
					+ " be useful if you are adding the mod to a pre-existing world. \n"
					+ " This is the most compatible, but causes server/simulation lag. \n"
					+ "  - Slow (15-50 ms, with spikes up to 200 ms) \n"
					+ "\n"
					+ " The multithreaded options may increase CPU load significantly (while generating) \n"
					+ " depending on how many world generation threads you have allocated. \n";
			}
			DistanceGenerationMode getDistanceGenerationMode();
			void setDistanceGenerationMode(DistanceGenerationMode newDistanceGenerationMode);
			
			LightGenerationMode LIGHT_GENERATION_MODE_DEFAULT = LightGenerationMode.FANCY;
			String LIGHT_GENERATION_MODE_DESC = ""
					+ " How should block and sky lights be processed for distant generation? \n"
					+ "\n"
					+ " Note that this include already existing chunks since vanilla \n"
					+ " does not store sky light values to save file. \n"
					+ "\n"
					+ " " + LightGenerationMode.FAST + ": Use height map to fake the light values. \n"
					+ " " + LightGenerationMode.FANCY + ": Use actaul light engines to generate proper values. \n"
					+ "\n"
					+ " This will effect generation speed, but not the rendering performance.";
			LightGenerationMode getLightGenerationMode();
			void setLightGenerationMode(LightGenerationMode newLightGenerationMode);
			
			
			GenerationPriority GENERATION_PRIORITY_DEFAULT = GenerationPriority.NEAR_FIRST;
			String GENERATION_PRIORITY_DESC = ""
					+ " In what priority should fake chunks be generated outside the vanilla render distance? \n"
					+ "\n"
					+ " " + GenerationPriority.FAR_FIRST + " \n"
					+ " Fake chunks are generated from lowest to highest detail \n"
					+ " with a priority for far away regions. \n"
					+ " This fills in the world fastest, but you will have large low detail \n"
					+ " blocks for a while while the generation happens. \n"
					+ "\n"
					+ " " + GenerationPriority.NEAR_FIRST + " \n"
					+ " Fake chunks are generated around the player \n"
					+ " in a spiral, similar to vanilla minecraft. \n"
					+ " Best used when on a server since we can't generate \n"
					+ " fake chunks. \n"
					+ "\n"
					+ " " + GenerationPriority.BALANCED + " \n"
					+ " A mix between "+GenerationPriority.NEAR_FIRST+"and"+GenerationPriority.FAR_FIRST+". \n"
					+ " First prioritise completing nearby highest detail chunks, \n"
					+ " then focus on filling in the low detail areas away from the player. \n"
					+ "\n"
					+ " " + GenerationPriority.AUTO + " \n"
					+ " Uses " + GenerationPriority.BALANCED + " when on a single player world \n"
					+ " and " + GenerationPriority.NEAR_FIRST + " when connected to a server. \n"
					+ "\n"
					+ " This shouldn't affect performance.";
			GenerationPriority getGenerationPriority();
			void setGenerationPriority(GenerationPriority newGenerationPriority);
			
			default GenerationPriority getResolvedGenerationPriority() {
				GenerationPriority priority = getGenerationPriority();
				IMinecraftClientWrapper MC = SingletonHandler.get(IMinecraftClientWrapper.class);
				if (priority == GenerationPriority.AUTO)
					priority = MC.hasSinglePlayerServer() ? GenerationPriority.FAR_FIRST : GenerationPriority.BALANCED;
				return priority;
			}
			
			BlocksToAvoid BLOCKS_TO_AVOID_DEFAULT = BlocksToAvoid.BOTH;
			String BLOCKS_TO_AVOID_DESC = ""
					+ " When generating fake chunks, what blocks should be ignored? \n"
					+ " Ignored blocks don't affect the height of the fake chunk, but might affect the color. \n"
					+ " So using " + BlocksToAvoid.BOTH + " will prevent snow covered blocks from appearing one block too tall, \n"
					+ " but will still show the snow's color.\n"
					+ "\n"
					+ " " + BlocksToAvoid.NONE + ": Use all blocks when generating fake chunks \n"
					+ " " + BlocksToAvoid.NON_FULL + ": Only use full blocks when generating fake chunks (ignores slabs, lanterns, torches, tall grass, etc.) \n"
					+ " " + BlocksToAvoid.NO_COLLISION + ": Only use solid blocks when generating fake chunks (ignores tall grass, torches, etc.) \n"
					+ " " + BlocksToAvoid.BOTH + ": Only use full solid blocks when generating fake chunks \n"
					+ "\n"
					+ " This wont't affect performance.";
			BlocksToAvoid getBlocksToAvoid();
			void setBlockToAvoid(BlocksToAvoid newBlockToAvoid);
		}
		
		
		
		
		//============================//
		// AdvancedModOptions Configs //
		//============================//
		interface IAdvanced
		{
			String DESC = "Advanced mod settings";
			
			IThreading threading();
			IDebugging debugging();
			IBuffers buffers();
			
			
			interface IThreading
			{
				String DESC = "These settings control how many CPU threads the mod uses for different tasks.";
				
				MinDefaultMax<Integer> NUMBER_OF_WORLD_GENERATION_THREADS_DEFAULT
					= new MinDefaultMax<Integer>(1, 
							Math.min(Runtime.getRuntime().availableProcessors()/2, 4),
							Runtime.getRuntime().availableProcessors());
				String NUMBER_OF_WORLD_GENERATION_THREADS_DESC = ""
						+ " How many threads should be used when generating fake chunks outside \n"
						+ " the normal render distance? \n"
						+ "\n"
						+ " If you experience stuttering when generating distant LODs, decrease \n"
						+ " this number. If you want to increase LOD generation speed, \n"
						+ " increase this number. \n"
						+ "\n"
						+ " This and the number of buffer builder threads are independent, \n"
						+ " so if they add up to more threads than your CPU has cores, \n"
						+ " that shouldn't cause an issue. \n"
						+ "\n"
						+ " The maximum value is the number of logical processors on your CPU. \n"
						+ " Requires a restart to take effect. \n";
				int getNumberOfWorldGenerationThreads();
				void setNumberOfWorldGenerationThreads(int newNumberOfWorldGenerationThreads);
				
				MinDefaultMax<Integer> NUMBER_OF_BUFFER_BUILDER_THREADS_MIN_DEFAULT_MAX
					= new MinDefaultMax<Integer>(1, 
							Math.min(Runtime.getRuntime().availableProcessors()/2, 2), 
							Runtime.getRuntime().availableProcessors());
				String NUMBER_OF_BUFFER_BUILDER_THREADS_DESC = ""
						+ " How many threads are used when building vertex buffers? \n"
						+ " (The things sent to your GPU to draw the fake chunks). \n"
						+ "\n"
						+ " If you experience high CPU usage when NOT generating distant \n"
						+ " fake chunks, lower this number. A higher number will make fake\n"
						+ " fake chunks' transition faster when moving around the world. \n"
						+ "\n"
						+ " This and the number of world generator threads are independent, \n"
						+ " so if they add up to more threads than your CPU has cores, \n"
						+ " that shouldn't cause an issue. \n"
						+ "\n"
						+ " The maximum value is the number of logical processors on your CPU. \n"
						+ " Requires a restart to take effect. \n";
				int getNumberOfBufferBuilderThreads();
				void setNumberOfBufferBuilderThreads(int newNumberOfWorldBuilderThreads);
			}
			
			interface IDebugging
			{
				String DESC = "These settings can be used to look for bugs, or see how certain aspects of the mod work.";
				
				boolean DRAW_LODS_DEFAULT = true;
				String DRAW_LODS_DESC = ""
						+ " If true, the mod is enabled and fake chunks will be drawn. \n"
						+ " If false, the mod will still generate fake chunks, \n"
						+ " but they won't be rendered. \n"
						+ "\n"
						+ " Disabling rendering will reduce GPU usage \n";
				boolean getDrawLods();
				void setDrawLods(boolean newDrawLods);
				
				DebugMode DEBUG_MODE_DEFAULT = DebugMode.OFF;
				String DEBUG_MODE_DESC = ""
						+ " Should specialized colors/rendering modes be used? \n"
						+ "\n"
						+ " " + DebugMode.OFF + ": Fake chunks will be drawn with their normal colors. \n"
						+ " " + DebugMode.SHOW_WIREFRAME + ": Fake chunks will be drawn as wireframes. \n"
						+ " " + DebugMode.SHOW_DETAIL + ": Fake chunks color will be based on their detail level. \n"
						+ " " + DebugMode.SHOW_DETAIL_WIREFRAME + ": Fake chunks color will be based on their detail level, drawn as a wireframe. \n"
						+ " " + DebugMode.SHOW_GENMODE + ": Fake chunks color will be based on their distant generation mode. \n"
						+ " " + DebugMode.SHOW_GENMODE_WIREFRAME + ": Fake chunks color will be based on their distant generation mode, drawn as a wireframe. \n";
				DebugMode getDebugMode();
				void setDebugMode(DebugMode newDebugMode);
				
				boolean DEBUG_KEYBINDINGS_ENABLED_DEFAULT = false;
				String DEBUG_KEYBINDINGS_ENABLED_DESC = ""
						+ " If true the F8 key can be used to cycle through the different debug modes. \n"
						+ " and the F6 key can be used to enable and disable LOD rendering.";
				boolean getDebugKeybindingsEnabled();
				void setDebugKeybindingsEnabled(boolean newEnableDebugKeybindings);

				IDebugSwitch debugSwitch();

				interface IDebugSwitch
				{
					String DESC = "These settings can be used to look for bugs by enabling and disabling logging.";

					/* The logging switches available:
					 * WorldGenEvent
					 * WorldGenPerformance
					 * WorldGenLoadEvent
					 * LodBuilderEvent
					 * RendererBufferEvent
					 * RendererGLEvent
					 * FileReadWriteEvent
					 * FileSubDimEvent
					 * NetworkEvent //NOT IMPL YET
					*/

					LoggerMode LOG_WORLDGEN_EVENT_DEFAULT = LoggerMode.LOG_WARNING_TO_CHAT_AND_INFO_TO_FILE;
					String LOG_WORLDGEN_EVENT_DESC = ""
							+ " If enabled, the mod will log information about the world generation process. \n"
							+ " This can be useful for debugging. \n";
					LoggerMode getLogWorldGenEvent();
					void setLogWorldGenEvent(LoggerMode newLogWorldGenEvent);

					LoggerMode LOG_WORLDGEN_PERFORMANCE_DEFAULT = LoggerMode.LOG_WARNING_TO_CHAT_AND_INFO_TO_FILE;
					String LOG_WORLDGEN_PERFORMANCE_DESC = ""
							+ " If enabled, the mod will log performance about the world generation process. \n"
							+ " This can be useful for debugging. \n";
					LoggerMode getLogWorldGenPerformance();
					void setLogWorldGenPerformance(LoggerMode newLogWorldGenPerformance);

					LoggerMode LOG_WORLDGEN_LOAD_EVENT_DEFAULT = LoggerMode.LOG_WARNING_TO_CHAT_AND_FILE;
					String LOG_WORLDGEN_LOAD_EVENT_DESC = ""
							+ " If enabled, the mod will log information about the world generation process. \n"
							+ " This can be useful for debugging. \n";
					LoggerMode getLogWorldGenLoadEvent();
					void setLogWorldGenLoadEvent(LoggerMode newLogWorldGenLoadEvent);

					LoggerMode LOG_LODBUILDER_EVENT_DEFAULT = LoggerMode.LOG_WARNING_TO_CHAT_AND_INFO_TO_FILE;
					String LOG_LODBUILDER_EVENT_DESC = ""
							+ " If enabled, the mod will log information about the LOD generation process. \n"
							+ " This can be useful for debugging. \n";
					LoggerMode getLogLodBuilderEvent();
					void setLogLodBuilderEvent(LoggerMode newLogLodBuilderEvent);

					LoggerMode LOG_RENDERER_BUFFER_EVENT_DEFAULT = LoggerMode.LOG_WARNING_TO_CHAT_AND_INFO_TO_FILE;
					String LOG_RENDERER_BUFFER_EVENT_DESC = ""
							+ " If enabled, the mod will log information about the renderer buffer process. \n"
							+ " This can be useful for debugging. \n";
					LoggerMode getLogRendererBufferEvent();
					void setLogRendererBufferEvent(LoggerMode newLogRendererBufferEvent);

					LoggerMode LOG_RENDERER_GL_EVENT_DEFAULT = LoggerMode.LOG_WARNING_TO_CHAT_AND_INFO_TO_FILE;
					String LOG_RENDERER_GL_EVENT_DESC = ""
							+ " If enabled, the mod will log information about the renderer OpenGL process. \n"
							+ " This can be useful for debugging. \n";
					LoggerMode getLogRendererGLEvent();
					void setLogRendererGLEvent(LoggerMode newLogRendererGLEvent);

					LoggerMode LOG_FILE_READWRITE_EVENT_DEFAULT = LoggerMode.LOG_WARNING_TO_CHAT_AND_INFO_TO_FILE;
					String LOG_FILE_READWRITE_EVENT_DESC = ""
							+ " If enabled, the mod will log information about file read/write operations. \n"
							+ " This can be useful for debugging. \n";
					LoggerMode getLogFileReadWriteEvent();
					void setLogFileReadWriteEvent(LoggerMode newLogFileReadWriteEvent);

					LoggerMode LOG_FILE_SUB_DIM_EVENT_DEFAULT = LoggerMode.LOG_WARNING_TO_CHAT_AND_INFO_TO_FILE;
					String LOG_FILE_SUB_DIM_EVENT_DESC = ""
							+ " If enabled, the mod will log information about file sub-dimension operations. \n"
							+ " This can be useful for debugging. \n";
					LoggerMode getLogFileSubDimEvent();
					void setLogFileSubDimEvent(LoggerMode newLogFileSubDimEvent);

					LoggerMode LOG_NETWORK_EVENT_DEFAULT = LoggerMode.LOG_WARNING_TO_CHAT_AND_INFO_TO_FILE;
					String LOG_NETWORK_EVENT_DESC = ""
							+ " If enabled, the mod will log information about network operations. \n"
							+ " This can be useful for debugging. \n";
					LoggerMode getLogNetworkEvent();
					void setLogNetworkEvent(LoggerMode newLogNetworkEvent);
				}
			}
			
			interface IBuffers
			{
				String DESC = "These settings affect how often geometry is rebuilt.";
				
				GpuUploadMethod GPU_UPLOAD_METHOD_DEFAULT = GpuUploadMethod.AUTO;
				String GPU_UPLOAD_METHOD_DESC = ""
						+ " What method should be used to upload geometry to the GPU? \n"
						+ "\n"
						+ " " + GpuUploadMethod.AUTO + ": Picks the best option based on the GPU you have. \n"
						+ " " + GpuUploadMethod.BUFFER_STORAGE + ": Default for NVIDIA if OpenGL 4.5 is supported. \n"
						+ "                 Fast rendering, no stuttering. \n"
						+ " " + GpuUploadMethod.SUB_DATA + ": Backup option for NVIDIA. \n"
						+ "           Fast rendering but may stutter when uploading. \n"
						+ " " + GpuUploadMethod.BUFFER_MAPPING + ": Slow rendering but won't stutter when uploading. Possibly the best option for integrated GPUs. \n"
						+ "                Default option for AMD/Intel. \n"
						+ "                May end up storing buffers in System memory. \n"
						+ "                Fast rendering if in GPU memory, slow if in system memory, \n"
						+ "                but won't stutter when uploading.  \n"
						+ " " + GpuUploadMethod.DATA + ": Fast rendering but will stutter when uploading. \n"
						+ "       Backup option for AMD/Intel. \n"
						+ "       Fast rendering but may stutter when uploading. \n"  
						+ "\n"
						+ " If you don't see any difference when changing these settings, or the world looks corrupted: \n"
						+ " Restart the game to clear the old buffers. \n";
				GpuUploadMethod getGpuUploadMethod();
				void setGpuUploadMethod(GpuUploadMethod newGpuUploadMethod);
				
				MinDefaultMax<Integer> GPU_UPLOAD_PER_MEGABYTE_IN_MILLISECONDS_DEFAULT = new MinDefaultMax<Integer>(0, 0, 50);
				String GPU_UPLOAD_PER_MEGABYTE_IN_MILLISECONDS_DESC = ""
						+ " How long should a buffer wait per Megabyte of data uploaded?\n"
						+ " Helpful resource for frame times: https://fpstoms.com \n"
						+ "\n"
						+ " Longer times may reduce stuttering but will make fake chunks \n"
						+ " transition and load slower. Change this to [0] for no timeout.\n"
						+ "\n"
						+ " NOTE:\n"
						+ " Before changing this config, try changing \"GPU Upload methods\"\n"
						+ "  and determined the best method for your hardware first. \n";
				int getGpuUploadPerMegabyteInMilliseconds();
				void setGpuUploadPerMegabyteInMilliseconds(int newMilliseconds);
				
				String REBUILD_TIMES_DESC = ""
						+ " How frequently should vertex buffers (geometry) be rebuilt and sent to the GPU? \n"
						+ " Higher settings may cause stuttering, but will prevent holes in the world \n";
				BufferRebuildTimes REBUILD_TIMES_DEFAULT = BufferRebuildTimes.NORMAL;
				BufferRebuildTimes getRebuildTimes();
				void setRebuildTimes(BufferRebuildTimes newBufferRebuildTimes);
			}

			boolean LOD_ONLY_MODE_DEFAULT = false;
			String LOD_ONLY_MODE_DESC = ""
					+ " Due to some demand for playing without vanilla terrains, \n"
					+ " we decided to add this mode for fun. \n"
					+ "\n"
					+ " NOTE: Do not report any issues when this mode is on! \n"
					+ "   Again, this setting is only for fun, and mod \n"
					+ "   compatibility is not guaranteed. \n"
					+ "\n";

			boolean getLodOnlyMode();
			void setLodOnlyMode(boolean newLodOnlyMode);
		}
	}
	
}
