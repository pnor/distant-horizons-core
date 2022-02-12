package com.seibel.lod.core;

import com.seibel.lod.core.config.*;
import com.seibel.lod.core.enums.config.*;
import com.seibel.lod.core.enums.rendering.*;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton;

/**
 * This handles any configuration the user has access to.
 * @author coolGi2007
 * @version 02-07-2022
 */
public class Config extends ConfigBase
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

    @ConfigAnnotations.Category
    public static Client client;

    public static class Client
    {
        @ConfigAnnotations.Category
        public static Graphics graphics;

        @ConfigAnnotations.Category
        public static WorldGenerator worldGenerator;

        @ConfigAnnotations.Category
        public static Advanced advanced;


        @ConfigAnnotations.Entry
        public static ConfigEntry optionsButton = new ConfigEntry.Builder<Boolean>()
                .comment(ILodConfigWrapperSingleton.IClient.OPTIONS_BUTTON_DESC)
                .set(ILodConfigWrapperSingleton.IClient.OPTIONS_BUTTON_DEFAULT)
                .build();


        public static class Graphics
        {
            @ConfigAnnotations.Category
            public static Quality quality;

            @ConfigAnnotations.Category
            public static FogQuality fogQuality;

            @ConfigAnnotations.Category
            public static AdvancedGraphics advancedGraphics;


            public static class Quality
            {
                @ConfigAnnotations.Entry
                public static ConfigEntry drawResolution =  new ConfigEntry.Builder<HorizontalResolution>()
                        .comment(ILodConfigWrapperSingleton.IClient.IGraphics.IQuality.DRAW_RESOLUTION_DESC)
                        .set(ILodConfigWrapperSingleton.IClient.IGraphics.IQuality.DRAW_RESOLUTION_DEFAULT)
                        .build();

                @ConfigAnnotations.Entry
                public static ConfigEntry lodChunkRenderDistance = new ConfigEntry.Builder<Integer>()
                        .comment(ILodConfigWrapperSingleton.IClient.IGraphics.IQuality.LOD_CHUNK_RENDER_DISTANCE_DESC)
                        .set(ILodConfigWrapperSingleton.IClient.IGraphics.IQuality.LOD_CHUNK_RENDER_DISTANCE_MIN_DEFAULT_MAX.defaultValue)
                        .setMinMax(ILodConfigWrapperSingleton.IClient.IGraphics.IQuality.LOD_CHUNK_RENDER_DISTANCE_MIN_DEFAULT_MAX.minValue, ILodConfigWrapperSingleton.IClient.IGraphics.IQuality.LOD_CHUNK_RENDER_DISTANCE_MIN_DEFAULT_MAX.maxValue)
                        .build();

                @ConfigAnnotations.Entry
                public static ConfigEntry verticalQuality = new ConfigEntry.Builder<VerticalQuality>()
                        .comment(ILodConfigWrapperSingleton.IClient.IGraphics.IQuality.VERTICAL_QUALITY_DESC)
                        .set(ILodConfigWrapperSingleton.IClient.IGraphics.IQuality.VERTICAL_QUALITY_DEFAULT)
                        .build();

                @ConfigAnnotations.Entry
                public static ConfigEntry horizontalScale = new ConfigEntry.Builder<Integer>()
                        .comment(ILodConfigWrapperSingleton.IClient.IGraphics.IQuality.HORIZONTAL_SCALE_DESC)
                        .set(ILodConfigWrapperSingleton.IClient.IGraphics.IQuality.HORIZONTAL_SCALE_MIN_DEFAULT_MAX.defaultValue)
                        .setMinMax(ILodConfigWrapperSingleton.IClient.IGraphics.IQuality.HORIZONTAL_SCALE_MIN_DEFAULT_MAX.minValue, ILodConfigWrapperSingleton.IClient.IGraphics.IQuality.HORIZONTAL_SCALE_MIN_DEFAULT_MAX.maxValue)
                        .build();

                @ConfigAnnotations.Entry
                public static ConfigEntry horizontalQuality = new ConfigEntry.Builder<HorizontalQuality>()
                        .comment(ILodConfigWrapperSingleton.IClient.IGraphics.IQuality.HORIZONTAL_SCALE_DESC)
                        .set(ILodConfigWrapperSingleton.IClient.IGraphics.IQuality.HORIZONTAL_QUALITY_DEFAULT)
                        .build();

                @ConfigAnnotations.Entry
                public static ConfigEntry dropoffQuality = new ConfigEntry.Builder<DropoffQuality>()
                        .comment(ILodConfigWrapperSingleton.IClient.IGraphics.IQuality.DROPOFF_QUALITY_DESC)
                        .set(ILodConfigWrapperSingleton.IClient.IGraphics.IQuality.DROPOFF_QUALITY_DEFAULT)
                        .build();
            }


            public static class FogQuality
            {
                @ConfigAnnotations.Entry
                public static ConfigEntry fogDistance = new ConfigEntry.Builder<FogDistance>()
                        .comment(ILodConfigWrapperSingleton.IClient.IGraphics.IFogQuality.FOG_DISTANCE_DESC)
                        .set(ILodConfigWrapperSingleton.IClient.IGraphics.IFogQuality.FOG_DISTANCE_DEFAULT)
                        .build();

                @ConfigAnnotations.Entry
                public static ConfigEntry fogDrawMode = new ConfigEntry.Builder<FogDrawMode>()
                        .comment(ILodConfigWrapperSingleton.IClient.IGraphics.IFogQuality.FOG_DRAW_MODE_DESC)
                        .set(ILodConfigWrapperSingleton.IClient.IGraphics.IFogQuality.FOG_DRAW_MODE_DEFAULT)
                        .build();

                @ConfigAnnotations.Entry
                public static ConfigEntry fogColorMode = new ConfigEntry.Builder<FogColorMode>()
                        .comment(ILodConfigWrapperSingleton.IClient.IGraphics.IFogQuality.FOG_COLOR_MODE_DESC)
                        .set(ILodConfigWrapperSingleton.IClient.IGraphics.IFogQuality.FOG_COLOR_MODE_DEFAULT)
                        .build();

                @ConfigAnnotations.Entry
                public static ConfigEntry disableVanillaFog = new ConfigEntry.Builder<Boolean>()
                        .comment(ILodConfigWrapperSingleton.IClient.IGraphics.IFogQuality.DISABLE_VANILLA_FOG_DESC)
                        .set(ILodConfigWrapperSingleton.IClient.IGraphics.IFogQuality.DISABLE_VANILLA_FOG_DEFAULT)
                        .build();
            }


            public static class AdvancedGraphics
            {
                @ConfigAnnotations.Entry
                public static ConfigEntry disableDirectionalCulling = new ConfigEntry.Builder<Boolean>()
                        .comment(ILodConfigWrapperSingleton.IClient.IGraphics.IAdvancedGraphics.DISABLE_DIRECTIONAL_CULLING_DESC)
                        .set(ILodConfigWrapperSingleton.IClient.IGraphics.IAdvancedGraphics.DISABLE_DIRECTIONAL_CULLING_DEFAULT)
                        .build();

                @ConfigAnnotations.Entry
                public static ConfigEntry vanillaOverdraw = new ConfigEntry.Builder<VanillaOverdraw>()
                        .comment(ILodConfigWrapperSingleton.IClient.IGraphics.IAdvancedGraphics.VANILLA_OVERDRAW_DESC)
                        .set(ILodConfigWrapperSingleton.IClient.IGraphics.IAdvancedGraphics.VANILLA_OVERDRAW_DEFAULT)
                        .build();

                @ConfigAnnotations.Entry
                public static ConfigEntry useExtendedNearClipPlane = new ConfigEntry.Builder<Boolean>()
                        .comment(ILodConfigWrapperSingleton.IClient.IGraphics.IAdvancedGraphics.USE_EXTENDED_NEAR_CLIP_PLANE_DESC)
                        .set(ILodConfigWrapperSingleton.IClient.IGraphics.IAdvancedGraphics.USE_EXTENDED_NEAR_CLIP_PLANE_DEFAULT)
                        .build();

				/*
				@ConfigAnnotations.Entry(minValue = 0, maxValue = 512)
				public static int backsideCullingRange = IAdvancedGraphics.VANILLA_CULLING_RANGE_MIN_DEFAULT_MAX.defaultValue;
				*/
            }
        }


        public static class WorldGenerator
        {
            @ConfigAnnotations.Entry
            public static ConfigEntry enableDistantGeneration = new ConfigEntry.Builder<Boolean>()
                    .comment(ILodConfigWrapperSingleton.IClient.IWorldGenerator.ENABLE_DISTANT_GENERATION_DESC)
                    .set(ILodConfigWrapperSingleton.IClient.IWorldGenerator.ENABLE_DISTANT_GENERATION_DEFAULT)
                    .build();

            @ConfigAnnotations.Entry
            public static ConfigEntry distanceGenerationMode = new ConfigEntry.Builder<DistanceGenerationMode>()
                    //.comment(ILodConfigWrapperSingleton.IClient.IWorldGenerator.getDistanceGenerationModeDesc())
                    .set(ILodConfigWrapperSingleton.IClient.IWorldGenerator.DISTANCE_GENERATION_MODE_DEFAULT)
                    .build();

            @ConfigAnnotations.Entry
            public static ConfigEntry lightGenerationMode = new ConfigEntry.Builder<LightGenerationMode>()
                    .comment(ILodConfigWrapperSingleton.IClient.IWorldGenerator.LIGHT_GENERATION_MODE_DESC)
                    .set(ILodConfigWrapperSingleton.IClient.IWorldGenerator.LIGHT_GENERATION_MODE_DEFAULT)
                    .build();

            @ConfigAnnotations.Entry
            public static ConfigEntry generationPriority = new ConfigEntry.Builder<GenerationPriority>()
                    .comment(ILodConfigWrapperSingleton.IClient.IWorldGenerator.GENERATION_PRIORITY_DESC)
                    .set(ILodConfigWrapperSingleton.IClient.IWorldGenerator.GENERATION_PRIORITY_DEFAULT)
                    .build();

            @ConfigAnnotations.Entry
            public static ConfigEntry blocksToAvoid = new ConfigEntry.Builder<BlocksToAvoid>()
                    .comment(ILodConfigWrapperSingleton.IClient.IWorldGenerator.BLOCKS_TO_AVOID_DESC)
                    .set(ILodConfigWrapperSingleton.IClient.IWorldGenerator.BLOCKS_TO_AVOID_DEFAULT)
                    .build();
        }

        public static class Advanced
        {
            @ConfigAnnotations.Category
            public static Threading threading;

            @ConfigAnnotations.Category
            public static Debugging debugging;

            @ConfigAnnotations.Category
            public static Buffers buffers;


            public static class Threading
            {
                @ConfigAnnotations.Entry
                public static ConfigEntry numberOfWorldGenerationThreads = new ConfigEntry.Builder<Integer>()
                        .comment(ILodConfigWrapperSingleton.IClient.IAdvanced.IThreading.NUMBER_OF_WORLD_GENERATION_THREADS_DESC)
                        .set(ILodConfigWrapperSingleton.IClient.IAdvanced.IThreading.NUMBER_OF_WORLD_GENERATION_THREADS_DEFAULT.defaultValue)
                        .setMinMax(ILodConfigWrapperSingleton.IClient.IAdvanced.IThreading.NUMBER_OF_WORLD_GENERATION_THREADS_DEFAULT.minValue, ILodConfigWrapperSingleton.IClient.IAdvanced.IThreading.NUMBER_OF_WORLD_GENERATION_THREADS_DEFAULT.maxValue)
                        .build();

                @ConfigAnnotations.Entry
                public static ConfigEntry numberOfBufferBuilderThreads = new ConfigEntry.Builder<Integer>()
                        .comment(ILodConfigWrapperSingleton.IClient.IAdvanced.IThreading.NUMBER_OF_BUFFER_BUILDER_THREADS_DESC)
                        .set(ILodConfigWrapperSingleton.IClient.IAdvanced.IThreading.NUMBER_OF_BUFFER_BUILDER_THREADS_MIN_DEFAULT_MAX.defaultValue)
                        .setMinMax(ILodConfigWrapperSingleton.IClient.IAdvanced.IThreading.NUMBER_OF_BUFFER_BUILDER_THREADS_MIN_DEFAULT_MAX.minValue, ILodConfigWrapperSingleton.IClient.IAdvanced.IThreading.NUMBER_OF_BUFFER_BUILDER_THREADS_MIN_DEFAULT_MAX.maxValue)
                        .build();
            }


            public static class Debugging
            {
                @ConfigAnnotations.Entry
                public static ConfigEntry drawLods = new ConfigEntry.Builder<Boolean>()
                        .comment(ILodConfigWrapperSingleton.IClient.IAdvanced.IDebugging.DRAW_LODS_DESC)
                        .set(ILodConfigWrapperSingleton.IClient.IAdvanced.IDebugging.DRAW_LODS_DEFAULT)
                        .build();

                @ConfigAnnotations.Entry
                public static ConfigEntry debugMode = new ConfigEntry.Builder<DebugMode>()
                        .comment(ILodConfigWrapperSingleton.IClient.IAdvanced.IDebugging.DEBUG_MODE_DESC)
                        .set(ILodConfigWrapperSingleton.IClient.IAdvanced.IDebugging.DEBUG_MODE_DEFAULT)
                        .build();

                @ConfigAnnotations.Entry
                public static ConfigEntry enableDebugKeybindings = new ConfigEntry.Builder<Boolean>()
                        .comment(ILodConfigWrapperSingleton.IClient.IAdvanced.IDebugging.DEBUG_KEYBINDINGS_ENABLED_DESC)
                        .set(ILodConfigWrapperSingleton.IClient.IAdvanced.IDebugging.DEBUG_KEYBINDINGS_ENABLED_DEFAULT)
                        .build();
            }


            public static class Buffers
            {
                @ConfigAnnotations.Entry
                public static ConfigEntry gpuUploadMethod = new ConfigEntry.Builder<GpuUploadMethod>()
                        .comment(ILodConfigWrapperSingleton.IClient.IAdvanced.IBuffers.GPU_UPLOAD_METHOD_DESC)
                        .set(ILodConfigWrapperSingleton.IClient.IAdvanced.IBuffers.GPU_UPLOAD_METHOD_DEFAULT)
                        .build();

                @ConfigAnnotations.Entry
                public static ConfigEntry gpuUploadPerMegabyteInMilliseconds = new ConfigEntry.Builder<Integer>()
                        .comment(ILodConfigWrapperSingleton.IClient.IAdvanced.IBuffers.GPU_UPLOAD_PER_MEGABYTE_IN_MILLISECONDS_DESC)
                        .set(ILodConfigWrapperSingleton.IClient.IAdvanced.IBuffers.GPU_UPLOAD_PER_MEGABYTE_IN_MILLISECONDS_DEFAULT.defaultValue)
                        .setMinMax(ILodConfigWrapperSingleton.IClient.IAdvanced.IBuffers.GPU_UPLOAD_PER_MEGABYTE_IN_MILLISECONDS_DEFAULT.minValue, ILodConfigWrapperSingleton.IClient.IAdvanced.IBuffers.GPU_UPLOAD_PER_MEGABYTE_IN_MILLISECONDS_DEFAULT.maxValue)
                        .build();

                @ConfigAnnotations.Entry
                public static ConfigEntry rebuildTimes = new ConfigEntry.Builder<BufferRebuildTimes>()
                        .comment(ILodConfigWrapperSingleton.IClient.IAdvanced.IBuffers.REBUILD_TIMES_DESC)
                        .set(ILodConfigWrapperSingleton.IClient.IAdvanced.IBuffers.REBUILD_TIMES_DEFAULT)
                        .build();
            }
        }
    }
}
