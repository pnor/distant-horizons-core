/*
 *    This file is part of the Distant Horizons mod (formerly the LOD Mod),
 *    licensed under the GNU LGPL v3 License.
 *
 *    Copyright (C) 2020-2022  James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.seibel.lod.core.render.objects;

import java.awt.Color;
import java.nio.FloatBuffer;
import java.util.function.Supplier;

import org.lwjgl.opengl.GL32;
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

	// TODO: A better way to set the fragData output name
	/** Creates a shader program.
	  * This will bind ShaderProgram */
	public ShaderProgram(String vert, String frag, String fragDataOutputName, String[] attributes)
	{
		this(() -> Shader.loadFile(vert, false, new StringBuilder()).toString(),
				() -> Shader.loadFile(frag, false, new StringBuilder()).toString(),
	fragDataOutputName, attributes);
	}

	public ShaderProgram(Supplier<String> vert, Supplier<String> frag, String fragDataOutputName, String[] attributes)
	{
		Shader vertShader = new Shader(GL32.GL_VERTEX_SHADER, vert.get());
		Shader fragShader = new Shader(GL32.GL_FRAGMENT_SHADER, frag.get());

		id = GL32.glCreateProgram();

		GL32.glAttachShader(this.id, vertShader.id);
		GL32.glAttachShader(this.id, fragShader.id);
		//GL32.glBindFragDataLocation(id, 0, fragDataOutputName);
		for (int i = 0; i < attributes.length; i++) {
			GL32.glBindAttribLocation(id, i, attributes[i]);
		}
		GL32.glLinkProgram(this.id);

		vertShader.free(); // important!
		fragShader.free(); // important!

		int status = GL32.glGetProgrami(this.id, GL32.GL_LINK_STATUS);
		if (status != GL32.GL_TRUE) {
			String message = "Shader Link Error. Details: "+GL32.glGetProgramInfoLog(this.id);
			free(); // important!
			throw new RuntimeException(message);
		}
		GL32.glUseProgram(id); // This HAVE to be a direct call to prevent calling the overloaded version
	}

	/** This will bind ShaderProgram */
	public void bind()
	{
		GL32.glUseProgram(id);
	}
	/** This will unbind ShaderProgram */
	public void unbind() {
		GL32.glUseProgram(0);
	}
	
	// REMEMBER to always free the resource!
	public void free()
	{
		GL32.glDeleteProgram(id);
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
		int i = GL32.glGetAttribLocation(id, name);
		if (i==-1) throw new RuntimeException("Attribute name not found: "+name);
		return i;
	}
	// Same as above but without throwing errors.
	// Return -1 if attribute doesn't exist or has been optimized out
	public int tryGetAttributeLocation(CharSequence name)
	{
		return GL32.glGetAttribLocation(id, name);
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
		int i = GL32.glGetUniformLocation(id, name);
		if (i==-1) throw new RuntimeException("Uniform name not found: "+name);
		return i;
	}

	// Same as above but without throwing errors.
	// Return -1 if uniform doesn't exist or has been optimized out
	public int tryGetUniformLocation(CharSequence name)
	{
		return GL32.glGetUniformLocation(id, name);
	}

	/** Requires ShaderProgram binded. */
	public void setUniform(int location, boolean value)
	{
		// This use -1 for false as that equals all one set
		GL32.glUniform1i(location, value ? 1 : 0);
	}

	/** Requires ShaderProgram binded. */
	public void setUniform(int location, int value)
	{
		GL32.glUniform1i(location, value);
	}

	/** Requires ShaderProgram binded. */
	public void setUniform(int location, float value)
	{
		GL32.glUniform1f(location, value);
	}

	/** Requires ShaderProgram binded. */
	public void setUniform(int location, Vec3f value)
	{
		GL32.glUniform3f(location, value.x, value.y, value.z);
	}

	/** Requires ShaderProgram binded. */
	public void setUniform(int location, Vec3d value)
	{
		GL32.glUniform3f(location, (float) value.x, (float) value.y, (float) value.z);
	}

	/** Requires ShaderProgram binded. */
	public void setUniform(int location, Mat4f value)
	{
		try (MemoryStack stack = MemoryStack.stackPush())
		{
			FloatBuffer buffer = stack.mallocFloat(4 * 4);
			value.store(buffer);
			GL32.glUniformMatrix4fv(location, false, buffer);
		}
	}
	
	/** Converts the color's RGBA values into values between 0 and 1.
	  * Requires ShaderProgram binded. */
	public void setUniform(int location, Color value)
	{
		GL32.glUniform4f(location, value.getRed() / 256.0f, value.getGreen() / 256.0f, value.getBlue() / 256.0f, value.getAlpha() / 256.0f);
	}
	
}
