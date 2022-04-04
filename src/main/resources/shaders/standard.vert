#version 150 core

in vec4 vPosition;
in vec4 color;

out vec4 vertexColor;
out vec3 vertexWorldPos;
out float vertexYPos;

uniform mat4 combinedMatrix;
uniform vec3 modelOffset;
uniform float worldYOffset;

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
    vertexWorldPos = vPosition.xyz + modelOffset;
    vertexYPos = vPosition.y + worldYOffset;

	float light2 = (mod(vPosition.a, 16)+0.5) / 16.0;
	float light = (floor(vPosition.a/16)+0.5) / 16.0;
	vertexColor = color * vec4(texture(lightMap, vec2(light, light2)).xyz, 1.0);

    gl_Position = combinedMatrix * vec4(vertexWorldPos, 1.0);
}
