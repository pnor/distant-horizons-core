#version 150 core

in vec4 vPosition;
in vec4 color;

out vec4 vertexColor;
out vec3 vertexWorldPos;

uniform mat4 modelViewMatrix;
uniform mat4 projectionMatrix;

uniform int worldSkyLight;
uniform sampler2D lightMap;


/** 
 * Vertex Shader
 * 
 * author: James Seibel
 * version: 12-8-2021
 *
 * updated: TomTheFurry
 * version: 15-2-2022
 */
void main()
{
	vec4 worldSpacePos = modelViewMatrix * vec4(vPosition.xyz,1);
	float light = (vPosition.a+0.5) / 256.0;

	vertexColor = color * texture(lightMap, vec2(light,0.5));
	
	vertexWorldPos = worldSpacePos.xyz;
	
    gl_Position = projectionMatrix * worldSpacePos;
}
