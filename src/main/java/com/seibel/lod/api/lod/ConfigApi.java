package com.seibel.lod.api.lod;

import com.seibel.lod.api.forge.LodConfig;
import com.seibel.lod.enums.config.BlockToAvoid;
import com.seibel.lod.enums.config.BufferRebuildTimes;
import com.seibel.lod.enums.config.DistanceGenerationMode;
import com.seibel.lod.enums.config.GenerationPriority;
import com.seibel.lod.enums.config.GpuUploadMethod;
import com.seibel.lod.enums.config.HorizontalQuality;
import com.seibel.lod.enums.config.HorizontalResolution;
import com.seibel.lod.enums.config.HorizontalScale;
import com.seibel.lod.enums.config.LodTemplate;
import com.seibel.lod.enums.config.VanillaOverdraw;
import com.seibel.lod.enums.config.VerticalQuality;
import com.seibel.lod.enums.rendering.DebugMode;
import com.seibel.lod.enums.rendering.FogDistance;
import com.seibel.lod.enums.rendering.FogDrawOverride;
import com.seibel.lod.objects.MinDefaultMax;
import com.seibel.lod.util.LodUtil;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * This holds the config defaults and setters/getters
 * that should be hooked into the host mod loader (Fabric, Forge, etc.).
 * 
 * @author James Seibel
 * @version 11-14-2021
 */
public class ConfigApi
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
				public static final HorizontalResolution DRAW_RESOLUTION_DEFAULT = HorizontalResolution.BLOCK;
				public HorizontalResolution getDrawResolution()
				{
					return LodConfig.CLIENT.graphics.qualityOption.drawResolution.get();
				}
				public void setDrawResolution(HorizontalResolution newHorizontalResolution)
				{
					LodConfig.CLIENT.graphics.qualityOption.drawResolution.set(newHorizontalResolution);
				}
				
				public static final MinDefaultMax<Integer> LOD_CHUNK_RENDER_DISTANCE_MIN_DEFAULT_MAX = new MinDefaultMax<Integer>(32, 64, 1024);
				public int getLodChunkRenderDistance()
				{
					return LodConfig.CLIENT.graphics.qualityOption.lodChunkRenderDistance.get();
				}
				public void setLodChunkRenderDistance(int newLodChunkRenderDistance)
				{
					LodConfig.CLIENT.graphics.qualityOption.lodChunkRenderDistance.set(newLodChunkRenderDistance);
				}
				
				public static final VerticalQuality VERTICAL_QUALITY_DEFAULT = VerticalQuality.MEDIUM;
				public VerticalQuality getVerticalQuality()
				{
					return LodConfig.CLIENT.graphics.qualityOption.verticalQuality.get();
				}
				public void setVerticalQuality(VerticalQuality newVerticalQuality)
				{
					LodConfig.CLIENT.graphics.qualityOption.verticalQuality.set(newVerticalQuality);
				}
				
				public static final HorizontalScale HORIZONTAL_SCALE_DEFAULT = HorizontalScale.MEDIUM;
				public HorizontalScale getHorizontalScale()
				{
					return LodConfig.CLIENT.graphics.qualityOption.horizontalScale.get();
				}
				public void setLodChunkRenderDistance(HorizontalScale newHorizontalScale)
				{
					LodConfig.CLIENT.graphics.qualityOption.horizontalScale.set(newHorizontalScale);
				}
				
				public static final HorizontalQuality HORIZONTAL_QUALITY_DEFAULT = HorizontalQuality.MEDIUM;
				public HorizontalQuality getHorizontalQuality()
				{
					return LodConfig.CLIENT.graphics.qualityOption.horizontalQuality.get();
				}
				public void setLodChunkRenderDistance(HorizontalQuality newHorizontalQuality)
				{
					LodConfig.CLIENT.graphics.qualityOption.horizontalQuality.set(newHorizontalQuality);
				}
			}
			
			
			public static class FogQualityOption
			{
				public final ForgeConfigSpec.EnumValue<FogDistance> fogDistance;
				
				public final ForgeConfigSpec.EnumValue<FogDrawOverride> fogDrawOverride;
				
				public final ForgeConfigSpec.BooleanValue disableVanillaFog;
				
				FogQualityOption(ForgeConfigSpec.Builder builder)
				{
					
					builder.comment("These settings control the fog quality.").push(this.getClass().getSimpleName());
					
					fogDistance = builder
							.comment("\n\n"
									+ " At what distance should Fog be drawn on the fake chunks? \n"
									+ " If the fog cuts off abruptly or you are using Optifine's \"fast\" fog option \n"
									+ " set this to " + FogDistance.NEAR + " or " + FogDistance.FAR + ". \n")
							.defineEnum("Fog Distance", FogDistance.FAR);
					
					fogDrawOverride = builder
							.comment("\n\n"
									+ " When should fog be drawn? \n"
									+ " " + FogDrawOverride.OPTIFINE_SETTING + ": Use whatever Fog setting Optifine is using. If Optifine isn't installed this defaults to " + FogDrawOverride.FANCY + ". \n"
									+ " " + FogDrawOverride.NO_FOG + ": Never draw fog on the LODs \n"
									+ " " + FogDrawOverride.FAST + ": Always draw fast fog on the LODs \n"
									+ " " + FogDrawOverride.FANCY + ": Always draw fancy fog on the LODs (if your graphics card supports it) \n")
							.defineEnum("Fog Draw Override", FogDrawOverride.FANCY);
					
					disableVanillaFog = builder
							.comment("\n\n"
									+ " If true disable Minecraft's fog. \n\n"
									+ ""
									+ " Experimental! May cause issues with Sodium. \n\n"
									+ ""
									+ " Unlike Optifine or Sodium's fog disabling option this won't change \n"
									+ " performance (we don't actually disable the fog, we just tell it to render a infinite distance away). \n"
									+ " May or may not play nice with other mods that edit fog. \n")
							.define("Experimental Disable Vanilla Fog", false);
					
					builder.pop();
				}
			}
			
			
			public static class AdvancedGraphicsOption
			{
				public final ForgeConfigSpec.EnumValue<LodTemplate> lodTemplate;
				
				public final ForgeConfigSpec.BooleanValue disableDirectionalCulling;
				
				public final ForgeConfigSpec.BooleanValue alwaysDrawAtMaxQuality;
				
				public final ForgeConfigSpec.EnumValue<VanillaOverdraw> vanillaOverdraw;
				
				public final ForgeConfigSpec.EnumValue<GpuUploadMethod> gpuUploadMethod;
				
				public final ForgeConfigSpec.BooleanValue useExtendedNearClipPlane;
				
				AdvancedGraphicsOption(ForgeConfigSpec.Builder builder)
				{
					
					builder.comment("Advanced graphics option for the mod").push(this.getClass().getSimpleName());
					
					lodTemplate = builder
							.comment("\n\n"
									+ " How should the LODs be drawn? \n"
									+ " NOTE: Currently only " + LodTemplate.CUBIC + " is implemented! \n"
									+ " \n"
									+ " " + LodTemplate.CUBIC + ": LOD Chunks are drawn as rectangular prisms (boxes). \n"
									+ " " + LodTemplate.TRIANGULAR + ": LOD Chunks smoothly transition between other. \n"
									+ " " + LodTemplate.DYNAMIC + ": LOD Chunks smoothly transition between each other, \n"
									+ " " + "         unless a neighboring chunk is at a significantly different height. \n")
							.defineEnum("LOD Template", LodTemplate.CUBIC);
					
					disableDirectionalCulling = builder
							.comment("\n\n"
									+ " If false fake chunks behind the player's camera \n"
									+ " aren't drawn, increasing performance. \n\n"
									+ ""
									+ " If true all LODs are drawn, even those behind \n"
									+ " the player's camera, decreasing performance. \n\n"
									+ ""
									+ " Disable this if you see LODs disappearing. \n"
									+ " (Which may happen if you are using a camera mod) \n")
							.define("Disable Directional Culling", false);
					
					alwaysDrawAtMaxQuality = builder
							.comment("\n\n"
									+ " Disable quality falloff, \n"
									+ " all fake chunks will be drawn at the highest \n"
									+ " available detail level. \n\n"
									+ " "
									+ " WARNING: \n"
									+ " This could cause a Out Of Memory crash on render \n"
									+ " distances higher than 128 \n")
							.define("Always Use Max Quality", false);
					
					vanillaOverdraw = builder
							.comment("\n\n"
									+ " How often should LODs be drawn on top of regular chunks? \n"
									+ " HALF and ALWAYS will prevent holes in the world, but may look odd for transparent blocks or in caves. \n\n"
									+ " " + VanillaOverdraw.NEVER + ": LODs won't render on top of vanilla chunks. \n"
									+ " " + VanillaOverdraw.BORDER + ": LODs will render only on the border of vanilla chunks preventing only some holes in the world. \n"
									+ " " + VanillaOverdraw.DYNAMIC + ": LODs will render on top of distant vanilla chunks to hide delayed loading. \n"
									+ " " + "     More effective on higher render distances. \n"
									+ " " + "     For vanilla render distances less than or equal to " + LodUtil.MINIMUM_RENDER_DISTANCE_FOR_PARTIAL_OVERDRAW + " \n"
									+ " " + "     " + VanillaOverdraw.NEVER + " or " + VanillaOverdraw.ALWAYS + " may be used depending on the dimension. \n"
									+ " " + VanillaOverdraw.ALWAYS + ": LODs will render on all vanilla chunks preventing holes in the world. \n")
							.defineEnum("Vanilla Overdraw", VanillaOverdraw.DYNAMIC);
					
					gpuUploadMethod = builder
							.comment("\n\n"
									+ " What method should be used to upload geometry to the GPU? \n\\n"
									+ ""
									+ " " + GpuUploadMethod.BUFFER_STORAGE + ": Default if OpenGL 4.5 is supported. Fast rendering, no stuttering. \n"
									+ " " + GpuUploadMethod.SUB_DATA + ": Default if OpenGL 4.5 is NOT supported. Fast rendering but may stutter when uploading. \n"
									+ " " + GpuUploadMethod.BUFFER_MAPPING + ": Slow rendering but won't stutter when uploading. Possibly better than " + GpuUploadMethod.SUB_DATA + " if using a integrated GPU. \n")
							.defineEnum("GPU Upload Method", GpuUploadMethod.BUFFER_STORAGE);
					
					// This is a temporary fix (like vanilla overdraw)
					// hopefully we can remove both once we get individual chunk rendering figured out
					useExtendedNearClipPlane = builder
							.comment("\n\n"
									+ " Will prevent some overdraw issues, but may cause nearby fake chunks to render incorrectly \n"
									+ " especially when in/near an ocean. \n")
							.define("Use Extended Near Clip Plane", false);
					
					
					builder.pop();
				}
			}
		}
		
		
		
		
		//========================//
		// WorldGenerator Configs //
		//========================//
		public static class WorldGenerator
		{
			public final ForgeConfigSpec.EnumValue<GenerationPriority> generationPriority;
			public final ForgeConfigSpec.EnumValue<DistanceGenerationMode> distanceGenerationMode;
			public final ForgeConfigSpec.BooleanValue allowUnstableFeatureGeneration;
			public final ForgeConfigSpec.EnumValue<BlockToAvoid> blockToAvoid;
			//public final ForgeConfigSpec.BooleanValue useExperimentalPreGenLoading;
			
			WorldGenerator(ForgeConfigSpec.Builder builder)
			{
				builder.comment("These settings control how fake chunks outside your normal view range are generated.").push("Generation");
				
				generationPriority = builder
						.comment("\n\n"
								+ " " + GenerationPriority.FAR_FIRST + " \n"
								+ " LODs are generated from low to high detail \n"
								+ " with a small priority for far away regions. \n"
								+ " This fills in the world fastest. \n\n"
								+ ""
								+ " " + GenerationPriority.NEAR_FIRST + " \n"
								+ " LODs are generated around the player \n"
								+ " in a spiral, similar to vanilla minecraft. \n")
						.defineEnum("Generation Priority", GenerationPriority.FAR_FIRST);
				
				distanceGenerationMode = builder
						.comment("\n\n"
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
								+ " SingleThreaded - Slow (15-50 ms, with spikes up to 200 ms) \n")
						.defineEnum("Distance Generation Mode", DistanceGenerationMode.SURFACE);
				
				allowUnstableFeatureGeneration = builder
						.comment("\n\n"
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
								+ " https://gitlab.com/jeseibel/minecraft-lod-mod/-/issues/35 \n")
						.define("Allow Unstable Feature Generation", false);
				
				blockToAvoid = builder
						.comment("\n\n"
								+ " " + BlockToAvoid.NONE + ": Use all blocks when generating fake chunks \n\n"
								+ ""
								+ " " + BlockToAvoid.NON_FULL + ": Only use full blocks when generating fake chunks (ignores slabs, lanterns, torches, grass, etc.) \n\n"
								+ ""
								+ " " + BlockToAvoid.NO_COLLISION + ": Only use solid blocks when generating fake chunks (ignores grass, torches, etc.) \n"
								+ ""
								+ " " + BlockToAvoid.BOTH + ": Only use full solid blocks when generating fake chunks \n"
								+ "\n")
						.defineEnum("Block to avoid", BlockToAvoid.BOTH);
				
				/*useExperimentalPreGenLoading = builder
						 .comment("\n\n"
								+ " if a chunk has been pre-generated, then the mod would use the real chunk for the \n"
								+ "fake chunk creation. May require a deletion of the lod file to see the result. \n")
						 .define("Use pre-generated chunks", false);*/
				builder.pop();
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
			
			public AdvancedModOptions(ForgeConfigSpec.Builder builder)
			{
				builder.comment("Advanced mod settings").push(this.getClass().getSimpleName());
				{
					threading = new Threading(builder);
					debugging = new Debugging(builder);
					buffers = new Buffers(builder);
				}
				builder.pop();
			}
			
			public static class Threading
			{
				public final ForgeConfigSpec.IntValue numberOfWorldGenerationThreads;
				public final ForgeConfigSpec.IntValue numberOfBufferBuilderThreads;
				
				Threading(ForgeConfigSpec.Builder builder)
				{
					builder.comment("These settings control how many CPU threads the mod uses for different tasks.").push(this.getClass().getSimpleName());
					
					numberOfWorldGenerationThreads = builder
							.comment("\n\n"
									+ " This is how many threads are used when generating LODs outside \n"
									+ " the normal render distance. \n"
									+ " If you experience stuttering when generating distant LODs, decrease \n"
									+ " this number. If you want to increase LOD generation speed, \n"
									+ " increase this number. \n\n"
									+ ""
									+ " The maximum value is the number of logical processors on your CPU. \n"
									+ " Requires a restart to take effect. \n")
							.defineInRange("numberOfWorldGenerationThreads", Math.max(1, Runtime.getRuntime().availableProcessors() / 2), 1, Runtime.getRuntime().availableProcessors());
					
					numberOfBufferBuilderThreads = builder
							.comment("\n\n"
									+ " This is how many threads are used when building vertex buffers \n"
									+ " (The things sent to your GPU to draw the fake chunks). \n"
									+ " If you experience high CPU usage when NOT generating distant \n"
									+ " fake chunks, lower this number. \n"
									+ " \n"
									+ " The maximum value is the number of logical processors on your CPU. \n"
									+ " Requires a restart to take effect. \n")
							.defineInRange("numberOfBufferBuilderThreads", Math.max(1, Runtime.getRuntime().availableProcessors() / 2), 1, Runtime.getRuntime().availableProcessors());
					
					builder.pop();
				}
			}

			
			
			
			//===============//
			// Debug Options //
			//===============//
			public static class Debugging
			{
				public final ForgeConfigSpec.BooleanValue drawLods;
				public final ForgeConfigSpec.EnumValue<DebugMode> debugMode;
				public final ForgeConfigSpec.BooleanValue enableDebugKeybindings;
				
				Debugging(ForgeConfigSpec.Builder builder)
				{
					builder.comment("These settings can be used to look for bugs, or see how certain aspects of the mod work.").push(this.getClass().getSimpleName());
					
					drawLods = builder
							.comment("\n\n"
									+ " If true, the mod is enabled and fake chunks will be drawn. \n"
									+ " If false, the mod will still generate fake chunks, \n"
									+ " but they won't be rendered. \n")
							.define("Enable Rendering", true);
					
					debugMode = builder
							.comment("\n\n"
									+ " " + DebugMode.OFF + ": Fake chunks will be drawn with their normal colors. \n"
									+ " " + DebugMode.SHOW_DETAIL + ": Fake chunks color will be based on their detail level. \n"
									+ " " + DebugMode.SHOW_DETAIL_WIREFRAME + ": Fake chunks color will be based on their detail level, drawn as a wireframe. \n")
							.defineEnum("Debug Mode", DebugMode.OFF);
					
					enableDebugKeybindings = builder
							.comment("\n\n"
									+ " If true the F4 key can be used to cycle through the different debug modes. \n"
									+ " and the F6 key can be used to enable and disable LOD rendering.")
							.define("Enable Debug Keybinding", false);
					
					builder.pop();
				}
			}
			
			
			public static class Buffers
			{
				public final ForgeConfigSpec.EnumValue<BufferRebuildTimes> rebuildTimes;
				
				Buffers(ForgeConfigSpec.Builder builder)
				{
					builder.comment("These settings affect how often geometry is are built.").push(this.getClass().getSimpleName());
					
					rebuildTimes = builder
							.comment("\n\n"
									+ " How frequently should geometry be rebuilt and sent to the GPU? \n"
									+ " Higher settings may cause stuttering, but will prevent holes in the world \n")
							.defineEnum("rebuildFrequency", BufferRebuildTimes.NORMAL);
					
					builder.pop();
				}
			}
		}
	}
	
}
