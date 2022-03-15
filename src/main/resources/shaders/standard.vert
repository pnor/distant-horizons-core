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

	// vec4 pos = projectionMatrix * worldSpacePos;
    gl_Position = projectionMatrix * worldSpacePos;
    /*pos.a = 1.0;
    if (pos.x>0) pos.x=-1; else pos.x=1;
    if (pos.y>0) pos.y=-1; else pos.y=1;
    pos.z = 0.5;
    gl_Position = pos;*/
}
