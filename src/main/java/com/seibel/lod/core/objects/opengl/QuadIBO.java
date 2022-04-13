package com.seibel.lod.core.objects.opengl;

import org.lwjgl.system.MemoryUtil;

import static org.lwjgl.opengl.GL11C.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL45.*;

public class QuadIBO {
    //Datatype of the stored indices (can be GL_UNSIGNED_INT, GL_UNSIGNED_SHORT, GL_UNSIGNED_BYTE)
    public int type;
    //IBO object
    int id;
    //Current capacity (in quads)
    int ccap;

    //Global object, used for sharing the IBO for any draw calls
    public static QuadIBO GLOBAL = new QuadIBO();

    public QuadIBO() {
        id = glCreateBuffers();
    }

    public void resize(int cap) {
        //If requested capacity is less than or equal to current capacity, ignore
        if (cap <= ccap)
            return;
        //Not really necessary, just to stop constant resizes
        cap *= 1.5;
        System.out.println("Resizing from "+ccap+" to " + cap);

        ccap = cap;

        //TODO: DO DYNAMIC TYPES, just makes things more efficent
        type = GL_UNSIGNED_INT;
        int DT_SIZE = 4;//Datatype size (int: 4, short: 2, byte: 1)

        //Resize the buffer
        glNamedBufferData(id, (long) DT_SIZE * 6 * cap, GL_STATIC_DRAW);// 4L is datatype
        //Map and write the index data to the buffer
        long ptr = nglMapNamedBuffer(id, GL_WRITE_ONLY);
        for (int base = 0; base < cap; base++) {
            //Write index's
            MemoryUtil.memPutInt(ptr+(base*6*DT_SIZE+DT_SIZE*0),(int)(base*4 + 0));
            MemoryUtil.memPutInt(ptr+(base*6*DT_SIZE+DT_SIZE*1),(int)(base*4 + 1));
            MemoryUtil.memPutInt(ptr+(base*6*DT_SIZE+DT_SIZE*2),(int)(base*4 + 2));
            MemoryUtil.memPutInt(ptr+(base*6*DT_SIZE+DT_SIZE*3),(int)(base*4 + 2));
            MemoryUtil.memPutInt(ptr+(base*6*DT_SIZE+DT_SIZE*4),(int)(base*4 + 3));
            MemoryUtil.memPutInt(ptr+(base*6*DT_SIZE+DT_SIZE*5),(int)(base*4 + 0));
        }
        glUnmapNamedBuffer(id);
    }

    public void bind(int capacity) {
        resize(capacity);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, id);
    }
}
