#version 150 core

in vec3 vPosition;
in vec4 color;
in float blockSkyLight;
in float blockLight;

out vec4 vertexColor;
out vec4 vertexWorldPos;
out float depth;


uniform mat4 modelViewMatrix;
uniform mat4 projectionMatrix;

uniform int worldSkyLight;
uniform sampler2D lightMap;


/** 
 * Vertex Shader
 * 
 * author: James Seibel
 * version: 12-8-2021
 */
void main()
{

	// just skylight
	// good for sanity checks; but will cause OpenGL errors since we are binding unused data
//	vertexColor = vec4(color.xyz * worldSkyLight / 16.0, color.w);
	
	float blockLightTex = blockLight / 16.0;
	float skyLightTex = worldSkyLight / 16.0;
	
	// we don't really need alpha in the lightmap
//	vertexColor = color * vec4(texture(lightMap, vec2(blockLightTex, skyLightTex)).xyz, 1);
	vertexColor = color * texture(lightMap, vec2(blockLightTex, skyLightTex));
	
	
	
	// TODO: add a simple white texture to support Optifine shaders
	//textureCoord = textureCoord;
	
	vertexWorldPos = vec4(vPosition, 1);
	
	// the vPosition needs to be converted to a vec4 so it can be multiplied
	// by the 4x4 matrices
    gl_Position = projectionMatrix * modelViewMatrix * vec4(vPosition, 1);
}
