#version 150 core

in vec4 vertexColor;
in vec4 vertexWorldPos;
//in vec2 textureCoord;


out vec4 fragColor;


//uniform sampler2D texImage;
uniform vec3 cameraPos;

uniform bool fogEnabled;
uniform float farPlane;
uniform float nearPlane;
uniform vec4 fogColor;


// method definitions
float getFogAlpha(float start, float end, float dist);



/** 
 * Fragment Shader
 * 
 * author: James Seibel
 * version: 11-26-2021
 */
void main()
{
	// TODO: add a white texture to support Optifine shaders
    //vec4 textureColor = texture(texImage, textureCoord);
    //fragColor = vertexColor * textureColor;
    
	
	vec4 returnColor;
	if (fogEnabled)
	{
		// add fog
		float dist = distance(vertexWorldPos, vec4(cameraPos,1));
		float fogAlpha = getFogAlpha(nearPlane, farPlane, dist);
		returnColor = mix(vec4(fogColor.xyz, 1), vertexColor, fogAlpha);
	}
	else
	{
		// simple flat color
		returnColor = vertexColor;
	}
	
	
	
	fragColor = returnColor;
}




/** 
 * Returns the fog strength for the given fragment.
 * This is the same implementation as legacy OpenGL's Linear fog option.
 */
float getFogAlpha(float start, float end, float dist)
{
	float fogAlpha = (end - dist) / (end - start);
    return clamp(fogAlpha, 0, 1);
}

