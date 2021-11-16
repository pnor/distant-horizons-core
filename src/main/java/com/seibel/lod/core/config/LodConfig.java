package com.seibel.lod.core.config;

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
import com.seibel.lod.core.objects.MinDefaultMax;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.wrappers.config.LodConfigWrapperSingleton;

/**
 * Interacts with the LodConfigWrapper to get the config
 * 
 * @author James Seibel
 * @version 11-14-2021
 */
public class LodConfig
{
	public static final Client client = new Client();
	
	
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
				public static final String DESC = "These settings control how detailed the fake chunks will be.";
				
				
				
				public static final String DRAW_RESOLUTION_DESC = ""
						+ " What is the maximum detail fake chunks should be drawn at? \n"
						+ " " + HorizontalResolution.CHUNK + ": render 1 LOD for each Chunk. \n"
						+ " " + HorizontalResolution.HALF_CHUNK + ": render 4 LODs for each Chunk. \n"
						+ " " + HorizontalResolution.FOUR_BLOCKS + ": render 16 LODs for each Chunk. \n"
						+ " " + HorizontalResolution.TWO_BLOCKS + ": render 64 LODs for each Chunk. \n"
						+ " " + HorizontalResolution.BLOCK + ": render 256 LODs for each Chunk. \n";
				public static final HorizontalResolution DRAW_RESOLUTION_DEFAULT = HorizontalResolution.BLOCK;
				
				public HorizontalResolution getDrawResolution()
				{
					return LodConfigWrapperSingleton.INSTANCE.getClient().LodConfigWrapperSingleton.INSTANCE.qualityOption.getDrawResolution();
				}
				public void setDrawResolution(HorizontalResolution newHorizontalResolution)
				{
					LodConfigWrapperSingleton.INSTANCE.getClient().LodConfigWrapperSingleton.INSTANCE.qualityOption.setDrawResolution(newHorizontalResolution);
				}
				
				
				
				public static final String LOD_CHUNK_RENDER_DISTANCE_DESC = ""
						+ " The mod's render distance, measured in chunks. \n";
				public static final MinDefaultMax<Integer> LOD_CHUNK_RENDER_DISTANCE_MIN_DEFAULT_MAX = new MinDefaultMax<Integer>(16, 64, 1024);
				
				public int getLodChunkRenderDistance()
				{
					return LodConfigWrapperSingleton.INSTANCE.getClient().LodConfigWrapperSingleton.INSTANCE.qualityOption.getLodChunkRenderDistance();
				}
				public void setLodChunkRenderDistance(int newLodChunkRenderDistance)
				{
					LodConfigWrapperSingleton.INSTANCE.getClient().LodConfigWrapperSingleton.INSTANCE.qualityOption.setLodChunkRenderDistance(newLodChunkRenderDistance);
				}
				
				
				
				public static final String VERTICAL_QUALITY_DESC = ""
						+ " This indicates how detailed fake chunks will represent \n"
						+ " overhangs, caves, floating islands, ect. \n"
						+ " Higher options will use more memory and increase GPU usage. \n"
						+ " " + VerticalQuality.LOW + ": uses at max 2 columns per position. \n"
						+ " " + VerticalQuality.MEDIUM + ": uses at max 4 columns per position. \n"
						+ " " + VerticalQuality.HIGH + ": uses at max 8 columns per position. \n";
				public static final VerticalQuality VERTICAL_QUALITY_DEFAULT = VerticalQuality.MEDIUM;
				
				public VerticalQuality getVerticalQuality()
				{
					return LodConfigWrapperSingleton.INSTANCE.getClient().LodConfigWrapperSingleton.INSTANCE.qualityOption.getVerticalQuality();
				}
				public void setVerticalQuality(VerticalQuality newVerticalQuality)
				{
					LodConfigWrapperSingleton.INSTANCE.getClient().LodConfigWrapperSingleton.INSTANCE.qualityOption.setVerticalQuality(newVerticalQuality);
				}
				
				
				
				public static final String HORIZONTAL_SCALE_DESC = ""
						+ " This indicates how quickly fake chunks drop off in quality. \n"
						+ " " + HorizontalScale.LOW + ": quality drops every " + HorizontalScale.LOW.distanceUnit / 16 + " chunks. \n"
						+ " " + HorizontalScale.MEDIUM + ": quality drops every " + HorizontalScale.MEDIUM.distanceUnit / 16 + " chunks. \n"
						+ " " + HorizontalScale.HIGH + ": quality drops every " + HorizontalScale.HIGH.distanceUnit / 16 + " chunks. \n";
				public static final HorizontalScale HORIZONTAL_SCALE_DEFAULT = HorizontalScale.MEDIUM;
				
				public HorizontalScale getHorizontalScale()
				{
					return LodConfigWrapperSingleton.INSTANCE.getClient().LodConfigWrapperSingleton.INSTANCE.qualityOption.getHorizontalScale();
				}
				public void setLodChunkRenderDistance(HorizontalScale newHorizontalScale)
				{
					LodConfigWrapperSingleton.INSTANCE.getClient().LodConfigWrapperSingleton.INSTANCE.qualityOption.setHorizontalScale(newHorizontalScale);
				}
				
				
				
				public static final String HORIZONTAL_QUALITY_DESC = ""
						+ " This indicates the exponential base of the quadratic drop-off \n"
						+ " " + HorizontalQuality.LOWEST + ": base " + HorizontalQuality.LOWEST.quadraticBase + ". \n"
						+ " " + HorizontalQuality.LOW + ": base " + HorizontalQuality.LOW.quadraticBase + ". \n"
						+ " " + HorizontalQuality.MEDIUM + ": base " + HorizontalQuality.MEDIUM.quadraticBase + ". \n"
						+ " " + HorizontalQuality.HIGH + ": base " + HorizontalQuality.HIGH.quadraticBase + ". \n";
				public static final HorizontalQuality HORIZONTAL_QUALITY_DEFAULT = HorizontalQuality.MEDIUM;
				
				public HorizontalQuality getHorizontalQuality()
				{
					return LodConfigWrapperSingleton.INSTANCE.getClient().LodConfigWrapperSingleton.INSTANCE.qualityOption.getHorizontalQuality();
				}
				public void setLodChunkRenderDistance(HorizontalQuality newHorizontalQuality)
				{
					LodConfigWrapperSingleton.INSTANCE.getClient().LodConfigWrapperSingleton.INSTANCE.qualityOption.setHorizontalQuality(newHorizontalQuality);
				}
			}
			
			
			public static class FogQualityOption
			{
				public static final String DESC = "These settings control the fog quality.";
				
				
				
				public static final String FOG_DISTANCE_DESC = ""
						+ " At what distance should Fog be drawn on the fake chunks? \n"
						+ " If the fog cuts off abruptly or you are using Optifine's \"fast\" fog option \n"
						+ " set this to " + FogDistance.NEAR + " or " + FogDistance.FAR + ". \n";
				public static final FogDistance FOG_DISTANCE_DEFAULT = FogDistance.FAR;
				
				public FogDistance getFogDistance()
				{
					return LodConfigWrapperSingleton.INSTANCE.getClient().LodConfigWrapperSingleton.INSTANCE.fogQualityOption.getFogDistance();
				}
				public void setFogDistance(FogDistance newFogDistance)
				{
					LodConfigWrapperSingleton.INSTANCE.getClient().LodConfigWrapperSingleton.INSTANCE.fogQualityOption.setFogDistance(newFogDistance);
				}
				
				
				
				public static final String FOG_DRAW_OVERRIDE_DESC = ""
						+ " When should fog be drawn? \n"
						+ " " + FogDrawOverride.OPTIFINE_SETTING + ": Use whatever Fog setting Optifine is using. If Optifine isn't installed this defaults to " + FogDrawOverride.FANCY + ". \n"
						+ " " + FogDrawOverride.NO_FOG + ": Never draw fog on the LODs \n"
						+ " " + FogDrawOverride.FAST + ": Always draw fast fog on the LODs \n"
						+ " " + FogDrawOverride.FANCY + ": Always draw fancy fog on the LODs (if your graphics card supports it) \n";
				public static final FogDrawOverride FOG_DRAW_OVERRIDE_DEFAULT = FogDrawOverride.FANCY;
				
				public FogDrawOverride getFogDrawOverride()
				{
					return LodConfigWrapperSingleton.INSTANCE.getClient().LodConfigWrapperSingleton.INSTANCE.fogQualityOption.getFogDrawOverride();
				}
				public void setFogDrawOverride(FogDrawOverride newFogDrawOverride)
				{
					LodConfigWrapperSingleton.INSTANCE.getClient().LodConfigWrapperSingleton.INSTANCE.fogQualityOption.setFogDrawOverride(newFogDrawOverride);
				}
				
				
				
				public static final String DISABLE_VANILLA_FOG_DESC = ""
						+ " If true disable Minecraft's fog. \n\n"
						+ ""
						+ " Experimental! May cause issues with Sodium. \n\n"
						+ ""
						+ " Unlike Optifine or Sodium's fog disabling option this won't change \n"
						+ " performance (we don't actually disable the fog, we just tell it to render a infinite distance away). \n"
						+ " May or may not play nice with other mods that edit fog. \n";
				public static final boolean DISABLE_VANILLA_FOG_DEFAULT = false;
				
				public boolean getDisableVanillaFog()
				{
					return LodConfigWrapperSingleton.INSTANCE.getClient().LodConfigWrapperSingleton.INSTANCE.fogQualityOption.getDisableVanillaFog();
				}
				public void setDisableVanillaFog(boolean newDisableVanillaFog)
				{
					LodConfigWrapperSingleton.INSTANCE.getClient().LodConfigWrapperSingleton.INSTANCE.fogQualityOption.setDisableVanillaFog(newDisableVanillaFog);
				}
			}
			
			
			public static class AdvancedGraphicsOption
			{
				public static final String DESC = "Advanced graphics options for the mod";
				
				
				
				public static final String LOD_TEMPLATE_DESC = ""
						+ " How should the LODs be drawn? \n"
						+ " NOTE: Currently only " + LodTemplate.CUBIC + " is implemented! \n"
						+ " \n"
						+ " " + LodTemplate.CUBIC + ": LOD Chunks are drawn as rectangular prisms (boxes). \n"
						+ " " + LodTemplate.TRIANGULAR + ": LOD Chunks smoothly transition between other. \n"
						+ " " + LodTemplate.DYNAMIC + ": LOD Chunks smoothly transition between each other, \n"
						+ " " + "         unless a neighboring chunk is at a significantly different height. \n";
				public static final LodTemplate LOD_TEMPLATE_DEFAULT = LodTemplate.CUBIC;
				
				public LodTemplate getLodTemplate()
				{
					return LodConfigWrapperSingleton.INSTANCE.getClient().LodConfigWrapperSingleton.INSTANCE.advancedGraphicsOption.getLodTemplate();
				}
				public void setLodTemplate(LodTemplate newLodTemplate)
				{
					LodConfigWrapperSingleton.INSTANCE.getClient().LodConfigWrapperSingleton.INSTANCE.advancedGraphicsOption.setLodTemplate(newLodTemplate);
				}
				
				
				
				public static final String DISABLE_DIRECTIONAL_CULLING_DESC = ""
						+ " If false fake chunks behind the player's camera \n"
						+ " aren't drawn, increasing performance. \n\n"
						+ ""
						+ " If true all LODs are drawn, even those behind \n"
						+ " the player's camera, decreasing performance. \n\n"
						+ ""
						+ " Disable this if you see LODs disappearing. \n"
						+ " (Which may happen if you are using a camera mod) \n";
				public static final boolean DISABLE_DIRECTIONAL_CULLING_DEFAULT = false;
				
				public boolean getDisableDirectionalCulling()
				{
					return LodConfigWrapperSingleton.INSTANCE.getClient().LodConfigWrapperSingleton.INSTANCE.advancedGraphicsOption.getDisableDirectionalCulling();
				}
				public void setDisableDirectionalCulling(boolean newDisableDirectionalCulling)
				{
					LodConfigWrapperSingleton.INSTANCE.getClient().LodConfigWrapperSingleton.INSTANCE.advancedGraphicsOption.setDisableDirectionalCulling(newDisableDirectionalCulling);
				}
				
				
				
				public static final String ALWAYS_DRAW_AT_MAD_QUALITY_DESC = ""
						+ " Disable quality falloff, \n"
						+ " all fake chunks will be drawn at the highest \n"
						+ " available detail level. \n\n"
						+ " "
						+ " WARNING: \n"
						+ " This could cause a Out Of Memory crash on render \n"
						+ " distances higher than 128 \n";
				public static final boolean ALWAYS_DRAW_AT_MAD_QUALITY_DEFAULT = false;
				
				public boolean getAlwaysDrawAtMaxQuality()
				{
					return LodConfigWrapperSingleton.INSTANCE.getClient().LodConfigWrapperSingleton.INSTANCE.advancedGraphicsOption.getAlwaysDrawAtMaxQuality();
				}
				public void setAlwaysDrawAtMaxQuality(boolean newAlwaysDrawAtMaxQuality)
				{
					LodConfigWrapperSingleton.INSTANCE.getClient().LodConfigWrapperSingleton.INSTANCE.advancedGraphicsOption.setAlwaysDrawAtMaxQuality(newAlwaysDrawAtMaxQuality);
				}
				
				
				
				public static final String VANILLA_OVERDRAW_DESC = ""
						+ " How often should LODs be drawn on top of regular chunks? \n"
						+ " HALF and ALWAYS will prevent holes in the world, but may look odd for transparent blocks or in caves. \n\n"
						+ " " + VanillaOverdraw.NEVER + ": LODs won't render on top of vanilla chunks. \n"
						+ " " + VanillaOverdraw.BORDER + ": LODs will render only on the border of vanilla chunks preventing only some holes in the world. \n"
						+ " " + VanillaOverdraw.DYNAMIC + ": LODs will render on top of distant vanilla chunks to hide delayed loading. \n"
						+ " " + "     More effective on higher render distances. \n"
						+ " " + "     For vanilla render distances less than or equal to " + LodUtil.MINIMUM_RENDER_DISTANCE_FOR_PARTIAL_OVERDRAW + " \n"
						+ " " + "     " + VanillaOverdraw.NEVER + " or " + VanillaOverdraw.ALWAYS + " may be used depending on the dimension. \n"
						+ " " + VanillaOverdraw.ALWAYS + ": LODs will render on all vanilla chunks preventing holes in the world. \n";
				public static final VanillaOverdraw VANILLA_OVERDRAW_DEFAULT = VanillaOverdraw.DYNAMIC;
				
				public VanillaOverdraw getVanillaOverdraw()
				{
					return LodConfigWrapperSingleton.INSTANCE.getClient().LodConfigWrapperSingleton.INSTANCE.advancedGraphicsOption.getVanillaOverdraw();
				}
				public void setVanillaOverdraw(VanillaOverdraw newVanillaOverdraw)
				{
					LodConfigWrapperSingleton.INSTANCE.getClient().LodConfigWrapperSingleton.INSTANCE.advancedGraphicsOption.setVanillaOverdraw(newVanillaOverdraw);
				}
				
				
				
				public static final String GPU_UPLOAD_METHOD_DESC = ""
						+ " What method should be used to upload geometry to the GPU? \n\n"
						+ ""
						+ " " + GpuUploadMethod.BUFFER_STORAGE + ": Default if OpenGL 4.5 is supported. Fast rendering, no stuttering. \n"
						+ " " + GpuUploadMethod.SUB_DATA + ": Default if OpenGL 4.5 is NOT supported. Fast rendering but may stutter when uploading. \n"
						+ " " + GpuUploadMethod.BUFFER_MAPPING + ": Slow rendering but won't stutter when uploading. Possibly better than " + GpuUploadMethod.SUB_DATA + " if using a integrated GPU. \n";
				public static final GpuUploadMethod GPU_UPLOAD_METHOD_DEFAULT = GpuUploadMethod.BUFFER_STORAGE;
				
				public GpuUploadMethod getGpuUploadMethod()
				{
					return LodConfigWrapperSingleton.INSTANCE.getClient().LodConfigWrapperSingleton.INSTANCE.advancedGraphicsOption.getGpuUploadMethod();
				}
				public void setGpuUploadMethod(GpuUploadMethod newDisableVanillaFog)
				{
					LodConfigWrapperSingleton.INSTANCE.getClient().LodConfigWrapperSingleton.INSTANCE.advancedGraphicsOption.setGpuUploadMethod(newDisableVanillaFog);
				}
				
				
				
				public static final String USE_EXTENDED_NEAR_CLIP_PLANE_DESC = ""
						+ " Will prevent some overdraw issues, but may cause nearby fake chunks to render incorrectly \n"
						+ " especially when in/near an ocean. \n";
				public static final boolean USE_EXTENDED_NEAR_CLIP_PLANE_DEFAULT = false;
				
				public boolean getUseExtendedNearClipPlane()
				{
					return LodConfigWrapperSingleton.INSTANCE.getClient().LodConfigWrapperSingleton.INSTANCE.advancedGraphicsOption.getUseExtendedNearClipPlane();
				}
				public void setGpuUploadMethod(boolean newUseExtendedNearClipPlane)
				{
					LodConfigWrapperSingleton.INSTANCE.getClient().LodConfigWrapperSingleton.INSTANCE.advancedGraphicsOption.setUseExtendedNearClipPlane(newUseExtendedNearClipPlane);
				}
			}
		}
		
		
		
		
		//========================//
		// WorldGenerator Configs //
		//========================//
		public static class WorldGenerator
		{
			public static final String DESC = "These settings control how fake chunks outside your normal view range are generated.";
			
			
			
			public static final String GENERATION_PRIORITY_DESC = ""
					+ " " + GenerationPriority.FAR_FIRST + " \n"
					+ " LODs are generated from low to high detail \n"
					+ " with a small priority for far away regions. \n"
					+ " This fills in the world fastest. \n\n"
					+ ""
					+ " " + GenerationPriority.NEAR_FIRST + " \n"
					+ " LODs are generated around the player \n"
					+ " in a spiral, similar to vanilla minecraft. \n";
			public static final GenerationPriority GENERATION_PRIORITY_DEFAULT = GenerationPriority.FAR_FIRST;
			
			public GenerationPriority getGenerationPriority()
			{
				return LodConfigWrapperSingleton.INSTANCE.LodConfigWrapperSingleton.INSTANCE.worldGenerator.getGenerationPriority();
			}
			public void setGenerationPriority(GenerationPriority newGenerationPriority)
			{
				LodConfigWrapperSingleton.INSTANCE.LodConfigWrapperSingleton.INSTANCE.worldGenerator.setGenerationPriority(newGenerationPriority);
			}
			
			
			
			public static final String DISTANCE_GENERATION_MODE_DESC = ""
					+ " Note: The times listed here are the amount of time it took \n"
					+ "       one of the developer's PC to generate 1 chunk, \n"
					+ "       and are included so you can compare the \n"
					+ "       different generation options. Your mileage may vary. \n"
					+ "\n"
					
					+ " " + DistanceGenerationMode.NONE + " \n"
					+ " Don't run the distance generator. \n"
					
					+ "\n"
					+ " " + DistanceGenerationMode.BIOME_ONLY + " \n"
					+ " Only generate the biomes and use the biome's \n"
					+ " grass color, water color, or snow color. \n"
					+ " Doesn't generate height, everything is shown at sea level. \n"
					+ " Multithreaded - Fastest (2-5 ms) \n"
					
					+ "\n"
					+ " " + DistanceGenerationMode.BIOME_ONLY_SIMULATE_HEIGHT + " \n"
					+ " Same as BIOME_ONLY, except instead \n"
					+ " of always using sea level as the LOD height \n"
					+ " different biome types (mountain, ocean, forest, etc.) \n"
					+ " use predetermined heights to simulate having height data. \n"
					+ " Multithreaded - Fastest (2-5 ms) \n"
					
					+ "\n"
					+ " " + DistanceGenerationMode.SURFACE + " \n"
					+ " Generate the world surface, \n"
					+ " this does NOT include trees, \n"
					+ " or structures. \n"
					+ " Multithreaded - Faster (10-20 ms) \n"
					
					+ "\n"
					+ " " + DistanceGenerationMode.FEATURES + " \n"
					+ " Generate everything except structures. \n"
					+ " WARNING: This may cause world generation bugs or instability! \n"
					+ " Multithreaded - Fast (15-20 ms) \n"
					
					+ "\n"
					+ " " + DistanceGenerationMode.SERVER + " \n"
					+ " Ask the server to generate/load each chunk. \n"
					+ " This will show player made structures, which can \n"
					+ " be useful if you are adding the mod to a pre-existing world. \n"
					+ " This is the most compatible, but causes server/simulation lag. \n"
					+ " SingleThreaded - Slow (15-50 ms, with spikes up to 200 ms) \n";
			public static final DistanceGenerationMode DISTANCE_GENERATION_MODE_DEFAULT = DistanceGenerationMode.SURFACE;
			
			public DistanceGenerationMode getDistanceGenerationMode()
			{
				return LodConfigWrapperSingleton.INSTANCE.LodConfigWrapperSingleton.INSTANCE.worldGenerator.getDistanceGenerationMode();
			}
			public void setDistanceGenerationMode(DistanceGenerationMode newDistanceGenerationMode)
			{
				LodConfigWrapperSingleton.INSTANCE.LodConfigWrapperSingleton.INSTANCE.worldGenerator.setDistanceGenerationMode(newDistanceGenerationMode);
			}
			
			
			
			public static final String ALLOW_UNSTABLE_FEATURE_GENERATION_DESC = ""
					+ " When using the " + DistanceGenerationMode.FEATURES + " generation mode \n"
					+ " some features may not be thread safe, which could \n"
					+ " cause instability and crashes. \n"
					+ " By default (false) those features are skipped, \n"
					+ " improving stability, but decreasing how many features are \n"
					+ " actually generated. \n"
					+ " (for example: some tree generation is unstable, \n"
					+ "               so some trees may not be generated.) \n"
					+ " By setting this to true, all features will be generated, \n"
					+ " but your game will be more unstable and crashes may occur. \n"
					+ " \n"
					+ " I would love to remove this option and always generate everything, \n"
					+ " but I'm not sure how to do that. \n"
					+ " If you are a Java wizard, check out the git issue here: \n"
					+ " https://gitlab.com/jeseibel/minecraft-lod-mod/-/issues/35 \n";
			public static final boolean ALLOW_UNSTABLE_FEATURE_GENERATION_DEFAULT = false;
			
			public boolean getAllowUnstableFeatureGeneration()
			{
				return LodConfigWrapperSingleton.INSTANCE.LodConfigWrapperSingleton.INSTANCE.worldGenerator.getAllowUnstableFeatureGeneration();
			}
			public void setAllowUnstableFeatureGeneration(boolean newAllowUnstableFeatureGeneration)
			{
				LodConfigWrapperSingleton.INSTANCE.LodConfigWrapperSingleton.INSTANCE.worldGenerator.setAllowUnstableFeatureGeneration(newAllowUnstableFeatureGeneration);
			}
			
			
			
			public static final String BLOCK_TO_AVOID_DESC = ""
					+ " " + BlockToAvoid.NONE + ": Use all blocks when generating fake chunks \n\n"
					+ ""
					+ " " + BlockToAvoid.NON_FULL + ": Only use full blocks when generating fake chunks (ignores slabs, lanterns, torches, grass, etc.) \n\n"
					+ ""
					+ " " + BlockToAvoid.NO_COLLISION + ": Only use solid blocks when generating fake chunks (ignores grass, torches, etc.) \n"
					+ ""
					+ " " + BlockToAvoid.BOTH + ": Only use full solid blocks when generating fake chunks \n";
			public static final BlockToAvoid BLOCK_TO_AVOID_DEFAULT = BlockToAvoid.BOTH;
			
			public BlockToAvoid getBlockToAvoid()
			{
				return LodConfigWrapperSingleton.INSTANCE.LodConfigWrapperSingleton.INSTANCE.worldGenerator.getBlockToAvoid();
			}
			public void setBlockToAvoid(BlockToAvoid newBlockToAvoid)
			{
				LodConfigWrapperSingleton.INSTANCE.LodConfigWrapperSingleton.INSTANCE.worldGenerator.setBlockToAvoid(newBlockToAvoid);
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
				public static final String DESC = "These settings control how many CPU threads the mod uses for different tasks.";
				
				
				
				public static final String NUMBER_OF_WORLD_GENERATION_THREADS_DESC = ""
						+ " This is how many threads are used when generating LODs outside \n"
						+ " the normal render distance. \n"
						+ " If you experience stuttering when generating distant LODs, decrease \n"
						+ " this number. If you want to increase LOD generation speed, \n"
						+ " increase this number. \n\n"
						+ ""
						+ " The maximum value is the number of logical processors on your CPU. \n"
						+ " Requires a restart to take effect. \n";
				public static final MinDefaultMax<Integer> NUMBER_OF_WORLD_GENERATION_THREADS_DEFAULT = new MinDefaultMax<Integer>(1, Runtime.getRuntime().availableProcessors() / 2, Runtime.getRuntime().availableProcessors());
				
				public int getNumberOfWorldGenerationThreads()
				{
					return LodConfigWrapperSingleton.INSTANCE.getClient().LodConfigWrapperSingleton.INSTANCE.threading.getNumberOfWorldGenerationThreads();
				}
				public void setNumberOfWorldGenerationThreads(int newNumberOfWorldGenerationThreads)
				{
					LodConfigWrapperSingleton.INSTANCE.getClient().LodConfigWrapperSingleton.INSTANCE.threading.setNumberOfWorldGenerationThreads(newNumberOfWorldGenerationThreads);
				}
				
				
				
				public static final String NUMBER_OF_BUFFER_BUILDER_THREADS_DESC = ""
						+ " This is how many threads are used when building vertex buffers \n"
						+ " (The things sent to your GPU to draw the fake chunks). \n"
						+ " If you experience high CPU usage when NOT generating distant \n"
						+ " fake chunks, lower this number. \n"
						+ " \n"
						+ " The maximum value is the number of logical processors on your CPU. \n"
						+ " Requires a restart to take effect. \n";
				public static final MinDefaultMax<Integer> NUMBER_OF_BUFFER_BUILDER_THREADS_MIN_DEFAULT_MAX = new MinDefaultMax<Integer>(1, Runtime.getRuntime().availableProcessors() / 2, Runtime.getRuntime().availableProcessors());
				
				public int getNumberOfBufferBuilderThreads()
				{
					return LodConfigWrapperSingleton.INSTANCE.getClient().LodConfigWrapperSingleton.INSTANCE.threading.getNumberOfBufferBuilderThreads();
				}
				public void setNumberOfBufferBuilderThreads(int newNumberOfWorldBuilderThreads)
				{
					LodConfigWrapperSingleton.INSTANCE.getClient().LodConfigWrapperSingleton.INSTANCE.threading.setNumberOfBufferBuilderThreads(newNumberOfWorldBuilderThreads);
				}
			}
			
			
			
			
			//===============//
			// Debug Options //
			//===============//
			public static class Debugging
			{
				public static final String DESC = "These settings can be used to look for bugs, or see how certain aspects of the mod work.";
				
				
				
				public static final String DRAW_LODS_DESC = ""
						+ " If true, the mod is enabled and fake chunks will be drawn. \n"
						+ " If false, the mod will still generate fake chunks, \n"
						+ " but they won't be rendered. \n";
				public static final boolean DRAW_LODS_DEFAULT = true;
				
				public boolean getDrawLods()
				{
					return LodConfigWrapperSingleton.INSTANCE.getClient().LodConfigWrapperSingleton.INSTANCE.debugging.getDrawLods();
				}
				public void setDrawLods(boolean newDrawLods)
				{
					LodConfigWrapperSingleton.INSTANCE.getClient().LodConfigWrapperSingleton.INSTANCE.debugging.setDrawLods(newDrawLods);
				}
				
				
				
				public static final String DEBUG_MODE_DESC = ""
						+ " " + DebugMode.OFF + ": Fake chunks will be drawn with their normal colors. \n"
						+ " " + DebugMode.SHOW_DETAIL + ": Fake chunks color will be based on their detail level. \n"
						+ " " + DebugMode.SHOW_DETAIL_WIREFRAME + ": Fake chunks color will be based on their detail level, drawn as a wireframe. \n";
				public static final DebugMode DEBUG_MODE_DEFAULT = DebugMode.OFF;
				
				public DebugMode getDebugMode()
				{
					return LodConfigWrapperSingleton.INSTANCE.getClient().LodConfigWrapperSingleton.INSTANCE.debugging.getDebugMode();
				}
				public void setDebugMode(DebugMode newDebugMode)
				{
					LodConfigWrapperSingleton.INSTANCE.getClient().LodConfigWrapperSingleton.INSTANCE.debugging.setDebugMode(newDebugMode);
				}
				
				
				
				public static final String ENABLE_DEBUG_KEYBINDINGS_DESC = ""
						+ " If true the F4 key can be used to cycle through the different debug modes. \n"
						+ " and the F6 key can be used to enable and disable LOD rendering.";
				public static final boolean ENABLE_DEBUG_KEYBINDINGS_DEFAULT = true;
				
				public boolean getEnableDebugKeybindings()
				{
					return LodConfigWrapperSingleton.INSTANCE.getClient().LodConfigWrapperSingleton.INSTANCE.debugging.getEnableDebugKeybindings();
				}
				public void setEnableDebugKeybindings(boolean newEnableDebugKeybindings)
				{
					LodConfigWrapperSingleton.INSTANCE.getClient().LodConfigWrapperSingleton.INSTANCE.debugging.setEnableDebugKeybindings(newEnableDebugKeybindings);
				}
			}
			
			
			public static class Buffers
			{
				public static final String DESC = "These settings affect how often geometry is rebuilt.";
				
				
				
				public static final String REBUILD_TIMES_DESC = ""
						+ " How frequently should geometry be rebuilt and sent to the GPU? \n"
						+ " Higher settings may cause stuttering, but will prevent holes in the world \n";
				public static final BufferRebuildTimes REBUILD_TIMES_DEFAULT = BufferRebuildTimes.NORMAL;
				
				public BufferRebuildTimes getDrawLods()
				{
					return LodConfigWrapperSingleton.INSTANCE.getClient().getAdvancedModOptions().getBuffers().getRebuildTimes();
				}
				public void setDrawLods(BufferRebuildTimes newBufferRebuildTimes)
				{
					LodConfigWrapperSingleton.INSTANCE.getClient().getAdvancedModOptions().getBuffers().setRebuildTimes(newBufferRebuildTimes);
				}
			}
		}
	}
	
}
