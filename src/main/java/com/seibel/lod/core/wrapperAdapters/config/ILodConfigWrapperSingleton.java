package com.seibel.lod.core.wrapperAdapters.config;

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
 * @version 11-15-2021
 */
public interface ILodConfigWrapperSingleton
{
	public ILodConfigWrapperSingleton getInstance();
	
	public IClient getClient();
	
	public interface IClient
	{
		public IGraphics getGraphics();
		public IWorldGenerator getWorldGenerator();
		public IAdvancedModOptions getAdvancedModOptions();
		
		
		//==================//
		// Graphics Configs //
		//==================//
		public interface IGraphics
		{
			public IQualityOption getQualityOption();
			public IFogQualityOption getFogQualityOption();
			public IAdvancedGraphicsOption getAdvancedGraphicsOption();
			
			
			public interface IQualityOption
			{
				public HorizontalResolution getDrawResolution();
				public void setDrawResolution(HorizontalResolution newHorizontalResolution);
				
				public int getLodChunkRenderDistance();
				public void setLodChunkRenderDistance(int newLodChunkRenderDistance);
				
				public VerticalQuality getVerticalQuality();
				public void setVerticalQuality(VerticalQuality newVerticalQuality);
				
				public HorizontalScale getHorizontalScale();
				public void setHorizontalScale(HorizontalScale newHorizontalScale);
				
				public HorizontalQuality getHorizontalQuality();
				public void setHorizontalQuality(HorizontalQuality newHorizontalQuality);
			}
			
			public interface IFogQualityOption
			{
				public FogDistance getFogDistance();
				public void setFogDistance(FogDistance newFogDistance);
				
				public FogDrawOverride getFogDrawOverride();
				public void setFogDrawOverride(FogDrawOverride newFogDrawOverride);
				
				public boolean getDisableVanillaFog();
				public void setDisableVanillaFog(boolean newDisableVanillaFog);
			}
			
			public interface IAdvancedGraphicsOption
			{
				public LodTemplate getLodTemplate();
				public void setLodTemplate(LodTemplate newLodTemplate);
				
				public boolean getDisableDirectionalCulling();
				public void setDisableDirectionalCulling(boolean newDisableDirectionalCulling);
				
				public boolean getAlwaysDrawAtMaxQuality();
				public void setAlwaysDrawAtMaxQuality(boolean newAlwaysDrawAtMaxQuality);
				
				public VanillaOverdraw getVanillaOverdraw();
				public void setVanillaOverdraw(VanillaOverdraw newVanillaOverdraw);
				
				public GpuUploadMethod getGpuUploadMethod();
				public void setGpuUploadMethod(GpuUploadMethod newDisableVanillaFog);
				
				public boolean getUseExtendedNearClipPlane();
				public void setUseExtendedNearClipPlane(boolean newUseExtendedNearClipPlane);
			}
		}
		
		
		
		
		//========================//
		// WorldGenerator Configs //
		//========================//
		public interface IWorldGenerator
		{
			public GenerationPriority getGenerationPriority();
			public void setGenerationPriority(GenerationPriority newGenerationPriority);

			public DistanceGenerationMode getDistanceGenerationMode();
			public void setDistanceGenerationMode(DistanceGenerationMode newDistanceGenerationMode);
			
			public boolean getAllowUnstableFeatureGeneration();
			public void setAllowUnstableFeatureGeneration(boolean newAllowUnstableFeatureGeneration);
			
			public BlockToAvoid getBlockToAvoid();
			public void setBlockToAvoid(BlockToAvoid newBlockToAvoid);
		}
		
		
		
		
		//============================//
		// AdvancedModOptions Configs //
		//============================//
		public interface IAdvancedModOptions
		{
			public IThreading getThreading();
			public IDebugging getDebugging();
			public IBuffers getBuffers();
			
			
			public interface IThreading
			{
				public int getNumberOfWorldGenerationThreads();
				public void setNumberOfWorldGenerationThreads(int newNumberOfWorldGenerationThreads);
				
				public int getNumberOfBufferBuilderThreads();
				public void setNumberOfBufferBuilderThreads(int newNumberOfWorldBuilderThreads);
			}
			
			public interface IDebugging
			{
				public boolean getDrawLods();
				public void setDrawLods(boolean newDrawLods);
				
				public DebugMode getDebugMode();
				public void setDebugMode(DebugMode newDebugMode);
								
				public boolean getEnableDebugKeybindings();
				public void setEnableDebugKeybindings(boolean newEnableDebugKeybindings);
			}
			
			public interface IBuffers
			{
				public BufferRebuildTimes getRebuildTimes();
				public void setRebuildTimes(BufferRebuildTimes newBufferRebuildTimes);
			}
		}
	}
	
}
