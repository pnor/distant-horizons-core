package com.seibel.lod.core.render.objects;

import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

public class LightmapTexture {
	public final int id;
	
	public LightmapTexture() {
		id = GL30.glGenTextures();
		bind();
		GL20.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_WRAP_S, GL20.GL_CLAMP_TO_BORDER);
		GL20.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_WRAP_T, GL20.GL_CLAMP_TO_BORDER);
		GL20.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_MIN_FILTER, GL20.GL_NEAREST);
		GL20.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_MAG_FILTER, GL20.GL_NEAREST);
	}
	
	public void bind() {
		GL20.glBindTexture(GL20.GL_TEXTURE_2D, id);
	}
	public void unbind() {
		GL20.glBindTexture(GL20.GL_TEXTURE_2D, 0);
	}

	public void free() {
		GL20.glDeleteTextures(id);
	}
	
	public void fillData(int lightMapWidth, int lightMapHeight, int[] pixels) {
		if (pixels.length != lightMapWidth*lightMapHeight)
			throw new RuntimeException("Lightmap Width*Height not equal to pixels provided!");
		
		// comment me out to see when the lightmap is changing
//			boolean same = true;
//			int badIndex = 0;
//			if (testArray != null && pixels != null)
//			for (int i = 0; i < pixels.length; i++)
//			{
//				if(pixels[i] != testArray[i])
//				{
//					same = false;
//					badIndex = i;
//					break;	
//				}
//			}
//			testArray = pixels;
//			MC.sendChatMessage(same + " " + badIndex);
		
		// comment this line out to prevent uploading the new lightmap
		GL20.glTexImage2D(GL20.GL_TEXTURE_2D, 0, GL20.GL_RGBA, lightMapWidth,
				lightMapHeight, 0, GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE, pixels);
	}
	
}
