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
 
package com.seibel.lod.core.render;

import com.seibel.lod.core.enums.config.EGpuUploadMethod;
import com.seibel.lod.core.enums.config.ELoggerMode;
import com.seibel.lod.core.handlers.dependencyInjection.SingletonHandler;
import com.seibel.lod.core.logging.ConfigBasedLogger;

import com.seibel.lod.core.logging.ConfigBasedSpamLogger;
import com.seibel.lod.core.render.objects.GLVertexBuffer;
import com.seibel.lod.core.render.objects.GLState;
import com.seibel.lod.core.render.objects.ShaderProgram;
import com.seibel.lod.core.render.objects.VertexAttribute;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftRenderWrapper;
import org.apache.logging.log4j.LogManager;
import org.lwjgl.opengl.GL32;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;


public class RenderSystemTest {

    public RenderSystemTest() {}

    public static final ConfigBasedLogger logger = new ConfigBasedLogger(
            LogManager.getLogger(RenderSystemTest.class), () -> ELoggerMode.LOG_ALL_TO_CHAT);
    public static final ConfigBasedSpamLogger spamLogger = new ConfigBasedSpamLogger(
            LogManager.getLogger(RenderSystemTest.class), () -> ELoggerMode.LOG_ALL_TO_CHAT, 1);
    private static final IMinecraftRenderWrapper MC_RENDER = SingletonHandler.get(IMinecraftRenderWrapper.class);

    ShaderProgram basicShader;
    GLVertexBuffer sameContextBuffer;
    GLVertexBuffer sharedContextBuffer;
    VertexAttribute va;
    boolean init = false;

    public void init() {
        if (init) return;
        logger.info("init");
        init = true;
        va = VertexAttribute.create();
        va.bind();
        // Pos
        va.setVertexAttribute(0, 0, VertexAttribute.VertexPointer.addVec2Pointer(false));
        // Color
        va.setVertexAttribute(0, 1, VertexAttribute.VertexPointer.addVec4Pointer(false));
        va.completeAndCheck(Float.BYTES * 6);
        basicShader = new ShaderProgram("shaders/test/vert.vert", "shaders/test/frag.frag",
                "fragColor", new String[]{"vPosition", "color"});
        createBuffer();
    }

    // Render a square with uv color
    private static final float[] vertices = {
            // PosX,Y, ColorR,G,B,A
        -0.5f, -0.5f, 0.0f, 0.0f, 0.0f, 1.0f,
        0.4f, -0.4f, 1.0f, 0.0f, 0.0f, 1.0f,
        0.3f, 0.3f, 1.0f, 1.0f, 0.0f, 0.0f,
        -0.2f, 0.2f, 0.0f, 1.0f, 1.0f, 1.0f
    };

    private static GLVertexBuffer createTextingBuffer() {
        ByteBuffer buffer = ByteBuffer.allocateDirect(vertices.length * Float.BYTES);
        // Fill buffer with the vertices.
        buffer = buffer.order(ByteOrder.nativeOrder());
        buffer.asFloatBuffer().put(vertices);
        buffer.rewind();
        GLVertexBuffer vbo = new GLVertexBuffer(false);
        vbo.bind();
        vbo.uploadBuffer(buffer, 4, EGpuUploadMethod.DATA, vertices.length * Float.BYTES);
        return vbo;
    }

    private void createBuffer() {
        GLProxy.getInstance().recordOpenGlCall(() -> sharedContextBuffer = createTextingBuffer());
        GLProxy.ensureAllGLJobCompleted();
        sameContextBuffer = createTextingBuffer();
    }

    public void render() {
        spamLogger.debug("rendering");

        GLState state = new GLState();
        init();
        GL32.glBindFramebuffer(GL32.GL_FRAMEBUFFER, MC_RENDER.getTargetFrameBuffer());
        GL32.glViewport(0,0, MC_RENDER.getTargetFrameBufferViewportWidth(), MC_RENDER.getTargetFrameBufferViewportHeight());
        GL32.glPolygonMode(GL32.GL_FRONT_AND_BACK, GL32.GL_FILL);
        GL32.glDisable(GL32.GL_CULL_FACE);
        GL32.glDisable(GL32.GL_DEPTH_TEST);
        GL32.glDisable(GL32.GL_STENCIL_TEST);
        GL32.glDisable(GL32.GL_BLEND);
        //GL32.glDisable(GL32.GL_SCISSOR_TEST);

        basicShader.bind();
        va.bind();

        // Switch between the two buffers per second
        if (System.currentTimeMillis() % 2000 < 1000) {
            sameContextBuffer.bind();
            va.bindBufferToAllBindingPoint(sameContextBuffer.getId());
            spamLogger.debug("same context buffer");
        } else {
            sameContextBuffer.bind();
            va.bindBufferToAllBindingPoint(sharedContextBuffer.getId());
            spamLogger.debug("shared context buffer");
        }
        // Render the square
        GL32.glDrawArrays(GL32.GL_TRIANGLE_FAN, 0, 4);
        GL32.glClear(GL32.GL_DEPTH_BUFFER_BIT);

        state.restore();
        spamLogger.incLogTries();
    }



}
