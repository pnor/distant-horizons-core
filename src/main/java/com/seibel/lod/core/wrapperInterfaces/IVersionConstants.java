package com.seibel.lod.core.wrapperInterfaces;

import com.seibel.lod.core.enums.config.DistanceGenerationMode;
import com.seibel.lod.core.handlers.dependencyInjection.IBindable;

/**
 * A singleton that contains variables specific to each version of Minecraft
 * which can be used to change how DH-Core runs. For example: After MC 1.17
 * blocks can be negative, which changes how we generate LODs.
 * 
 * @author James Seibel
 * @version 3-5-2022
 */
public interface IVersionConstants extends IBindable {
	/** @returns the minimum height blocks can be generated */
	int getMinimumWorldHeight();

	/**
	 * @Returns True if the given DistanceGenerationMode can be run on our own
	 *          thread. <br>
	 *          False if the generation must be run on Minecraft's server thread.
	 */
	boolean isWorldGeneratorSingleThreaded(DistanceGenerationMode distanceGenerationMode);

	/**
	 * @Returns True if BatchGeneration is implemented <br>
	 *          False if it is not supported
	 */
	boolean hasBatchGenerationImplementation();

	/**
	 * @Returns the number of generations call per thread.
	 */
	default int getWorldGenerationCountPerThread() {
		return 8;
	}
	
	boolean isVanillaRenderedChunkSquare();

}
