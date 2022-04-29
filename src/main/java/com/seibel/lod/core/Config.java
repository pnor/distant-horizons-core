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

package com.seibel.lod.core;


import com.seibel.lod.core.config.*;
import com.seibel.lod.core.config.types.*;

import com.seibel.lod.core.enums.config.*;
import com.seibel.lod.core.enums.rendering.*;

import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton.IClient.IAdvanced.*;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton.IClient.IGraphics.*;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton.IClient.IGraphics.IFogQuality.IAdvancedFog;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton.IClient.IGraphics.IFogQuality.IAdvancedFog.IHeightFog;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton.IClient.IMultiplayer;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton.IClient.IWorldGenerator;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton.IClient.IAdvanced;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton.IClient.IAdvanced.IDebugging.*;


/**
 * This handles any configuration the user has access to.
 * @author coolGi2007
 * @version 04-29-2022
 */

public class Config
{
    // CONFIG STRUCTURE
    // 	-> Client
    //		|
    //		|-> Graphics
    //		|		|-> Quality
    //		|		|-> FogQuality
    //		|		|-> AdvancedGraphics
    //		|
    //		|-> World Generation
    //		|
    //		|-> Advanced
    //				|-> Threads
    //				|-> Buffers
    //				|-> Debugging

    // Since the original config system uses forge stuff, that means we have to rewrite the whole config system

    public static ConfigCategory client = new ConfigCategory.Builder().set(Client.class).build();


    public static class Client
    {
        public static ConfigCategory graphics = new ConfigCategory.Builder().set(Graphics.class).build();

        public static ConfigCategory worldGenerator = new ConfigCategory.Builder().set(WorldGenerator.class).build();

        public static ConfigCategory multiplayer = new ConfigCategory.Builder().set(Multiplayer.class).build();

        public static ConfigCategory advanced = new ConfigCategory.Builder().set(Advanced.class).build();

        public static ConfigEntry<Boolean> optionsButton = new ConfigEntry.Builder<Boolean>()
                .comment(ILodConfigWrapperSingleton.IClient.OPTIONS_BUTTON_DESC)
                .set(ILodConfigWrapperSingleton.IClient.OPTIONS_BUTTON_DEFAULT)
                .build();


        public static class Graphics
        {
            public static ConfigCategory quality = new ConfigCategory.Builder().set(Quality.class).build();

            public static ConfigCategory fogQuality = new ConfigCategory.Builder().set(FogQuality.class).build();

            public static ConfigCategory advancedGraphics = new ConfigCategory.Builder().set(AdvancedGraphics.class).build();


            public static class Quality
            {
                public static ConfigEntry<HorizontalResolution> drawResolution = new ConfigEntry.Builder<HorizontalResolution>()
                        .comment(IQuality.DRAW_RESOLUTION_DESC)
                        .set(IQuality.DRAW_RESOLUTION_DEFAULT)
                        .build();

                public static ConfigEntry<Integer> lodChunkRenderDistance = new ConfigEntry.Builder<Integer>()
                        .comment(IQuality.LOD_CHUNK_RENDER_DISTANCE_DESC)
                        .setMinDefaultMax(IQuality.LOD_CHUNK_RENDER_DISTANCE_MIN_DEFAULT_MAX)
                        .build();

                public static ConfigEntry<VerticalQuality> verticalQuality = new ConfigEntry.Builder<VerticalQuality>()
                        .comment(IQuality.VERTICAL_QUALITY_DESC)
                        .set(IQuality.VERTICAL_QUALITY_DEFAULT)
                        .build();

                public static ConfigEntry<Integer> horizontalScale = new ConfigEntry.Builder<Integer>()
                        .comment(IQuality.HORIZONTAL_SCALE_DESC)
                        .setMinDefaultMax(IQuality.HORIZONTAL_SCALE_MIN_DEFAULT_MAX)
                        .build();

                public static ConfigEntry<HorizontalQuality> horizontalQuality = new ConfigEntry.Builder<HorizontalQuality>()
                        .comment(IQuality.HORIZONTAL_SCALE_DESC)
                        .set(IQuality.HORIZONTAL_QUALITY_DEFAULT)
                        .build();

                public static ConfigEntry<DropoffQuality> dropoffQuality = new ConfigEntry.Builder<DropoffQuality>()
                        .comment(IQuality.DROPOFF_QUALITY_DESC)
                        .set(IQuality.DROPOFF_QUALITY_DEFAULT)
                        .build();

                public static ConfigEntry<Integer> lodBiomeBlending = new ConfigEntry.Builder<Integer>()
                        .comment(IQuality.LOD_BIOME_BLENDING_DESC)
                        .setMinDefaultMax(IQuality.LOD_BIOME_BLENDING_MIN_DEFAULT_MAX)
                        .build();
            }


            public static class FogQuality
            {
                public static ConfigEntry<FogDistance> fogDistance = new ConfigEntry.Builder<FogDistance>()
                        .comment(IFogQuality.FOG_DISTANCE_DESC)
                        .set(IFogQuality.FOG_DISTANCE_DEFAULT)
                        .build();

                public static ConfigEntry<FogDrawMode> fogDrawMode = new ConfigEntry.Builder<FogDrawMode>()
                        .comment(IFogQuality.FOG_DRAW_MODE_DESC)
                        .set(IFogQuality.FOG_DRAW_MODE_DEFAULT)
                        .build();

                public static ConfigEntry<FogColorMode> fogColorMode = new ConfigEntry.Builder<FogColorMode>()
                        .comment(IFogQuality.FOG_COLOR_MODE_DESC)
                        .set(IFogQuality.FOG_COLOR_MODE_DEFAULT)
                        .build();

                public static ConfigEntry<Boolean> disableVanillaFog = new ConfigEntry.Builder<Boolean>()
                        .comment(ILodConfigWrapperSingleton.IClient.IGraphics.IFogQuality.DISABLE_VANILLA_FOG_DESC)
                        .set(ILodConfigWrapperSingleton.IClient.IGraphics.IFogQuality.DISABLE_VANILLA_FOG_DEFAULT)
                        .build();

                public static ConfigCategory advancedFog = new ConfigCategory.Builder().set(AdvancedFog.class).build();


                public static class AdvancedFog {
                    public static ConfigEntry<Double> farFogStart = new ConfigEntry.Builder<Double>()
                            .comment(IAdvancedFog.FAR_FOG_START_DESC)
                            .setMinDefaultMax(IAdvancedFog.FAR_FOG_START_MIN_DEFAULT_MAX)
                            .build();

                    public static ConfigEntry<Double> farFogEnd = new ConfigEntry.Builder<Double>()
                            .comment(IAdvancedFog.FAR_FOG_END_DESC)
                            .setMinDefaultMax(IAdvancedFog.FAR_FOG_END_MIN_DEFAULT_MAX)
                            .build();

                    public static ConfigEntry<Double> farFogMin = new ConfigEntry.Builder<Double>()
                            .comment(IAdvancedFog.FAR_FOG_MIN_DESC)
                            .setMinDefaultMax(IAdvancedFog.FAR_FOG_MIN_MIN_DEFAULT_MAX)
                            .build();

                    public static ConfigEntry<Double> farFogMax = new ConfigEntry.Builder<Double>()
                            .comment(IAdvancedFog.FAR_FOG_MAX_DESC)
                            .setMinDefaultMax(IAdvancedFog.FAR_FOG_MAX_MIN_DEFAULT_MAX)
                            .build();

                    public static ConfigEntry<FogSetting.FogType> farFogType = new ConfigEntry.Builder<FogSetting.FogType>()
                            .comment(IAdvancedFog.FAR_FOG_TYPE_DESC)
                            .set(IAdvancedFog.FAR_FOG_TYPE_DEFAULT)
                            .build();

                    public static ConfigEntry<Double> farFogDensity = new ConfigEntry.Builder<Double>()
                            .comment(IAdvancedFog.FAR_FOG_DENSITY_DESC)
                            .setMinDefaultMax(IAdvancedFog.FAR_FOG_DENSITY_MIN_DEFAULT_MAX)
                            .build();

                    public static ConfigCategory heightFog = new ConfigCategory.Builder().set(HeightFog.class).build();


                    public static class HeightFog {
                        public static ConfigEntry<HeightFogMixMode> heightFogMixMode = new ConfigEntry.Builder<HeightFogMixMode>()
                                .comment(IHeightFog.HEIGHT_FOG_MIX_MODE_DESC)
                                .set(IHeightFog.HEIGHT_FOG_MIX_MODE_DEFAULT)
                                .build();

                        public static ConfigEntry<HeightFogMode> heightFogMode = new ConfigEntry.Builder<HeightFogMode>()
                                .comment(IHeightFog.HEIGHT_FOG_MODE_DESC)
                                .set(IHeightFog.HEIGHT_FOG_MODE_DEFAULT)
                                .build();

                        public static ConfigEntry<Double> heightFogHeight = new ConfigEntry.Builder<Double>()
                                .comment(IHeightFog.HEIGHT_FOG_HEIGHT_DESC)
                                .setMinDefaultMax(IHeightFog.HEIGHT_FOG_HEIGHT_MIN_DEFAULT_MAX)
                                .build();

                        public static ConfigEntry<Double> heightFogStart = new ConfigEntry.Builder<Double>()
                                .comment(IHeightFog.HEIGHT_FOG_START_DESC)
                                .setMinDefaultMax(IHeightFog.HEIGHT_FOG_START_MIN_DEFAULT_MAX)
                                .build();

                        public static ConfigEntry<Double> heightFogEnd = new ConfigEntry.Builder<Double>()
                                .comment(IHeightFog.HEIGHT_FOG_END_DESC)
                                .setMinDefaultMax(IHeightFog.HEIGHT_FOG_END_MIN_DEFAULT_MAX)
                                .build();

                        public static ConfigEntry<Double> heightFogMin = new ConfigEntry.Builder<Double>()
                                .comment(IHeightFog.HEIGHT_FOG_MIN_DESC)
                                .setMinDefaultMax(IHeightFog.HEIGHT_FOG_MIN_MIN_DEFAULT_MAX)
                                .build();

                        public static ConfigEntry<Double> heightFogMax = new ConfigEntry.Builder<Double>()
                                .comment(IHeightFog.HEIGHT_FOG_MAX_DESC)
                                .setMinDefaultMax(IHeightFog.HEIGHT_FOG_MAX_MIN_DEFAULT_MAX)
                                .build();

                        public static ConfigEntry<FogSetting.FogType> heightFogType = new ConfigEntry.Builder<FogSetting.FogType>()
                                .comment(IHeightFog.HEIGHT_FOG_TYPE_DESC)
                                .set(IHeightFog.HEIGHT_FOG_TYPE_DEFAULT)
                                .build();

                        public static ConfigEntry<Double> heightFogDensity = new ConfigEntry.Builder<Double>()
                                .comment(IHeightFog.HEIGHT_FOG_DENSITY_DESC)
                                .setMinDefaultMax(IHeightFog.HEIGHT_FOG_DENSITY_MIN_DEFAULT_MAX)
                                .build();

                    }
                }
            }


            public static class AdvancedGraphics
            {
                public static ConfigEntry<Boolean> disableDirectionalCulling = new ConfigEntry.Builder<Boolean>()
                        .comment(IAdvancedGraphics.DISABLE_DIRECTIONAL_CULLING_DESC)
                        .set(IAdvancedGraphics.DISABLE_DIRECTIONAL_CULLING_DEFAULT)
                        .build();

                public static ConfigEntry<VanillaOverdraw> vanillaOverdraw = new ConfigEntry.Builder<VanillaOverdraw>()
                        .comment(IAdvancedGraphics.VANILLA_OVERDRAW_DESC)
                        .set(IAdvancedGraphics.VANILLA_OVERDRAW_DEFAULT)
                        .build();

                public static ConfigEntry<Integer> overdrawOffset = new ConfigEntry.Builder<Integer>()
                        .comment(IAdvancedGraphics.OVERDRAW_OFFSET_DESC)
                        .setMinDefaultMax(IAdvancedGraphics.OVERDRAW_OFFSET_MIN_DEFAULT_MAX)
                        .build();

                public static ConfigEntry<Boolean> useExtendedNearClipPlane = new ConfigEntry.Builder<Boolean>()
                        .comment(IAdvancedGraphics.USE_EXTENDED_NEAR_CLIP_PLANE_DESC)
                        .set(IAdvancedGraphics.USE_EXTENDED_NEAR_CLIP_PLANE_DEFAULT)
                        .build();

                public static ConfigEntry<Double> brightnessMultiplier = new ConfigEntry.Builder<Double>()
                        .comment(IAdvancedGraphics.BRIGHTNESS_MULTIPLIER_DESC)
                        .set(IAdvancedGraphics.BRIGHTNESS_MULTIPLIER_DEFAULT)
                        .build();

                public static ConfigEntry<Double> saturationMultiplier = new ConfigEntry.Builder<Double>()
                        .comment(IAdvancedGraphics.SATURATION_MULTIPLIER_DESC)
                        .set(IAdvancedGraphics.SATURATION_MULTIPLIER_DEFAULT)
                        .build();

                public static ConfigEntry<Boolean> enableCaveCulling = new ConfigEntry.Builder<Boolean>()
                        .comment(IAdvancedGraphics.ENABLE_CAVE_CULLING_DESC)
                        .set(IAdvancedGraphics.ENABLE_CAVE_CULLING_DEFAULT)
                        .build();

                public static ConfigEntry<Integer> caveCullingHeight = new ConfigEntry.Builder<Integer>()
                        .comment(IAdvancedGraphics.CAVE_CULLING_HEIGHT_DESC)
                        .setMinDefaultMax(IAdvancedGraphics.CAVE_CULLING_HEIGHT_MIN_DEFAULT_MAX)
                        .build();

                public static ConfigEntry<Integer> earthCurveRatio = new ConfigEntry.Builder<Integer>()
                        .comment(IAdvancedGraphics.EARTH_CURVE_RATIO_DESC)
                        .setMinDefaultMax(IAdvancedGraphics.EARTH_CURVE_RATIO_MIN_DEFAULT_MAX)
                        .build();

				/*
				@ConfigAnnotations.FileComment
				public static String _backsideCullingRange = IAdvancedGraphics.VANILLA_CULLING_RANGE_DESC;
				@ConfigAnnotations.Entry(minValue = 0, maxValue = 512)
				public static int backsideCullingRange = IAdvancedGraphics.VANILLA_CULLING_RANGE_MIN_DEFAULT_MAX.defaultValue;
				*/
            }
        }


        public static class WorldGenerator
        {
            public static ConfigEntry<Boolean> enableDistantGeneration = new ConfigEntry.Builder<Boolean>()
                    .comment(IWorldGenerator.ENABLE_DISTANT_GENERATION_DESC)
                    .set(IWorldGenerator.ENABLE_DISTANT_GENERATION_DEFAULT)
                    .build();

            public static ConfigEntry<DistanceGenerationMode> distanceGenerationMode = new ConfigEntry.Builder<DistanceGenerationMode>()
//                    .comment(IWorldGenerator.getDistanceGenerationModeDesc())
                    .set(IWorldGenerator.DISTANCE_GENERATION_MODE_DEFAULT)
                    .build();

            public static ConfigEntry<LightGenerationMode> lightGenerationMode = new ConfigEntry.Builder<LightGenerationMode>()
                    .comment(IWorldGenerator.LIGHT_GENERATION_MODE_DESC)
                    .set(IWorldGenerator.LIGHT_GENERATION_MODE_DEFAULT)
                    .build();

            public static ConfigEntry<GenerationPriority> generationPriority = new ConfigEntry.Builder<GenerationPriority>()
                    .comment(IWorldGenerator.GENERATION_PRIORITY_DESC)
                    .set(IWorldGenerator.GENERATION_PRIORITY_DEFAULT)
                    .build();

            public static ConfigEntry<BlocksToAvoid> blocksToAvoid = new ConfigEntry.Builder<BlocksToAvoid>()
                    .comment(IWorldGenerator.BLOCKS_TO_AVOID_DESC)
                    .set(IWorldGenerator.BLOCKS_TO_AVOID_DEFAULT)
                    .build();
        }


        public static class Multiplayer
        {
            public static ConfigEntry<ServerFolderNameMode> serverFolderNameMode = new ConfigEntry.Builder<ServerFolderNameMode>()
                    .comment(IMultiplayer.SERVER_FOLDER_NAME_MODE_DESC)
                    .set(IMultiplayer.SERVER_FOLDER_NAME_MODE_DEFAULT)
                    .build();

            public static ConfigEntry<Double> multiDimensionRequiredSimilarity = new ConfigEntry.Builder<Double>()
                    .comment(IMultiplayer.MULTI_DIMENSION_REQUIRED_SIMILARITY_DESC)
                    .setMinDefaultMax(IMultiplayer.MULTI_DIMENSION_REQUIRED_SIMILARITY_MIN_DEFAULT_MAX)
                    .build();

        }


        public static class Advanced
        {
            public static ConfigCategory threading = new ConfigCategory.Builder().set(Threading.class).build();

            public static ConfigCategory debugging = new ConfigCategory.Builder().set(Debugging.class).build();

            public static ConfigCategory buffers = new ConfigCategory.Builder().set(Buffers.class).build();

            public static ConfigEntry<Boolean> lodOnlyMode = new ConfigEntry.Builder<Boolean>()
                    .comment(IAdvanced.LOD_ONLY_MODE_DESC)
                    .set(IAdvanced.LOD_ONLY_MODE_DEFAULT)
                    .build();


            public static class Threading
            {
                public static ConfigEntry<Double> numberOfWorldGenerationThreads = new ConfigEntry.Builder<Double>()
                        .comment(IThreading.NUMBER_OF_WORLD_GENERATION_THREADS_DESC)
                        .setMinDefaultMax(IThreading.NUMBER_OF_WORLD_GENERATION_THREADS_DEFAULT)
                        .build();

                public static ConfigEntry<Integer> numberOfBufferBuilderThreads = new ConfigEntry.Builder<Integer>()
                        .comment(IThreading.NUMBER_OF_BUFFER_BUILDER_THREADS_DESC)
                        .setMinDefaultMax(IThreading.NUMBER_OF_BUFFER_BUILDER_THREADS_MIN_DEFAULT_MAX)
                        .build();
            }


            public static class Debugging
            {
                public static ConfigEntry<RendererType> rendererType = new ConfigEntry.Builder<RendererType>()
                        .comment(IDebugging.RENDERER_TYPE_DESC)
                        .set(IDebugging.RENDERER_TYPE_DEFAULT)
                        .build();

                public static ConfigEntry<DebugMode> debugMode = new ConfigEntry.Builder<DebugMode>()
                        .comment(IDebugging.DEBUG_MODE_DESC)
                        .set(IDebugging.DEBUG_MODE_DEFAULT)
                        .build();

                public static ConfigEntry<Boolean> enableDebugKeybindings = new ConfigEntry.Builder<Boolean>()
                        .comment(IDebugging.DEBUG_KEYBINDINGS_ENABLED_DESC)
                        .set(IDebugging.DEBUG_KEYBINDINGS_ENABLED_DEFAULT)
                        .build();

                public static ConfigCategory debugSwitch = new ConfigCategory.Builder().set(DebugSwitch.class).build();


                public static class DebugSwitch {
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
                    public static ConfigEntry<LoggerMode> logWorldGenEvent = new ConfigEntry.Builder<LoggerMode>()
                            .comment(IDebugSwitch.LOG_WORLDGEN_EVENT_DESC)
                            .set(IDebugSwitch.LOG_WORLDGEN_EVENT_DEFAULT)
                            .build();

                    public static ConfigEntry<LoggerMode> logWorldGenPerformance = new ConfigEntry.Builder<LoggerMode>()
                            .comment(IDebugSwitch.LOG_WORLDGEN_PERFORMANCE_DESC)
                            .set(IDebugSwitch.LOG_WORLDGEN_PERFORMANCE_DEFAULT)
                            .build();

                    public static ConfigEntry<LoggerMode> logWorldGenLoadEvent = new ConfigEntry.Builder<LoggerMode>()
                            .comment(IDebugSwitch.LOG_WORLDGEN_LOAD_EVENT_DESC)
                            .set(IDebugSwitch.LOG_WORLDGEN_LOAD_EVENT_DEFAULT)
                            .build();

                    public static ConfigEntry<LoggerMode> logLodBuilderEvent = new ConfigEntry.Builder<LoggerMode>()
                            .comment(IDebugSwitch.LOG_LODBUILDER_EVENT_DESC)
                            .set(IDebugSwitch.LOG_LODBUILDER_EVENT_DEFAULT)
                            .build();

                    public static ConfigEntry<LoggerMode> logRendererBufferEvent = new ConfigEntry.Builder<LoggerMode>()
                            .comment(IDebugSwitch.LOG_RENDERER_BUFFER_EVENT_DESC)
                            .set(IDebugSwitch.LOG_RENDERER_BUFFER_EVENT_DEFAULT)
                            .build();

                    public static ConfigEntry<LoggerMode> logRendererGLEvent = new ConfigEntry.Builder<LoggerMode>()
                            .comment(IDebugSwitch.LOG_RENDERER_GL_EVENT_DESC)
                            .set(IDebugSwitch.LOG_RENDERER_GL_EVENT_DEFAULT)
                            .build();

                    public static ConfigEntry<LoggerMode> logFileReadWriteEvent = new ConfigEntry.Builder<LoggerMode>()
                            .comment(IDebugSwitch.LOG_FILE_READWRITE_EVENT_DESC)
                            .set(IDebugSwitch.LOG_FILE_READWRITE_EVENT_DEFAULT)
                            .build();

                    public static ConfigEntry<LoggerMode> logFileSubDimEvent = new ConfigEntry.Builder<LoggerMode>()
                            .comment(IDebugSwitch.LOG_FILE_SUB_DIM_EVENT_DESC)
                            .set(IDebugSwitch.LOG_FILE_SUB_DIM_EVENT_DEFAULT)
                            .build();

                    public static ConfigEntry<LoggerMode> logNetworkEvent = new ConfigEntry.Builder<LoggerMode>()
                            .comment(IDebugSwitch.LOG_NETWORK_EVENT_DESC)
                            .set(IDebugSwitch.LOG_NETWORK_EVENT_DEFAULT)
                            .build();
                }
            }


            public static class Buffers
            {
                public static ConfigEntry<GpuUploadMethod> gpuUploadMethod = new ConfigEntry.Builder<GpuUploadMethod>()
                        .comment(IBuffers.GPU_UPLOAD_METHOD_DESC)
                        .set(IBuffers.GPU_UPLOAD_METHOD_DEFAULT)
                        .build();

                public static ConfigEntry<Integer> gpuUploadPerMegabyteInMilliseconds = new ConfigEntry.Builder<Integer>()
                        .comment(IBuffers.GPU_UPLOAD_PER_MEGABYTE_IN_MILLISECONDS_DESC)
                        .setMinDefaultMax(IBuffers.GPU_UPLOAD_PER_MEGABYTE_IN_MILLISECONDS_DEFAULT)
                        .build();

                public static ConfigEntry<BufferRebuildTimes> rebuildTimes = new ConfigEntry.Builder<BufferRebuildTimes>()
                        .comment(IBuffers.REBUILD_TIMES_DESC)
                        .set(IBuffers.REBUILD_TIMES_DEFAULT)
                        .build();
            }
        }
    }
}
