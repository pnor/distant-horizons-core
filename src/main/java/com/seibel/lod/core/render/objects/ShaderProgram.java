/*
 *    This file is part of the Distant Horizon mod (formerly the LOD Mod),
 *    licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2020  James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.seibel.lod.core.render.objects;

import java.awt.Color;
import java.nio.FloatBuffer;

import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;

import com.seibel.lod.core.objects.math.Mat4f;
import com.seibel.lod.core.objects.math.Vec3d;
import com.seibel.lod.core.objects.math.Vec3f;


/**
 * This object holds the reference to a OpenGL shader program
 * and contains a few methods that can be used with OpenGL shader programs.
 * The reason for many of these simple wrapper methods is as reminders of what
 * can (and needs to be) done with a shader program.
 * 
 * @author James Seibel
 * @version 11-26-2021
 */
public class ShaderProgram
{
	/** Stores the handle of the program. */
	public final int id;
	
	/** Creates a shader program. */
	// FIXME: A better way to set the fragData output name
	public ShaderProgram(String vert, String frag, String fragDataOutputName)
	{
		Shader vertShader = new Shader(GL20.GL_VERTEX_SHADER, vert, false);
		Shader fragShader = new Shader(GL20.GL_FRAGMENT_SHADER, frag, false);
		
		id = GL20.glCreateProgram();
		
		GL20.glAttachShader(this.id, vertShader.id);
		GL20.glAttachShader(this.id, fragShader.id);
	    //GL30.glBindFragDataLocation(id, 0, fragDataOutputName);
		GL20.glLinkProgram(this.id);
		
		vertShader.free(); // important!
		fragShader.free(); // important!
		
		int status = GL20.glGetProgrami(this.id, GL20.GL_LINK_STATUS);
		if (status != GL20.GL_TRUE) {
			String message = "Shader Link Error. Details: "+GL20.glGetProgramInfoLog(this.id);
			free(); // important!
			throw new RuntimeException(message);
		}
	}
	
	/** Calls GL20.glUseProgram(this.id) */
	public void bind()
	{
		GL20.glUseProgram(id);
	}
	public void unbind() {
		GL20.glUseProgram(0);
	}
	
	// REMEMBER to always free the resource!
	public void free()
	{
		GL20.glDeleteProgram(id);
	}
	
	/** WARNING: Slow native call! Cache it if possible!
	 * Gets the location of an attribute variable with specified name.
	 * Calls GL20.glGetAttribLocation(id, name)
	 *
	 * @param name Attribute name
	 * @throws RuntimeException if attribute not found
	 * @return Location of the attribute
	 */
	public int getAttributeLocation(CharSequence name)
	{
		int i = GL20.glGetAttribLocation(id, name);
		if (i==-1) throw new RuntimeException("Attribute name not found: "+name);
		return i;
	}
	
	/** WARNING: Slow native call! Cache it if possible!
	 * Gets the location of a uniform variable with specified name.
	 * Calls GL20.glGetUniformLocation(id, name)
	 *
	 * @param name Uniform name
	 * @throws RuntimeException if uniform not found
	 * @return Location of the Uniform
	 */
	public int getUniformLocation(CharSequence name)
	{
		int i = GL20.glGetUniformLocation(id, name);
		if (i==-1) throw new RuntimeException("Uniform name not found: "+name);
		return i;
	}
	
	public void setUniform(int location, boolean value)
	{
		// This use -1 for false as that equals all one set
		GL20.glUniform1i(location, value ? 1 : 0);
	}
	
	public void setUniform(int location, int value)
	{
		GL20.glUniform1i(location, value);
	}
	
	public void setUniform(int location, float value)
	{
		GL20.glUniform1f(location, value);
	}
	
	public void setUniform(int location, Vec3f value)
	{
		GL20.glUniform3f(location, value.x, value.y, value.z);
	}
	
	public void setUniform(int location, Vec3d value)
	{
		GL20.glUniform3f(location, (float) value.x, (float) value.y, (float) value.z);
	}
	
	public void setUniform(int location, Mat4f value)
	{
		try (MemoryStack stack = MemoryStack.stackPush())
		{
			FloatBuffer buffer = stack.mallocFloat(4 * 4);
			value.store(buffer);
			GL20.glUniformMatrix4fv(location, false, buffer);
		}
	}
	
	/** Converts the color's RGBA values into values between 0 and 1. */
	public void setUniform(int location, Color value)
	{
		GL20.glUniform4f(location, value.getRed() / 256.0f, value.getGreen() / 256.0f, value.getBlue() / 256.0f, value.getAlpha() / 256.0f);
	}
	
}