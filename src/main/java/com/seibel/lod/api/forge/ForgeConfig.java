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

package com.seibel.lod.api.forge;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.seibel.lod.ModInfo;
import com.seibel.lod.config.LodConfig;
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

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

/**
 * This handles any configuration the user has access to.
 * @author Leonardo Amato
 * @author James Seibel
 * @version 11-14-2021
 */
@Mod.EventBusSubscriber
public class ForgeConfig
{
	// CONFIG STRUCTURE
	// 	-> Client
	//		|
	//		|-> Graphics
	//		|		|-> QualityOption
	//		|		|-> FogQualityOption
	//		|		|-> AdvancedGraphicsOption
	//		|
	//		|-> World Generation
	//		|
	//		|-> Advanced Mod Option
	//				|-> Threads
	//				|-> Buffers
	//				|-> Debugging
	
	
	
	public static class Client
	{
		public final Graphics graphics;
		public final WorldGenerator worldGenerator;
		public final AdvancedModOptions advancedModOptions;
		
		
		//================//
		// Client Configs //
		//================//
		public Client(ForgeConfigSpec.Builder builder)
		{
			builder.push(this.getClass().getSimpleName());
			{
				graphics = new Graphics(builder);
				worldGenerator = new WorldGenerator(builder);
				advancedModOptions = new AdvancedModOptions(builder);
			}
			builder.pop();
		}
		
		
		//==================//
		// Graphics Configs //
		//==================//
		public static class Graphics
		{
			
			public final QualityOption qualityOption;
			public final FogQualityOption fogQualityOption;
			public final AdvancedGraphicsOption advancedGraphicsOption;
			
			Graphics(ForgeConfigSpec.Builder builder)
			{
				builder.comment("These settings control how the mod will look in game").push("Graphics");
				{
					qualityOption = new QualityOption(builder);
					advancedGraphicsOption = new AdvancedGraphicsOption(builder);
					fogQualityOption = new FogQualityOption(builder);
				}
				builder.pop();
			}
			
			
			public static class QualityOption
			{
				public final ForgeConfigSpec.EnumValue<HorizontalResolution> drawResolution;
				
				public final ForgeConfigSpec.IntValue lodChunkRenderDistance;
				
				public final ForgeConfigSpec.EnumValue<VerticalQuality> verticalQuality;
				
				public final ForgeConfigSpec.EnumValue<HorizontalScale> horizontalScale;
				
				public final ForgeConfigSpec.EnumValue<HorizontalQuality> horizontalQuality;
				
				
				QualityOption(ForgeConfigSpec.Builder builder)
				{
					builder.comment(LodConfig.Client.Graphics.QualityOption.DESC).push(this.getClass().getSimpleName());
					
					verticalQuality = builder
							.comment("\n\n"
									+ LodConfig.Client.Graphics.QualityOption.VERTICAL_QUALITY_DESC)
							.defineEnum("Vertical Quality", LodConfig.Client.Graphics.QualityOption.VERTICAL_QUALITY_DEFAULT);
					
					horizontalScale = builder
							.comment("\n\n"
									+ LodConfig.Client.Graphics.QualityOption.HORIZONTAL_SCALE_DESC)
							.defineEnum("Horizontal Scale", LodConfig.Client.Graphics.QualityOption.HORIZONTAL_SCALE_DEFAULT);
					
					horizontalQuality = builder
							.comment("\n\n"
									+ LodConfig.Client.Graphics.QualityOption.HORIZONTAL_QUALITY_DESC)
							.defineEnum("Horizontal Quality", LodConfig.Client.Graphics.QualityOption.HORIZONTAL_QUALITY_DEFAULT);
					
					drawResolution = builder
							.comment("\n\n"
									+ LodConfig.Client.Graphics.QualityOption.DRAW_RESOLUTION_DESC)
							.defineEnum("Block size", LodConfig.Client.Graphics.QualityOption.DRAW_RESOLUTION_DEFAULT);
					
					MinDefaultMax<Integer> minDefaultMax = LodConfig.Client.Graphics.QualityOption.LOD_CHUNK_RENDER_DISTANCE_MIN_DEFAULT_MAX;
					lodChunkRenderDistance = builder
							.comment("\n\n"
									+ LodConfig.Client.Graphics.QualityOption.LOD_CHUNK_RENDER_DISTANCE_DESC)
							.defineInRange("Lod Render Distance", minDefaultMax.defaultValue, minDefaultMax.minValue, minDefaultMax.maxValue);
					
					builder.pop();
				}
			}
			
			
			public static class FogQualityOption
			{
				public final ForgeConfigSpec.EnumValue<FogDistance> fogDistance;
				
				public final ForgeConfigSpec.EnumValue<FogDrawOverride> fogDrawOverride;
				
				public final ForgeConfigSpec.BooleanValue disableVanillaFog;
				
				FogQualityOption(ForgeConfigSpec.Builder builder)
				{
					
					builder.comment(LodConfig.Client.Graphics.FogQualityOption.DESC).push(this.getClass().getSimpleName());
					
					fogDistance = builder
							.comment("\n\n"
									+ LodConfig.Client.Graphics.FogQualityOption.FOG_DISTANCE_DESC)
							.defineEnum("Fog Distance", LodConfig.Client.Graphics.FogQualityOption.FOG_DISTANCE_DEFAULT);
					
					fogDrawOverride = builder
							.comment("\n\n"
									+ LodConfig.Client.Graphics.FogQualityOption.FOG_DRAW_OVERRIDE_DESC)
							.defineEnum("Fog Draw Override", LodConfig.Client.Graphics.FogQualityOption.FOG_DRAW_OVERRIDE_DEFAULT);
					
					disableVanillaFog = builder
							.comment("\n\n"
									+ LodConfig.Client.Graphics.FogQualityOption.DISABLE_VANILLA_FOG_DESC)
							.define("Experimental Disable Vanilla Fog", LodConfig.Client.Graphics.FogQualityOption.DISABLE_VANILLA_FOG_DEFAULT);
					
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
					
					builder.comment(LodConfig.Client.Graphics.AdvancedGraphicsOption.DESC).push(this.getClass().getSimpleName());
					
					lodTemplate = builder
							.comment("\n\n"
									+ LodConfig.Client.Graphics.AdvancedGraphicsOption.LOD_TEMPLATE_DESC)
							.defineEnum("LOD Template", LodConfig.Client.Graphics.AdvancedGraphicsOption.LOD_TEMPLATE_DEFAULT);
					
					disableDirectionalCulling = builder
							.comment("\n\n"
									+ LodConfig.Client.Graphics.AdvancedGraphicsOption.DISABLE_DIRECTIONAL_CULLING_DESC)
							.define("Disable Directional Culling", LodConfig.Client.Graphics.AdvancedGraphicsOption.DISABLE_DIRECTIONAL_CULLING_DEFAULT);
					
					alwaysDrawAtMaxQuality = builder
							.comment("\n\n"
									+ LodConfig.Client.Graphics.AdvancedGraphicsOption.ALWAYS_DRAW_AT_MAD_QUALITY_DESC)
							.define("Always Use Max Quality", LodConfig.Client.Graphics.AdvancedGraphicsOption.ALWAYS_DRAW_AT_MAD_QUALITY_DEFAULT);
					
					vanillaOverdraw = builder
							.comment("\n\n"
									+ LodConfig.Client.Graphics.AdvancedGraphicsOption.VANILLA_OVERDRAW_DESC)
							.defineEnum("Vanilla Overdraw", LodConfig.Client.Graphics.AdvancedGraphicsOption.VANILLA_OVERDRAW_DEFAULT);
					
					gpuUploadMethod = builder
							.comment("\n\n"
									+ LodConfig.Client.Graphics.AdvancedGraphicsOption.GPU_UPLOAD_METHOD_DESC)
							.defineEnum("GPU Upload Method", LodConfig.Client.Graphics.AdvancedGraphicsOption.GPU_UPLOAD_METHOD_DEFAULT);
					
					// This is a temporary fix (like vanilla overdraw)
					// hopefully we can remove both once we get individual chunk rendering figured out
					useExtendedNearClipPlane = builder
							.comment("\n\n"
									+ LodConfig.Client.Graphics.AdvancedGraphicsOption.USE_EXTENDED_NEAR_CLIP_PLANE_DESC)
							.define("Use Extended Near Clip Plane", LodConfig.Client.Graphics.AdvancedGraphicsOption.USE_EXTENDED_NEAR_CLIP_PLANE_DEFAULT);
					
					
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
				builder.comment(LodConfig.Client.WorldGenerator.DESC).push("Generation");
				
				generationPriority = builder
						.comment("\n\n"
								+ LodConfig.Client.WorldGenerator.GENERATION_PRIORITY_DESC)
						.defineEnum("Generation Priority", LodConfig.Client.WorldGenerator.GENERATION_PRIORITY_DEFAULT);
				
				distanceGenerationMode = builder
						.comment("\n\n"
								+ LodConfig.Client.WorldGenerator.DISTANCE_GENERATION_MODE_DESC)
						.defineEnum("Distance Generation Mode", LodConfig.Client.WorldGenerator.DISTANCE_GENERATION_MODE_DEFAULT);
				
				allowUnstableFeatureGeneration = builder
						.comment("\n\n"
								+ LodConfig.Client.WorldGenerator.ALLOW_UNSTABLE_FEATURE_GENERATION_DESC)
						.define("Allow Unstable Feature Generation", LodConfig.Client.WorldGenerator.ALLOW_UNSTABLE_FEATURE_GENERATION_DEFAULT);
				
				blockToAvoid = builder
						.comment("\n\n"
								+ LodConfig.Client.WorldGenerator.BLOCK_TO_AVOID_DESC)
						.defineEnum("Block to avoid", LodConfig.Client.WorldGenerator.BLOCK_TO_AVOID_DEFAULT);
				
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
					builder.comment(LodConfig.Client.AdvancedModOptions.Threading.DESC).push(this.getClass().getSimpleName());
					
					MinDefaultMax<Integer> minDefaultMax = LodConfig.Client.AdvancedModOptions.Threading.NUMBER_OF_WORLD_GENERATION_THREADS_DEFAULT;
					numberOfWorldGenerationThreads = builder
							.comment("\n\n"
									+ LodConfig.Client.AdvancedModOptions.Threading.NUMBER_OF_WORLD_GENERATION_THREADS_DESC)
							.defineInRange("numberOfWorldGenerationThreads", minDefaultMax.defaultValue, minDefaultMax.minValue, minDefaultMax.maxValue);
					
					
					minDefaultMax = LodConfig.Client.AdvancedModOptions.Threading.NUMBER_OF_BUFFER_BUILDER_THREADS_MIN_DEFAULT_MAX;
					numberOfBufferBuilderThreads = builder
							.comment("\n\n"
									+ LodConfig.Client.AdvancedModOptions.Threading.NUMBER_OF_BUFFER_BUILDER_THREADS_MIN_DEFAULT_MAX)
							.defineInRange("numberOfBufferBuilderThreads", minDefaultMax.defaultValue, minDefaultMax.minValue, minDefaultMax.maxValue);
					
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
					builder.comment(LodConfig.Client.AdvancedModOptions.Debugging.DESC).push(this.getClass().getSimpleName());
					
					drawLods = builder
							.comment("\n\n"
									+ LodConfig.Client.AdvancedModOptions.Debugging.DRAW_LODS_DESC)
							.define("Enable Rendering", LodConfig.Client.AdvancedModOptions.Debugging.DRAW_LODS_DEFAULT);
					
					debugMode = builder
							.comment("\n\n"
									+ LodConfig.Client.AdvancedModOptions.Debugging.DEBUG_MODE_DESC)
							.defineEnum("Debug Mode", LodConfig.Client.AdvancedModOptions.Debugging.DEBUG_MODE_DEFAULT);
					
					enableDebugKeybindings = builder
							.comment("\n\n"
									+ LodConfig.Client.AdvancedModOptions.Debugging.ENABLE_DEBUG_KEYBINDINGS_DESC)
							.define("Enable Debug Keybinding", LodConfig.Client.AdvancedModOptions.Debugging.ENABLE_DEBUG_KEYBINDINGS_DEFAULT);
					
					builder.pop();
				}
			}
			
			
			public static class Buffers
			{
				public final ForgeConfigSpec.EnumValue<BufferRebuildTimes> rebuildTimes;
				
				Buffers(ForgeConfigSpec.Builder builder)
				{
					builder.comment(LodConfig.Client.AdvancedModOptions.Buffers.DESC).push(this.getClass().getSimpleName());
					
					rebuildTimes = builder
							.comment("\n\n"
									+ LodConfig.Client.AdvancedModOptions.Buffers.REBUILD_TIMES_DESC)
							.defineEnum("rebuildFrequency", LodConfig.Client.AdvancedModOptions.Buffers.REBUILD_TIMES_DEFAULT);
					
					builder.pop();
				}
			}
		}
	}
	
	
	/** {@link Path} to the configuration file of this mod */
	private static final Path CONFIG_PATH = Paths.get("config", ModInfo.NAME + ".toml");
	
	public static final ForgeConfigSpec CLIENT_SPEC;
	public static final Client CLIENT;
	
	static
	{
		final Pair<Client, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Client::new);
		CLIENT_SPEC = specPair.getRight();
		CLIENT = specPair.getLeft();
		CommentedFileConfig clientConfig = CommentedFileConfig.builder(CONFIG_PATH)
				.writingMode(WritingMode.REPLACE)
				.build();
		clientConfig.load();
		clientConfig.save();
		CLIENT_SPEC.setConfig(clientConfig);
	}
	
	@SubscribeEvent
	public static void onLoad(final ModConfig.Loading configEvent)
	{
		LogManager.getLogger().debug(ModInfo.NAME, "Loaded forge config file {}", configEvent.getConfig().getFileName());
	}
	
	@SubscribeEvent
	public static void onFileChange(final ModConfig.Reloading configEvent)
	{
		LogManager.getLogger().debug(ModInfo.NAME, "Forge config just got changed on the file system!");
	}
	
}
