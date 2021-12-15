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

package com.seibel.lod.core.render;

import java.awt.Color;

import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL42;
import org.lwjgl.opengl.GL43;

import com.seibel.lod.core.enums.rendering.FogDistance;
import com.seibel.lod.core.enums.rendering.FogDrawMode;
import com.seibel.lod.core.objects.math.Mat4f;
import com.seibel.lod.core.objects.math.Vec3f;
import com.seibel.lod.core.render.objects.ShaderProgram;
import com.seibel.lod.core.render.objects.VertexAttribute;
import com.seibel.lod.core.render.objects.VertexAttributePostGL43;
import com.seibel.lod.core.render.objects.VertexAttributePreGL43;
import com.seibel.lod.core.util.LodUtil;

public class LodRenderProgram extends ShaderProgram {
	public static final String VERTEX_SHADER_PATH = "shaders/standard.vert";
	public static final String FRAGMENT_SHADER_PATH = "shaders/flat_shaded.frag";
	
	public final VertexAttribute vao;
	
	// Attributes
	public final int posAttrib;
	public final int colAttrib;
	public final int blockSkyLightAttrib;
	public final int blockLightAttrib;
	// Uniforms
	public final int mvmUniform;
	public final int projUniform;
	public final int cameraUniform;
	public final int fogColorUniform;
	// public final int skyLightUniform; worldSkyLight is currently not used
	public final int lightMapUniform;
	// Fog Uniforms
	public final int fogEnabledUniform;
	public final int nearFogEnabledUniform;
	public final int farFogEnabledUniform;
	public final int nearFogStartUniform;
	public final int nearFogEndUniform;
	public final int farFogStartUniform;
	public final int farFogEndUniform;
	
	public LodRenderProgram() {
		super(VERTEX_SHADER_PATH, FRAGMENT_SHADER_PATH, "fragColor");
		
		super.bind();
		
        posAttrib = getAttributeLocation("vPosition");
        colAttrib = getAttributeLocation("color");
        blockSkyLightAttrib = getAttributeLocation("blockSkyLight");
        blockLightAttrib = getAttributeLocation("blockLight");
        
        mvmUniform = getUniformLocation("modelViewMatrix");
		projUniform = getUniformLocation("projectionMatrix");
		cameraUniform = getUniformLocation("cameraPos");
		fogColorUniform = getUniformLocation("fogColor");
		// skyLightUniform = getUniformLocation("worldSkyLight");
		lightMapUniform = getUniformLocation("lightMap");

		// Fog uniforms
		fogEnabledUniform = getUniformLocation("fogEnabled");
		nearFogEnabledUniform = getUniformLocation("nearFogEnabled");
		farFogEnabledUniform = getUniformLocation("farFogEnabled");
		// near
		nearFogStartUniform = getUniformLocation("nearFogStart");
		nearFogEndUniform = getUniformLocation("nearFogEnd");
		// far
		farFogStartUniform = getUniformLocation("farFogStart");
		farFogEndUniform = getUniformLocation("farFogEnd");
		
		// TODO: Add better use of the LODFormat thing
		int vertexByteCount = LodUtil.LOD_VERTEX_FORMAT.getByteSize();
		if (GLProxy.getInstance().openGL43VertexAttributeSupported)
			vao = new VertexAttributePostGL43();
		else
			vao = new VertexAttributePreGL43();
		vao.bind();
		vao.setVertexAttribute(0, posAttrib, VertexAttribute.VertexPointer.addVec3Pointer(false));
		vao.setVertexAttribute(0, colAttrib, VertexAttribute.VertexPointer.addUnsignedBytesPointer(4, true));
		vao.setVertexAttribute(0, blockSkyLightAttrib, VertexAttribute.VertexPointer.addUnsignedBytePointer(false));
		vao.setVertexAttribute(0, blockLightAttrib, VertexAttribute.VertexPointer.addUnsignedBytePointer(false));
		vao.completeAndCheck(vertexByteCount);
		//vao.unbind();
		//super.unbind();
	}
	
	// Override ShaderProgram.bind()
	public void bind() {
		super.bind();
		vao.bind();
	}
	// Override ShaderProgram.unbind()
	public void unbind() {
		super.unbind();
		vao.unbind();
	}
	
	// Override ShaderProgram.free()
	public void free() {
		vao.free();
		super.free();
	}
	
	public void bindVertexBuffer(int vbo) {
		vao.bindBufferToAllBindingPoint(vbo);
	}
	
	public void unbindVertexBuffer() {
		vao.unbindBufferFromAllBindingPoint();
	}
	
	public void fillUniformData(Mat4f modelViewMatrix, Mat4f projectionMatrix, Vec3f cameraPos, Color fogColor, int skyLight, int lightmapBindPoint) {
        super.bind();
		// uniforms
        setUniform(mvmUniform, modelViewMatrix);
		setUniform(projUniform, projectionMatrix);
		setUniform(cameraUniform, cameraPos);
		setUniform(fogColorUniform, fogColor);
		// setUniform(skyLightUniform, skyLight);
		setUniform(lightMapUniform, lightmapBindPoint);
	}
	
	public void fillUniformDataForFog(LodFogConfig fogSettings) {
		super.bind();
		if (fogSettings.fogDrawMode != FogDrawMode.FOG_DISABLED) {
			setUniform(fogEnabledUniform, true);
			setUniform(nearFogEnabledUniform, fogSettings.fogDistance != FogDistance.FAR);
			setUniform(farFogEnabledUniform, fogSettings.fogDistance != FogDistance.NEAR);
			// near
			setUniform(nearFogStartUniform, fogSettings.nearFogStart);
			setUniform(nearFogEndUniform, fogSettings.nearFogEnd);
			// far
			setUniform(farFogStartUniform, fogSettings.farFogStart);
			setUniform(farFogEndUniform, fogSettings.farFogEnd);
		} else {
			setUniform(fogEnabledUniform, false);
		}
	}

}
