package com.seibel.lod.core.render.objects;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.TreeSet;

import org.lwjgl.opengl.GL20;

import com.seibel.lod.core.api.ClientApi;


public final class VertexAttributePreGL43 extends VertexAttribute {
	
	// I tried to use as much raw arrays as possible as those lookups
	// happens every frame, and the speed directly effects fps
	int strideSize = 0;
	int[][] bindingPointsToIndex;
	VertexPointer[] pointers;
	int[] pointersOffset;
	
	
	TreeMap<Integer, TreeSet<Integer>> bindingPointsToIndexBuilder;
	ArrayList<VertexPointer> pointersBuilder;

	// This will bind VertexAttribute
	public VertexAttributePreGL43() {
		super(); // also bind VertexAttribute
		bindingPointsToIndexBuilder = new TreeMap<Integer, TreeSet<Integer>>();
		pointersBuilder = new ArrayList<VertexPointer>();
	}
	
	@Override
	// Requires VertexAttribute binded, VertexBuffer binded
	public void bindBufferToAllBindingPoint(int buffer) {
		for (int i=0; i<pointers.length; i++)
			GL20.glEnableVertexAttribArray(i);
		
		for (int i=0; i< pointers.length; i++) {
			VertexPointer pointer = pointers[i];
			if (pointer==null) continue;
			GL20.glVertexAttribPointer(i, pointer.elementCount, pointer.glType,
					pointer.normalized, strideSize, pointersOffset[i]);
		}
	}

	@Override
	// Requires VertexAttribute binded, VertexBuffer binded
	public void bindBufferToBindingPoint(int buffer, int bindingPoint) {
		int[] toBind = bindingPointsToIndex[bindingPoint];
		
		for (int i=0; i<toBind.length; i++)
			GL20.glEnableVertexAttribArray(toBind[i]);
		
		for (int i=0; i< toBind.length; i++) {
			VertexPointer pointer = pointers[toBind[i]];
			if (pointer==null) continue;
			GL20.glVertexAttribPointer(toBind[i], pointer.elementCount, pointer.glType,
					pointer.normalized, strideSize, pointersOffset[toBind[i]]);
		}

	}
	@Override
	// Requires VertexAttribute binded
	public void unbindBuffersFromAllBindingPoint() {
		for (int i=0; i<pointers.length; i++)
			GL20.glDisableVertexAttribArray(i);
	}

	@Override
	// Requires VertexAttribute binded
	public void unbindBuffersFromBindingPoint(int bindingPoint) {
		int[] toBind = bindingPointsToIndex[bindingPoint];
		
		for (int i=0; i<toBind.length; i++)
			GL20.glDisableVertexAttribArray(toBind[i]);
	}

	@Override
	// Requires VertexAttribute binded
	public void setVertexAttribute(int bindingPoint, int attributeIndex, VertexPointer attribute) {
		TreeSet<Integer> intArray = bindingPointsToIndexBuilder.get(bindingPoint);
		if (intArray == null) {
			intArray = new TreeSet<Integer>();
			bindingPointsToIndexBuilder.put(bindingPoint, intArray);
		}
		intArray.add(attributeIndex);
		
		while (pointersBuilder.size() <= attributeIndex) {
			// This is dumb, but ArrayList doesn't have a resize, And this code
			// should only be ran when it's building the Vertex Attribute anyways.
			pointersBuilder.add(null);
		}
		pointersBuilder.set(attributeIndex, attribute);
	}

	@Override
	// Requires VertexAttribute binded
	public void completeAndCheck(int expectedStrideSize) {
		int maxBindPointNumber = bindingPointsToIndexBuilder.lastKey();
		bindingPointsToIndex = new int[maxBindPointNumber+1][];
		
		bindingPointsToIndexBuilder.forEach((Integer i, TreeSet<Integer> set) -> {
			bindingPointsToIndex[i] = new int[set.size()];
			Iterator<Integer> iter = set.iterator();
			for (int j = 0; j<set.size(); j++) {
				bindingPointsToIndex[i][j] = iter.next();
			}
		});
		
		pointers = pointersBuilder.toArray(new VertexPointer[pointersBuilder.size()]);
		pointersOffset = new int[pointers.length];
		pointersBuilder = null; // Release the builder
		bindingPointsToIndexBuilder = null; // Release the builder
		
		// Check if all pointers are valid
		int currentOffset = 0;
		for (int i = 0; i < pointers.length; i++) {
			VertexPointer pointer = pointers[i];
			if (pointer == null) {
				ClientApi.LOGGER.warn("Vertex Attribute index "+i+" is not set! No index should be skipped normally!");
				continue;
			}
			pointersOffset[i] = currentOffset;
			currentOffset += pointer.byteSize;
		}
		if (currentOffset != expectedStrideSize)
			ClientApi.LOGGER.error("Vertex Attribute calculated stride size " + currentOffset +
					" does not match the provided expected stride size " + expectedStrideSize + "!");
		strideSize = currentOffset;
		ClientApi.LOGGER.info("Vertex Attribute (pre GL43) completed.");
		
		// Debug logging
		ClientApi.LOGGER.info("Vertex Attribute Debug Data:");
		ClientApi.LOGGER.info("AttributeIndex: ElementCount, glType, normalized, strideSize, offset");
		
		for (int i=0; i< pointers.length; i++) {
			VertexPointer pointer = pointers[i];
			if (pointer==null) {
				ClientApi.LOGGER.warn(i + ": Null!!!!");
				continue;
				}
			ClientApi.LOGGER.info(i + ": "+pointer.elementCount+", "+
				pointer.glType+", "+pointer.normalized+", "+strideSize+", "+pointersOffset[i]);
		}
		
	}

}
