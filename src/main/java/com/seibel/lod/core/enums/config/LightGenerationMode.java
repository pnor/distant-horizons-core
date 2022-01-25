package com.seibel.lod.core.enums.config;

public enum LightGenerationMode
{
	
	// Fake in light values based on height maps
	FAST,
	
	// Run the light engine though the chunk to generate proper light values
	FANCY
}
