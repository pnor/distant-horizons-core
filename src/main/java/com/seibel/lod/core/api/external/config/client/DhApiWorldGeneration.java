package com.seibel.lod.core.api.external.config.client;

import com.seibel.lod.core.api.external.apiObjects.enums.*;
import com.seibel.lod.core.api.external.apiObjects.wrapperInterfaces.IDhApiConfig;
import com.seibel.lod.core.api.implementation.objects.GenericEnumConverter;
import com.seibel.lod.core.api.implementation.wrappers.DhApiConfig;
import com.seibel.lod.core.config.Config.Client.Advanced.Debugging;
import com.seibel.lod.core.config.Config.Client.WorldGenerator;
import com.seibel.lod.core.enums.config.EBlocksToAvoid;
import com.seibel.lod.core.enums.config.EDistanceGenerationMode;
import com.seibel.lod.core.enums.config.EGenerationPriority;
import com.seibel.lod.core.enums.config.ELightGenerationMode;
import com.seibel.lod.core.enums.rendering.ERendererType;

/**
 * General graphics settings.
 *
 * @author James Seibel
 * @version 2022-6-13
 */
public class DhApiWorldGeneration
{
	
	/**
	 * Returns the config related to whether Distant Horizons
	 * will attempt to generate fake chunks outside Minecraft's
	 * vanilla render distance.
	 */
	public static IDhApiConfig<Boolean> getEnableDistantWorldGenerationConfig()
	{ return new DhApiConfig<>(WorldGenerator.enableDistantGeneration); }
	
	/**
	 * Returns the config related to how Distant Horizons' distant world
	 * generator will generate chunks.
	 */
	public static IDhApiConfig<EDhApiDistanceGenerationMode> getDistantGeneratorModeConfig()
	{ return new DhApiConfig<>(WorldGenerator.distanceGenerationMode, new GenericEnumConverter<>(EDistanceGenerationMode.class, EDhApiDistanceGenerationMode.class)); }
	
	/**
	 * Returns the config related to how Distant Horizons' distant world
	 * generator will light the chunks it generates.
	 */
	public static IDhApiConfig<EDhApiLightGenerationMode> getLightingModeConfig()
	{ return new DhApiConfig<>(WorldGenerator.lightGenerationMode, new GenericEnumConverter<>(ELightGenerationMode.class, EDhApiLightGenerationMode.class)); }
	
	/**
	 * Returns the config related to the order Distant Horizons' distant world
	 * generator will generate chunks.
	 */
	public static IDhApiConfig<EDhApiLightGenerationMode> getGenerationPriorityConfig()
	{ return new DhApiConfig<>(WorldGenerator.generationPriority, new GenericEnumConverter<>(EGenerationPriority.class, EDhApiLightGenerationMode.class)); }
	
	/**
	 * Returns the config related to what blocks Distant Horizons' distant world
	 * generator will ignore when generating LODs.
	 *
	 * @deprecated this method won't be needed once we transition to an ID based save system <br>
	 * 				(vs the color based system we have currently)
	 */
	@Deprecated
	public static IDhApiConfig<EDhApiBlocksToAvoid> getBlocksToAvoidConfig()
	{ return new DhApiConfig<>(WorldGenerator.blocksToAvoid, new GenericEnumConverter<>(EBlocksToAvoid.class, EDhApiBlocksToAvoid.class)); }
	
	/**
	 * Returns the config related to whether Distant Horizons' distant world
	 * generator will color the blocks below an avoided block. <Br>
	 * (IE: if flowers are avoided should they color the grass below them?)
	 *
	 * @deprecated this method won't be needed once we transition to an ID based save system <br>
	 * 				(vs the color based system we have currently)
	 */
	@Deprecated
	public static IDhApiConfig<Boolean> getTintWithAvoidedBlocksConfig()
	{ return new DhApiConfig<>(WorldGenerator.tintWithAvoidedBlocks); }
	
	
}
