package com.seibel.lod.core.objects.opengl;

public class RenderRegion implements AutoCloseable {
	 LodVertexBuffer[] vbos;
	
	public RenderRegion(int size) {
		vbos = new LodVertexBuffer[size];
	}
	
	public void resize(int size) {
		if (vbos.length != size) {
			LodVertexBuffer[] newVbos = new LodVertexBuffer[size];
			if (vbos.length > size) {
				for (int i=size; i<vbos.length; i++) {
					vbos[i].close();
					vbos[i] = null;
				}
			}
			for (int i=0; i<newVbos.length && i<vbos.length; i++) {
				newVbos[i] = vbos[i];
				vbos[i] = null;
			}
			for (LodVertexBuffer b : vbos) {
				if (b != null) throw new RuntimeException("LEAKING VBO!");
			}
			vbos = newVbos;
		}
	}
	
	public LodVertexBuffer[] debugGetBuffers() {
		return vbos;
	} 

	@Override
	public void close() {
		
		
	}

	public LodVertexBuffer getOrMakeVbo(int iIndex, boolean useBuffStorage) {
		if (vbos[iIndex] == null) {
			vbos[iIndex] = new LodVertexBuffer(useBuffStorage);
		} else if (vbos[iIndex].isBufferStorage != useBuffStorage) {
			vbos[iIndex].close();
			vbos[iIndex] = new LodVertexBuffer(useBuffStorage);
		}
		return vbos[iIndex];
	}

}
