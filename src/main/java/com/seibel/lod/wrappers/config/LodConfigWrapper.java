package com.seibel.lod.wrappers.config;

import com.seibel.lod.api.forge.ForgeConfig;
import com.seibel.lod.core.enums.config.BlockToAvoid;
import com.seibel.lod.core.enums.config.BufferRebuildTimes;
import com.seibel.lod.core.enums.config.DistanceGenerationMode;
import com.seibel.lod.core.enums.config.GenerationPriority;
import com.seibel.lod.core.enums.config.GpuUploadMethod;
import com.seibel.lod.core.enums.config.HorizontalQuality;
import com.seibel.lod.core.enums.config.HorizontalResolution;
import com.seibel.lod.core.enums.config.HorizontalScale;
import com.seibel.lod.core.enums.config.LodTemplate;
import com.seibel.lod.core.enums.config.VanillaOverdraw;
import com.seibel.lod.core.enums.config.VerticalQuality;
import com.seibel.lod.core.enums.rendering.DebugMode;
import com.seibel.lod.core.enums.rendering.FogDistance;
import com.seibel.lod.core.enums.rendering.FogDrawOverride;

/**
 * This holds the config defaults and setters/getters
 * that should be hooked into the host mod loader (Fabric, Forge, etc.).
 * 
 * @author James Seibel
 * @version 11-14-2021
 */
public class LodConfigWrapper
{
	public static final Client CLIENT = new Client();
	
	
	public static class Client
	{
		public final Graphics graphics;
		public final WorldGenerator worldGenerator;
		public final AdvancedModOptions advancedModOptions;
		
		
		//================//
		// Client Configs //
		//================//
		public Client()
		{
			graphics = new Graphics();
			worldGenerator = new WorldGenerator();
			advancedModOptions = new AdvancedModOptions();
		}
		
		
		//==================//
		// Graphics Configs //
		//==================//
		public static class Graphics
		{
			
			public final QualityOption qualityOption;
			public final FogQualityOption fogQualityOption;
			public final AdvancedGraphicsOption advancedGraphicsOption;
			
			Graphics()
			{
				qualityOption = new QualityOption();
				advancedGraphicsOption = new AdvancedGraphicsOption();
				fogQualityOption = new FogQualityOption();
			}
			
			
			public static class QualityOption
			{
				
				public HorizontalResolution getDrawResolution()
				{
					return ForgeConfig.CLIENT.graphics.qualityOption.drawResolution.get();
				}
				public void setDrawResolution(HorizontalResolution newHorizontalResolution)
				{
					ForgeConfig.CLIENT.graphics.qualityOption.drawResolution.set(newHorizontalResolution);
				}
				
				
				
				public int getLodChunkRenderDistance()
				{
					return ForgeConfig.CLIENT.graphics.qualityOption.lodChunkRenderDistance.get();
				}
				public void setLodChunkRenderDistance(int newLodChunkRenderDistance)
				{
					ForgeConfig.CLIENT.graphics.qualityOption.lodChunkRenderDistance.set(newLodChunkRenderDistance);
				}
				
				
				
				public VerticalQuality getVerticalQuality()
				{
					return ForgeConfig.CLIENT.graphics.qualityOption.verticalQuality.get();
				}
				public void setVerticalQuality(VerticalQuality newVerticalQuality)
				{
					ForgeConfig.CLIENT.graphics.qualityOption.verticalQuality.set(newVerticalQuality);
				}
				
				
				
				public HorizontalScale getHorizontalScale()
				{
					return ForgeConfig.CLIENT.graphics.qualityOption.horizontalScale.get();
				}
				public void setHorizontalScale(HorizontalScale newHorizontalScale)
				{
					ForgeConfig.CLIENT.graphics.qualityOption.horizontalScale.set(newHorizontalScale);
				}
				
				
				
				public HorizontalQuality getHorizontalQuality()
				{
					return ForgeConfig.CLIENT.graphics.qualityOption.horizontalQuality.get();
				}
				public void setHorizontalQuality(HorizontalQuality newHorizontalQuality)
				{
					ForgeConfig.CLIENT.graphics.qualityOption.horizontalQuality.set(newHorizontalQuality);
				}
			}
			
			
			public static class FogQualityOption
			{
				
				public FogDistance getFogDistance()
				{
					return ForgeConfig.CLIENT.graphics.fogQualityOption.fogDistance.get();
				}
				public void setFogDistance(FogDistance newFogDistance)
				{
					ForgeConfig.CLIENT.graphics.fogQualityOption.fogDistance.set(newFogDistance);
				}
				
				
				
				public FogDrawOverride getFogDrawOverride()
				{
					return ForgeConfig.CLIENT.graphics.fogQualityOption.fogDrawOverride.get();
				}
				public void setFogDrawOverride(FogDrawOverride newFogDrawOverride)
				{
					ForgeConfig.CLIENT.graphics.fogQualityOption.fogDrawOverride.set(newFogDrawOverride);
				}
				
				
				
				public boolean getDisableVanillaFog()
				{
					return ForgeConfig.CLIENT.graphics.fogQualityOption.disableVanillaFog.get();
				}
				public void setDisableVanillaFog(boolean newDisableVanillaFog)
				{
					ForgeConfig.CLIENT.graphics.fogQualityOption.disableVanillaFog.set(newDisableVanillaFog);
				}
			}
			
			
			public static class AdvancedGraphicsOption
			{
				
				public LodTemplate getLodTemplate()
				{
					return ForgeConfig.CLIENT.graphics.advancedGraphicsOption.lodTemplate.get();
				}
				public void setLodTemplate(LodTemplate newLodTemplate)
				{
					ForgeConfig.CLIENT.graphics.advancedGraphicsOption.lodTemplate.set(newLodTemplate);
				}
				
				
				
				public boolean getDisableDirectionalCulling()
				{
					return ForgeConfig.CLIENT.graphics.advancedGraphicsOption.disableDirectionalCulling.get();
				}
				public void setDisableDirectionalCulling(boolean newDisableDirectionalCulling)
				{
					ForgeConfig.CLIENT.graphics.advancedGraphicsOption.disableDirectionalCulling.set(newDisableDirectionalCulling);
				}
				
				
				
				public boolean getAlwaysDrawAtMaxQuality()
				{
					return ForgeConfig.CLIENT.graphics.advancedGraphicsOption.alwaysDrawAtMaxQuality.get();
				}
				public void setAlwaysDrawAtMaxQuality(boolean newAlwaysDrawAtMaxQuality)
				{
					ForgeConfig.CLIENT.graphics.advancedGraphicsOption.alwaysDrawAtMaxQuality.set(newAlwaysDrawAtMaxQuality);
				}
				
				
				
				public VanillaOverdraw getVanillaOverdraw()
				{
					return ForgeConfig.CLIENT.graphics.advancedGraphicsOption.vanillaOverdraw.get();
				}
				public void setVanillaOverdraw(VanillaOverdraw newVanillaOverdraw)
				{
					ForgeConfig.CLIENT.graphics.advancedGraphicsOption.vanillaOverdraw.set(newVanillaOverdraw);
				}
				
				
				
				public GpuUploadMethod getGpuUploadMethod()
				{
					return ForgeConfig.CLIENT.graphics.advancedGraphicsOption.gpuUploadMethod.get();
				}
				public void setGpuUploadMethod(GpuUploadMethod newDisableVanillaFog)
				{
					ForgeConfig.CLIENT.graphics.advancedGraphicsOption.gpuUploadMethod.set(newDisableVanillaFog);
				}
				
				
				
				public boolean getUseExtendedNearClipPlane()
				{
					return ForgeConfig.CLIENT.graphics.advancedGraphicsOption.useExtendedNearClipPlane.get();
				}
				public void setUseExtendedNearClipPlane(boolean newUseExtendedNearClipPlane)
				{
					ForgeConfig.CLIENT.graphics.advancedGraphicsOption.useExtendedNearClipPlane.set(newUseExtendedNearClipPlane);
				}
			}
		}
		
		
		
		
		//========================//
		// WorldGenerator Configs //
		//========================//
		public static class WorldGenerator
		{
			
			public GenerationPriority getGenerationPriority()
			{
				return ForgeConfig.CLIENT.worldGenerator.generationPriority.get();
			}
			public void setGenerationPriority(GenerationPriority newGenerationPriority)
			{
				ForgeConfig.CLIENT.worldGenerator.generationPriority.set(newGenerationPriority);
			}

			
			
			public DistanceGenerationMode getDistanceGenerationMode()
			{
				return ForgeConfig.CLIENT.worldGenerator.distanceGenerationMode.get();
			}
			public void setDistanceGenerationMode(DistanceGenerationMode newDistanceGenerationMode)
			{
				ForgeConfig.CLIENT.worldGenerator.distanceGenerationMode.set(newDistanceGenerationMode);
			}
			
			
			
			public boolean getAllowUnstableFeatureGeneration()
			{
				return ForgeConfig.CLIENT.worldGenerator.allowUnstableFeatureGeneration.get();
			}
			public void setAllowUnstableFeatureGeneration(boolean newAllowUnstableFeatureGeneration)
			{
				ForgeConfig.CLIENT.worldGenerator.allowUnstableFeatureGeneration.set(newAllowUnstableFeatureGeneration);
			}
			
			
			
			public BlockToAvoid getBlockToAvoid()
			{
				return ForgeConfig.CLIENT.worldGenerator.blockToAvoid.get();
			}
			public void setBlockToAvoid(BlockToAvoid newBlockToAvoid)
			{
				ForgeConfig.CLIENT.worldGenerator.blockToAvoid.set(newBlockToAvoid);
			}
		}
		
		
		
		
		//============================//
		// AdvancedModOptions Configs //
		//============================//
		public static class AdvancedModOptions
		{
			
			public final Threading threading;
			public final Debugging debugging;
			public final Buffers buffers;
			
			public AdvancedModOptions()
			{
				threading = new Threading();
				debugging = new Debugging();
				buffers = new Buffers();
			}
			
			public static class Threading
			{
				
				public int getNumberOfWorldGenerationThreads()
				{
					return ForgeConfig.CLIENT.advancedModOptions.threading.numberOfWorldGenerationThreads.get();
				}
				public void setNumberOfWorldGenerationThreads(int newNumberOfWorldGenerationThreads)
				{
					ForgeConfig.CLIENT.advancedModOptions.threading.numberOfWorldGenerationThreads.set(newNumberOfWorldGenerationThreads);
				}
				
				
				
				public int getNumberOfBufferBuilderThreads()
				{
					return ForgeConfig.CLIENT.advancedModOptions.threading.numberOfBufferBuilderThreads.get();
				}
				public void setNumberOfBufferBuilderThreads(int newNumberOfWorldBuilderThreads)
				{
					ForgeConfig.CLIENT.advancedModOptions.threading.numberOfBufferBuilderThreads.set(newNumberOfWorldBuilderThreads);
				}
			}

			
			
			
			//===============//
			// Debug Options //
			//===============//
			public static class Debugging
			{
				
				public boolean getDrawLods()
				{
					return ForgeConfig.CLIENT.advancedModOptions.debugging.drawLods.get();
				}
				public void setDrawLods(boolean newDrawLods)
				{
					ForgeConfig.CLIENT.advancedModOptions.debugging.drawLods.set(newDrawLods);
				}

				
				
				public DebugMode getDebugMode()
				{
					return ForgeConfig.CLIENT.advancedModOptions.debugging.debugMode.get();
				}
				public void setDebugMode(DebugMode newDebugMode)
				{
					ForgeConfig.CLIENT.advancedModOptions.debugging.debugMode.set(newDebugMode);
				}

				
				
				public boolean getEnableDebugKeybindings()
				{
					return ForgeConfig.CLIENT.advancedModOptions.debugging.enableDebugKeybindings.get();
				}
				public void setEnableDebugKeybindings(boolean newEnableDebugKeybindings)
				{
					ForgeConfig.CLIENT.advancedModOptions.debugging.enableDebugKeybindings.set(newEnableDebugKeybindings);
				}
			}
			
			
			public static class Buffers
			{
				
				public BufferRebuildTimes getRebuildTimes()
				{
					return ForgeConfig.CLIENT.advancedModOptions.buffers.rebuildTimes.get();
				}
				public void setRebuildTimes(BufferRebuildTimes newBufferRebuildTimes)
				{
					ForgeConfig.CLIENT.advancedModOptions.buffers.rebuildTimes.set(newBufferRebuildTimes);
				}
			}
		}
	}
	
}
