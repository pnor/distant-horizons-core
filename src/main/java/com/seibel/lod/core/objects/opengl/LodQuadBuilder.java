package com.seibel.lod.core.objects.opengl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;

import com.seibel.lod.core.api.ApiShared;
import com.seibel.lod.core.api.ClientApi;
import com.seibel.lod.core.enums.LodDirection;
import com.seibel.lod.core.enums.LodDirection.Axis;
import com.seibel.lod.core.enums.config.GpuUploadMethod;
import com.seibel.lod.core.util.ColorUtil;

public class LodQuadBuilder {
	static final int MAX_BUFFER_SIZE = (1024 * 1024 * 1);
	static final int QUAD_BYTE_SIZE = (12 * 6);
	static final int MAX_QUADS_PER_BUFFER = MAX_BUFFER_SIZE / QUAD_BYTE_SIZE;
	//static final int MAX_MERGED_QUAD_SIZE = 64;

	static class Quad {
		final short x;
		final short y;
		final short z;
		short w0;
		short w1;
		int color;
		final byte skylight;
		final byte blocklight;
		final LodDirection dir;
		double distance = 0d;

		Quad(short x, short y, short z, short w0, short w1, int color, byte skylight, byte blocklight,
				LodDirection dir) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.w0 = w0;
			this.w1 = w1;
			this.color = color;
			this.skylight = skylight;
			this.blocklight = blocklight;
			this.dir = dir;
		}
		
		private static double pow(double d) {return d*d;}
		
		// NOTE: This is only a rough but fast calculation!
		void calculateDistance(double relativeX, double relativeY, double relativeZ) {
			distance = pow(relativeX-x) + pow(relativeY-y) + pow(relativeZ-z);
		}
		
		private static int _compondCompare(short a0, short b0, short c0, short a1, short b1, short c1) {
			if (a0 != a1) return a0-a1;
			if (b0 != b1) return b0-b1;
			return c0-c1;
		}
		
		public int compareTo1(Quad o) {
			if (dir != o.dir) return dir.compareTo(o.dir);
			switch (dir.getAxis()) {
			case X:
				return _compondCompare(x, y, z, o.x, o.y, o.z);
			case Y:
				return _compondCompare(y, z, x, o.y, o.z, o.x);
			case Z:
				return _compondCompare(z, y, x, o.z, o.y, o.x);
			default:
				throw new IllegalArgumentException("Invalid Axis enum: " + dir.getAxis());
			}
		}
		public int compareTo2(Quad o) {
			if (dir != o.dir) return dir.compareTo(o.dir);
			switch (dir.getAxis()) {
			case X:
				return _compondCompare(x, z, y, o.x, o.z, o.y);
			case Y:
				return _compondCompare(y, x, z, o.y, o.x, o.z);
			case Z:
				return _compondCompare(z, x, y, o.z, o.x, o.y);
			default:
				throw new IllegalArgumentException("Invalid Axis enum: " + dir.getAxis());
			}
		}
		
		public boolean tryMergeWith1(Quad o)
		{
			if (dir != o.dir)
				return false;
			//if (w0 >= MAX_MERGED_QUAD_SIZE) return false;
			switch (dir.getAxis())
			{
			case X:
				if (x != o.x ||
						y != o.y ||
						z + w0 < o.z)
					return false;
				if (z + w0 > o.z)
				{
					ApiShared.LOGGER.warn("Overlapping quads detected!");
					o.color = ColorUtil.rgbToInt(255, 0, 0);
					return false;
				}
				if (w1 != o.w1 ||
						color != o.color ||
						skylight != o.skylight ||
						blocklight != o.blocklight)
					return false;
				
				w0 += o.w0;
				return true;
			case Y:
				if (y != o.y ||
						z != o.z ||
						x + w0 < o.x)
					return false;
				if (x + w0 > o.x)
				{
					ApiShared.LOGGER.warn("Overlapping quads detected!");
					o.color = ColorUtil.rgbToInt(255, 0, 0);
					return false;
				}
				if (w1 != o.w1 ||
						color != o.color ||
						skylight != o.skylight ||
						blocklight != o.blocklight)
					return false;
				
				w0 += o.w0;
				return true;
			case Z:
				if (z != o.z ||
						y != o.y ||
						x + w0 < o.x)
					return false;
				if (x + w0 > o.x)
				{
					ApiShared.LOGGER.warn("Overlapping quads detected!");
					o.color = ColorUtil.rgbToInt(255, 0, 0);
					return false;
				}
				if (w1 != o.w1 ||
						color != o.color ||
						skylight != o.skylight ||
						blocklight != o.blocklight)
					return false;
				
				w0 += o.w0;
				return true;
			default:
				throw new IllegalArgumentException("Invalid Axis enum: " + dir.getAxis());
			}
		}

		public boolean tryMergeWith2(Quad o)
		{
			if (dir != o.dir)
				return false;
			//if (w1 >= MAX_MERGED_QUAD_SIZE) return false;
			switch (dir.getAxis())
			{
			case X:
				if (x != o.x ||
						z != o.z ||
						y + w1 < o.y)
					return false;
				if (y + w1 > o.y)
				{
					ApiShared.LOGGER.warn("Overlapping quads detected!");
					o.color = ColorUtil.rgbToInt(255, 0, 0);
					return false;
				}
				if (w0 != o.w0 ||
						color != o.color ||
						skylight != o.skylight ||
						blocklight != o.blocklight)
					return false;
				w1 += o.w1;
				return true;
			case Y:
				if (y != o.y ||
						x != o.x ||
						z + w1 < o.z)
					return false;
				if (z + w1 > o.z)
				{
					ApiShared.LOGGER.warn("Overlapping quads detected!");
					o.color = ColorUtil.rgbToInt(255, 0, 0);
					return false;
				}
				if (w0 != o.w0 ||
						color != o.color ||
						skylight != o.skylight ||
						blocklight != o.blocklight)
					return false;
				w1 += o.w1;
				return true;
			case Z:
				if (z != o.z ||
						x != o.x ||
						y + w1 < o.y)
					return false;
				if (y + w1 > o.y)
				{
					ApiShared.LOGGER.warn("Overlapping quads detected!");
					o.color = ColorUtil.rgbToInt(255, 0, 0);
					return false;
				}
				if (w0 != o.w0 ||
						color != o.color ||
						skylight != o.skylight ||
						blocklight != o.blocklight)
					return false;
				w1 += o.w1;
				return true;
			default:
				throw new IllegalArgumentException("Invalid Axis enum: " + dir.getAxis());
			}
		}
		
	}

	final ArrayList<Quad> quads;

	public LodQuadBuilder(int initialSize) {
		quads = new ArrayList<Quad>();
	}

	public void addQuadAdj(LodDirection dir, short x, short y, short z, short w0, short wy, int color, byte skylight,
			byte blocklight) {
		if (dir.ordinal() <= LodDirection.DOWN.ordinal())
			throw new IllegalArgumentException("addQuadAdj() is only for adj direction! Not UP or Down!");
		quads.add(new Quad(x, y, z, w0, wy, color, skylight, blocklight, dir));
	}

	// XZ
	public void addQuadUp(short x, short y, short z, short wx, short wz, int color, byte skylight, byte blocklight) {
		quads.add(new Quad(x, y, z, wx, wz, color, skylight, blocklight, LodDirection.UP));
	}

	public void addQuadDown(short x, short y, short z, short wx, short wz, int color, byte skylight, byte blocklight) {
		quads.add(new Quad(x, y, z, wx, wz, color, skylight, blocklight, LodDirection.DOWN));
	}

	// XY
	public void addQuadN(short x, short y, short z, short wx, short wy, int color, byte skylight, byte blocklight) {
		quads.add(new Quad(x, y, z, wx, wy, color, skylight, blocklight, LodDirection.NORTH));
	}

	public void addQuadS(short x, short y, short z, short wx, short wy, int color, byte skylight, byte blocklight) {
		quads.add(new Quad(x, y, z, wx, wy, color, skylight, blocklight, LodDirection.SOUTH));
	}

	// ZY
	public void addQuadW(short x, short y, short z, short wz, short wy, int color, byte skylight, byte blocklight) {
		quads.add(new Quad(x, y, z, wz, wy, color, skylight, blocklight, LodDirection.WEST));
	}

	public void addQuadE(short x, short y, short z, short wz, short wy, int color, byte skylight, byte blocklight) {
		quads.add(new Quad(x, y, z, wz, wy, color, skylight, blocklight, LodDirection.EAST));
	}

	private static void putVertex(ByteBuffer bb, short x, short y, short z, int color, byte skylight, byte blocklight) {
		skylight %= 16;
		blocklight %= 16;

		bb.putShort(x);
		bb.putShort(y);
		bb.putShort(z);
		bb.putShort((short) (skylight | (blocklight << 4)));
		byte r = (byte) ColorUtil.getRed(color);
		byte g = (byte) ColorUtil.getGreen(color);
		byte b = (byte) ColorUtil.getBlue(color);
		byte a = (byte) ColorUtil.getAlpha(color);
		bb.put(r);
		bb.put(g);
		bb.put(b);
		bb.put(a);
	}

	private static void putQuad(ByteBuffer bb, Quad quad) {
		int[][] quadBase = DIRECTION_VERTEX_QUAD[quad.dir.ordinal()];
		short d0 = quad.w0;
		short d1 = quad.w1;
		Axis axis = quad.dir.getAxis();
		for (int i = 0; i < quadBase.length; i++) {
			short dx, dy, dz;
			switch (axis) {
			case X: // ZY
				dx = 0;
				dz = quadBase[i][0] == 1 ? d0 : 0;
				dy = quadBase[i][1] == 1 ? d1 : 0;
				break;
			case Y: // XZ
				dy = 0;
				dx = quadBase[i][0] == 1 ? d0 : 0;
				dz = quadBase[i][1] == 1 ? d1 : 0;
				break;
			case Z: // XY
				dz = 0;
				dx = quadBase[i][0] == 1 ? d0 : 0;
				dy = quadBase[i][1] == 1 ? d1 : 0;
				break;
			default:
				throw new IllegalArgumentException("Invalid Axis enum: " + axis);
			}
			putVertex(bb, (short) (quad.x + dx), (short) (quad.y + dy), (short) (quad.z + dz), quad.color,
					quad.skylight, quad.blocklight);
		}

	}

	private ByteBuffer writeVertexData(ByteBuffer bb, int quadsStart, int quadsCount) {
		if (quadsStart + quadsCount > quads.size())
			quadsCount = quads.size() - quadsStart;
		bb.clear();
		bb.limit(quadsCount * QUAD_BYTE_SIZE);
		for (Quad quad : quads.subList(quadsStart, quadsStart + quadsCount)) {
			putQuad(bb, quad);
		}
		if (bb.hasRemaining())
			throw new RuntimeException();
		bb.rewind();
		return bb;
	}
	
	public void sort(double dPlayerPosX, double dPlayerPosY, double dPlayerPosZ) {
		quads.forEach(p -> p.calculateDistance(dPlayerPosX, dPlayerPosY, dPlayerPosZ));
		quads.sort((a, b) -> Double.compare(a.distance, b.distance));
	}
	private long merggeQuadsPass1() {
		quads.sort(Quad::compareTo1);
		ListIterator<Quad> iter = quads.listIterator();
		long mergeCount = 0;
		Quad currentQuad = iter.next();
		while (iter.hasNext()) {
			Quad nextQuad = iter.next();
			if (currentQuad.tryMergeWith1(nextQuad)) {
				mergeCount++;
				iter.set(null);
			} else {
				currentQuad = nextQuad;
			}
		}
		quads.removeIf(o -> o==null);
		return mergeCount;
	}
	
	private long merggeQuadsPass2() {
		quads.sort(Quad::compareTo2);
		ListIterator<Quad> iter = quads.listIterator();
		long mergeCount = 0;
		Quad currentQuad = iter.next();
		while (iter.hasNext()) {
			Quad nextQuad = iter.next();
			if (currentQuad.tryMergeWith2(nextQuad)) {
				mergeCount++;
				iter.set(null);
			} else {
				currentQuad = nextQuad;
			}
		}
		quads.removeIf(o -> o==null);
		return mergeCount;
	}
	
	

	public void mergeQuads() {
		if (quads.size()<=1) return;
		
		long mergeCount = 0;
		long preQuadsCount = quads.size();
		mergeCount += merggeQuadsPass1();
		mergeCount += merggeQuadsPass2();
		long postQuadsCount = quads.size();
		//if (mergeCount != 0)
			//ApiShared.LOGGER.info("Merged {} out of {} quads, to now {} quads.", mergeCount, preQuadsCount, postQuadsCount);
	}
	
	public Iterator<ByteBuffer> makeVertexBuffers() {
		int numOfBuffers = getCurrentNeededVertexBuffers();
		return new Iterator<ByteBuffer>() {
			int counter = 0;
			ByteBuffer bb = ByteBuffer.allocateDirect(MAX_QUADS_PER_BUFFER * QUAD_BYTE_SIZE)
					.order(ByteOrder.nativeOrder());

			@Override
			public boolean hasNext() {
				return counter < numOfBuffers;
			}

			@Override
			public ByteBuffer next() {
				if (counter >= numOfBuffers) {
					return null;
				}
				return writeVertexData(bb, MAX_QUADS_PER_BUFFER * counter++, MAX_QUADS_PER_BUFFER);
			}
		};
	}
	
	public interface BufferFiller {
		boolean fill(LodVertexBuffer vbo); // If true: means more data is needed to be filled
	}

	public BufferFiller makeBufferFiller(GpuUploadMethod method) {
		int numOfBuffers = getCurrentNeededVertexBuffers();
		return new BufferFiller() {
			int counter = 0;
			public boolean fill(LodVertexBuffer vbo) {
				if (counter >= numOfBuffers) {
					return false;
				}
				int numOfQuads = MAX_QUADS_PER_BUFFER;
				if (quads.size()-(counter*MAX_QUADS_PER_BUFFER) < MAX_QUADS_PER_BUFFER)
					numOfQuads = quads.size()-(counter*MAX_QUADS_PER_BUFFER);
				if (numOfQuads != 0) {
					ByteBuffer bb = vbo.mapBuffer(numOfQuads*QUAD_BYTE_SIZE, method, MAX_QUADS_PER_BUFFER * QUAD_BYTE_SIZE);
					if (bb == null) throw new NullPointerException("mapBuffer returned null");
					writeVertexData(bb, MAX_QUADS_PER_BUFFER * counter++, numOfQuads).rewind();
					vbo.unmapBuffer(method);
				}
				vbo.vertexCount = numOfQuads*6;
				return counter < numOfBuffers;
			}
		};
	}

	public int getCurrentNeededVertexBuffers() {
		return quads.size() / MAX_QUADS_PER_BUFFER + 1;
	}

	public static final int[][][] DIRECTION_VERTEX_QUAD = new int[][][] {
			// X,Z
			{ // UP
					{ 1, 0 }, // 0
					{ 1, 1 }, // 1
					{ 0, 1 }, // 2

					{ 1, 0 }, // 0
					{ 0, 1 }, // 2
					{ 0, 0 }, // 3
			}, { // DOWN
					{ 0, 0 }, // 0
					{ 0, 1 }, // 1
					{ 1, 1 }, // 2

					{ 0, 0 }, // 0
					{ 1, 1 }, // 2
					{ 1, 0 }, // 3
			},
			// X,Y
			{ // NORTH
					{ 0, 0 }, // 0
					{ 0, 1 }, // 1
					{ 1, 1 }, // 2

					{ 0, 0 }, // 0
					{ 1, 1 }, // 2
					{ 1, 0 }, // 3
			}, { // SOUTH
					{ 1, 0 }, // 0
					{ 1, 1 }, // 1
					{ 0, 1 }, // 2

					{ 1, 0 }, // 0
					{ 0, 1 }, // 2
					{ 0, 0 }, // 3
			},
			// Z,Y
			{ // WEST
					{ 0, 0 }, // 0
					{ 1, 0 }, // 1
					{ 1, 1 }, // 2

					{ 0, 0 }, // 0
					{ 1, 1 }, // 2
					{ 0, 1 }, // 3
			}, { // EAST
					{ 0, 1 }, // 0
					{ 1, 1 }, // 1
					{ 1, 0 }, // 2

					{ 0, 1 }, // 0
					{ 1, 0 }, // 2
					{ 0, 0 }, // 3
			}, };

}
