package com.seibel.lod.wrappers;

import java.nio.FloatBuffer;

import com.seibel.lod.objects.rending.Mat4f;

import net.minecraft.util.math.vector.Matrix4f;

/**
 * This class converts between Minecraft objects (Ex: Matrix4f)
 * and objects we created (Ex: Mat4f).
 * Since we don't want to deal with a bunch of tiny changes
 * every time Minecraft renames a variable in Matrix4f or something.
 * 
 * @author James Seibel
 * @version 11-11-2021
 */
public class McObjectConverter
{
	
	public McObjectConverter()
	{
		
	}
	
	
	/** 4x4 float matrix converter */
	public static Mat4f Convert(Matrix4f mcMatrix)
	{
		FloatBuffer buffer = FloatBuffer.allocate(16);
		mcMatrix.store(buffer);
		Mat4f matrix = new Mat4f(buffer);
		matrix.transpose();
		return matrix;
	}
}
