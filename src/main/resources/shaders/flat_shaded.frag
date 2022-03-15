#version 150 core

in vec4 vertexColor;
in vec3 vertexWorldPos;

out vec4 fragColor;

uniform bool fogEnabled;
uniform bool nearFogEnabled;
uniform bool farFogEnabled;

uniform float nearFogStart;
uniform float nearFogEnd;
uniform float farFogStart;
uniform float farFogEnd;
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
		
		// no fog by default
		float fogAlpha = 0;
		
		float dist = length(vertexWorldPos);

		// less than because nearFogStart is farther away than nearFogEnd
		if (nearFogEnabled && dist < nearFogStart)
		{
			fogAlpha = getFogAlpha(nearFogStart, nearFogEnd, dist);
		}
		else if (farFogEnabled)
		{
			fogAlpha = getFogAlpha(farFogStart, farFogEnd, dist);
		}
		
		returnColor = mix(vertexColor, vec4(fogColor.xyz, 1), fogAlpha);
	}
	else
	{
		// simple flat color
		returnColor = vertexColor;
	}
	
	//fragColor = vec4(0.7,0.6,0.5,1.0);
	fragColor = vec4(returnColor.rgb,1.0);
}




/** 
 * Returns the fog strength for the given fragment.
 * This is the same implementation as legacy OpenGL's Linear fog option.
 * 1 = completely opaque fog
 * 0 = no fog
 */
float getFogAlpha(float start, float end, float dist)
{
	float fogAlpha = 1 - ((end - dist) / (end - start));
    return clamp(fogAlpha, 0, 1);
}

