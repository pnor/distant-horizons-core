package com.seibel.lod.core.render.objects;

import java.util.ArrayList;

import org.lwjgl.opengl.GL43;

import com.seibel.lod.core.api.ClientApi;
import com.seibel.lod.core.render.objects.VertexAttribute.VertexPointer;

// In OpenGL 4.3 and later, Vertex Attribute got a make-over.
// Now it provides support for buffer binding points natively.
// This means that setting up the VAO just use ONE native call when
// binding to a buffer.
//
// Since I no longer needs to implement binding points, I also no
// longer needs to keep track of Pointers.

public final class VertexAttributePostGL43 extends VertexAttribute {

	int numberOfBindingPoints = 0;
	int strideSize = 0;

	ArrayList<VertexPointer> pointersBuilder;
	
	public VertexAttributePostGL43() {
		super();
	}
	
	@Override
	public void bindBufferToAllBindingPoint(int buffer) {
		for (int i=0; i<numberOfBindingPoints; i++)
			GL43.glBindVertexBuffer(i, buffer, 0, strideSize);
	}

	@Override
	public void bindBufferToBindingPoint(int buffer, int bindingPoint) {
		GL43.glBindVertexBuffer(bindingPoint, buffer, 0, strideSize);
	}

	@Override
	public void unbindBufferFromAllBindingPoint() {
		for (int i=0; i<numberOfBindingPoints; i++)
			GL43.glBindVertexBuffer(i, 0, 0, 0);
	}

	@Override
	public void unbindBufferFromBindingPoint(int bindingPoint) {
		GL43.glBindVertexBuffer(bindingPoint, 0, 0, 0);
	}

	@Override
	public void setVertexAttribute(int bindingPoint, int attributeIndex, VertexPointer attribute) {
		GL43.glVertexAttribFormat(attributeIndex, attribute.elementCount,
				attribute.glType, attribute.normalized, strideSize);
		strideSize += attribute.byteSize;
		if (numberOfBindingPoints <= bindingPoint) numberOfBindingPoints = bindingPoint+1;
		GL43.glVertexAttribBinding(attributeIndex, bindingPoint);
	}

	@Override
	public void completeAndCheck(int expectedStrideSize) {
		if (strideSize != expectedStrideSize) {
			ClientApi.LOGGER.error("Vertex Attribute calculated stride size " + strideSize +
					" does not match the provided expected stride size " + expectedStrideSize + "!");
		}
		ClientApi.LOGGER.info("Vertex Attribute (GL43+) completed.");
	}

}
