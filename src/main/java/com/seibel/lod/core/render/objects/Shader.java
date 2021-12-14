/*
 *    This file is part of the Distant Horizon mod (formerly the LOD Mod),
 *    licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2021  James Seibel
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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.lwjgl.opengl.GL20;

import com.seibel.lod.core.api.ClientApi;

/**
 * This object holds a OpenGL reference to a shader
 * and allows for reading in and compiling a shader file.
 * 
 * @author James Seibel
 * @version 11-8-2021
 */
public class Shader
{	
	/** OpenGL shader ID */
	public final int id;
	
	/** Creates a shader with specified type.
	 * @param type Either GL_VERTEX_SHADER or GL_FRAGMENT_SHADER.
	 * @param path File path of the shader
	 * @param absoluteFilePath If false the file path is relative to the resource jar folder.
	 * @throws RuntimeException if the shader fails to compile 
	 */
	public Shader(int type, String path, boolean absoluteFilePath)
	{
		ClientApi.LOGGER.info("Loading shader at "+path);
		// Create an empty shader object
		id = GL20.glCreateShader(type);
		StringBuilder source = loadFile(path, absoluteFilePath);
		GL20.glShaderSource(id, source);

		GL20.glCompileShader(id);
		// check if the shader compiled
		int status = GL20.glGetShaderi(id, GL20.GL_COMPILE_STATUS);
		if (status != GL20.GL_TRUE) {
			String message = "Shader compiler error. Details: "+GL20.glGetShaderInfoLog(id);
			free(); // important!
			throw new RuntimeException(message);
		}
		ClientApi.LOGGER.info("Shader at "+path+" loaded sucessfully.");
	}

	// REMEMBER to always free the resource!
	public void free() {
		GL20.glDeleteShader(id);
	}
	
	private StringBuilder loadFile(String path, boolean absoluteFilePath) {
		StringBuilder stringBuilder = new StringBuilder();
		
		try
		{
			// open the file
			InputStream in;
			if (absoluteFilePath) {
				// Throws FileNotFoundException
				in = new FileInputStream(path); // Note: this should use OS path seperator
			} else {
				in = Shader.class.getClassLoader().getResourceAsStream(path); // Note: path seperator should be '/'
				if (in == null) {
					throw new FileNotFoundException("Shader file not found in resource: "+path);
				}
			}
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			
			// read in the file
			String line;
			while ((line = reader.readLine()) != null)
				stringBuilder.append(line).append("\n");
		}
		catch (IOException e)
		{
			throw new RuntimeException("Unable to load shader from file [" + path + "]. Error: " + e.getMessage());
		}
		return stringBuilder;
	}
}
