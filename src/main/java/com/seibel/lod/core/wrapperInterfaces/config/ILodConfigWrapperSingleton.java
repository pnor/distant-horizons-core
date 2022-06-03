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

package com.seibel.lod.core.wrapperInterfaces.config;

import com.seibel.lod.core.api.external.apiObjects.enums.DhApiFogDrawMode;
import com.seibel.lod.core.api.external.apiObjects.enums.DhApiFogColorMode;
import com.seibel.lod.core.api.external.apiObjects.enums.DhApiFogDistance;
import com.seibel.lod.core.api.external.apiObjects.enums.DhApiRendererType;
import com.seibel.lod.core.enums.config.*;
import com.seibel.lod.core.enums.rendering.*;
import com.seibel.lod.core.handlers.dependencyInjection.IBindable;
import com.seibel.lod.core.handlers.dependencyInjection.SingletonHandler;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;

/**
 * Use the real config rather than these getters
 */
@Deprecated
public interface ILodConfigWrapperSingleton extends IBindable
{
	IClient client();

	@Deprecated
	interface IClient
	{
		IGraphics graphics();
		IWorldGenerator worldGenerator();
		IMultiplayer multiplayer();
		IAdvanced advanced();

		boolean getOptionsButton();
		void setOptionsButton(boolean newOptionsButton);
		
		
		//==================//
		// Graphics Configs //
		//==================//
		@Deprecated
		interface IGraphics
		{
			IQuality quality();
			IFogQuality fogQuality();
			IAdvancedGraphics advancedGraphics();

			@Deprecated
			interface IQuality
			{
				HorizontalResolution getDrawResolution();
				void setDrawResolution(HorizontalResolution newHorizontalResolution);

				int getLodChunkRenderDistance();
				void setLodChunkRenderDistance(int newLodChunkRenderDistance);

				VerticalQuality getVerticalQuality();
				void setVerticalQuality(VerticalQuality newVerticalQuality);

				int getHorizontalScale();
				void setHorizontalScale(int newHorizontalScale);

				HorizontalQuality getHorizontalQuality();
				void setHorizontalQuality(HorizontalQuality newHorizontalQuality);

				DropoffQuality getDropoffQuality();
				void setDropoffQuality(DropoffQuality newDropoffQuality);
				default DropoffQuality getResolvedDropoffQuality() {
					DropoffQuality dropoffQuality = getDropoffQuality();
					if (dropoffQuality == DropoffQuality.AUTO)
						dropoffQuality = getLodChunkRenderDistance() < 128 ?
								DropoffQuality.SMOOTH_DROPOFF : DropoffQuality.PERFORMANCE_FOCUSED;
					return dropoffQuality;
				}

				int getLodBiomeBlending();
				void setLodBiomeBlending(int newLodBiomeBlending);
			}
			@Deprecated
			interface IFogQuality
			{
				DhApiFogDistance getFogDistance();
				void setFogDistance(DhApiFogDistance newFogDistance);

				DhApiFogDrawMode getFogDrawMode();
				void setFogDrawMode(DhApiFogDrawMode newFogDrawMode);

				DhApiFogColorMode getFogColorMode();
				void setFogColorMode(DhApiFogColorMode newFogColorMode);

				boolean getDisableVanillaFog();
				void setDisableVanillaFog(boolean newDisableVanillaFog);

				IAdvancedFog advancedFog();
				@Deprecated
				interface IAdvancedFog {
					double getFarFogStart();
					void setFarFogStart(double newFarFogStart);

					double getFarFogEnd();
					void setFarFogEnd(double newFarFogEnd);

					double getFarFogMin();
					void setFarFogMin(double newFarFogMin);

					double getFarFogMax();
					void setFarFogMax(double newFarFogMax);

					FogSetting.FogType getFarFogType();
					void setFarFogType(FogSetting.FogType newFarFogType);

					double getFarFogDensity();
					void setFarFogDensity(double newFarFogDensity);

					IHeightFog heightFog();
					@Deprecated
					interface IHeightFog {
						HeightFogMixMode getHeightFogMixMode();
						void setHeightFogMixMode(HeightFogMixMode newHeightFogMixMode);

						HeightFogMode getHeightFogMode();
						void setHeightFogMode(HeightFogMode newHeightFogMode);

						double getHeightFogHeight();
						void setHeightFogHeight(double newHeightFogHeight);

						double getHeightFogStart();
						void setHeightFogStart(double newHeightFogStart);

						double getHeightFogEnd();
						void setHeightFogEnd(double newHeightFogEnd);

						double getHeightFogMin();
						void setHeightFogMin(double newHeightFogMin);

						double getHeightFogMax();
						void setHeightFogMax(double newHeightFogMax);

						FogSetting.FogType getHeightFogType();
						void setHeightFogType(FogSetting.FogType newFarFogType);

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
			@Deprecated
			interface IAdvancedGraphics
			{
				boolean getDisableDirectionalCulling();
				void setDisableDirectionalCulling(boolean newDisableDirectionalCulling);

				VanillaOverdraw getVanillaOverdraw();
				void setVanillaOverdraw(VanillaOverdraw newVanillaOverdraw);

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

				boolean getUseExtendedNearClipPlane();
				void setUseExtendedNearClipPlane(boolean newUseExtendedNearClipPlane);

				double getBrightnessMultiplier();
				void setBrightnessMultiplier(double newBrightnessMultiplier);

				double getSaturationMultiplier();
				void setSaturationMultiplier(double newSaturationMultiplier);

				boolean getEnableCaveCulling();
				void setEnableCaveCulling(boolean newEnableCaveCulling);

				int getCaveCullingHeight();
				void setCaveCullingHeight(int newCaveCullingHeight);

				int getEarthCurveRatio();
				void setEarthCurveRatio(int newEarthCurveRatio);

			}
		}




		//=====================//
		// Multiplayer Configs //
		//=====================//
		@Deprecated
		interface IMultiplayer
		{
			ServerFolderNameMode getServerFolderNameMode();
			void setServerFolderNameMode(ServerFolderNameMode newServerFolderNameMode);

			double getMultiDimensionRequiredSimilarity();
			void setMultiDimensionRequiredSimilarity(double newMultiDimensionMinimumSimilarityPercent);
		}


		
		
		
		//========================//
		// WorldGenerator Configs //
		//========================//
		@Deprecated
		interface IWorldGenerator
		{
			boolean getEnableDistantGeneration();
			void setEnableDistantGeneration(boolean newEnableDistantGeneration);

			DistanceGenerationMode getDistanceGenerationMode();
			void setDistanceGenerationMode(DistanceGenerationMode newDistanceGenerationMode);

			LightGenerationMode getLightGenerationMode();
			void setLightGenerationMode(LightGenerationMode newLightGenerationMode);

			GenerationPriority getGenerationPriority();
			void setGenerationPriority(GenerationPriority newGenerationPriority);
			
			default GenerationPriority getResolvedGenerationPriority() {
				GenerationPriority priority = getGenerationPriority();
				IMinecraftClientWrapper MC = SingletonHandler.get(IMinecraftClientWrapper.class);
				if (priority == GenerationPriority.AUTO)
					priority = MC.hasSinglePlayerServer() ? GenerationPriority.FAR_FIRST : GenerationPriority.BALANCED;
				return priority;
			}

			BlocksToAvoid getBlocksToAvoid();
			void setBlockToAvoid(BlocksToAvoid newBlockToAvoid);

			Boolean getTintWithAvoidedBlocks();
			void setTintWithAvoidedBlocks(Boolean shouldTint);
		}
		
		
		
		
		//============================//
		// AdvancedModOptions Configs //
		//============================//
		@Deprecated
		interface IAdvanced
		{
			IThreading threading();
			IDebugging debugging();
			IBuffers buffers();

			@Deprecated
			interface IThreading
			{
				double getNumberOfWorldGenerationThreads();
				void setNumberOfWorldGenerationThreads(double newNumberOfWorldGenerationThreads);
				default int _getWorldGenerationThreadPoolSize()
				{
					return getNumberOfWorldGenerationThreads()<1 ? 1 :
							(int) Math.ceil(getNumberOfWorldGenerationThreads());
				}
				default double _getWorldGenerationPartialRunTime()
				{
					return getNumberOfWorldGenerationThreads()>1 ? 1.0 : getNumberOfWorldGenerationThreads();
				}

				int getNumberOfBufferBuilderThreads();
				void setNumberOfBufferBuilderThreads(int newNumberOfWorldBuilderThreads);
			}
			@Deprecated
			interface IDebugging
			{
				DhApiRendererType getRendererType();
				void setRendererType(DhApiRendererType newRendererType);

				DebugMode getDebugMode();
				void setDebugMode(DebugMode newDebugMode);

				boolean getDebugKeybindingsEnabled();
				void setDebugKeybindingsEnabled(boolean newEnableDebugKeybindings);

				IDebugSwitch debugSwitch();
				@Deprecated
				interface IDebugSwitch
				{
					/*
					 * The logging switches available:
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

					LoggerMode getLogWorldGenEvent();
					void setLogWorldGenEvent(LoggerMode newLogWorldGenEvent);

					LoggerMode getLogWorldGenPerformance();
					void setLogWorldGenPerformance(LoggerMode newLogWorldGenPerformance);

					LoggerMode getLogWorldGenLoadEvent();
					void setLogWorldGenLoadEvent(LoggerMode newLogWorldGenLoadEvent);

					LoggerMode getLogLodBuilderEvent();
					void setLogLodBuilderEvent(LoggerMode newLogLodBuilderEvent);

					LoggerMode getLogRendererBufferEvent();
					void setLogRendererBufferEvent(LoggerMode newLogRendererBufferEvent);

					LoggerMode getLogRendererGLEvent();
					void setLogRendererGLEvent(LoggerMode newLogRendererGLEvent);

					LoggerMode getLogFileReadWriteEvent();
					void setLogFileReadWriteEvent(LoggerMode newLogFileReadWriteEvent);

					LoggerMode getLogFileSubDimEvent();
					void setLogFileSubDimEvent(LoggerMode newLogFileSubDimEvent);

					LoggerMode getLogNetworkEvent();
					void setLogNetworkEvent(LoggerMode newLogNetworkEvent);
				}
			}
			@Deprecated
			interface IBuffers
			{
				GpuUploadMethod getGpuUploadMethod();
				void setGpuUploadMethod(GpuUploadMethod newGpuUploadMethod);

				int getGpuUploadPerMegabyteInMilliseconds();
				void setGpuUploadPerMegabyteInMilliseconds(int newMilliseconds);

				BufferRebuildTimes getRebuildTimes();
				void setRebuildTimes(BufferRebuildTimes newBufferRebuildTimes);
			}

			boolean getLodOnlyMode();
			void setLodOnlyMode(boolean newLodOnlyMode);
		}
	}
	
}
