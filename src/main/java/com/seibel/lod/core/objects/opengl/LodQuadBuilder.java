package com.seibel.lod.core.objects.opengl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Iterator;

import com.seibel.lod.core.enums.LodDirection;
import com.seibel.lod.core.enums.LodDirection.Axis;
import com.seibel.lod.core.util.ColorUtil;

public class LodQuadBuilder {
	static final int MAX_BUFFER_SIZE = (1024 * 1024 * 1);
	static final int QUAD_BYTE_SIZE = (12 * 6);
	static final int MAX_QUADS_PER_BUFFER = MAX_BUFFER_SIZE / QUAD_BYTE_SIZE;

	static class Quad {
		final short x;
		final short y;
		final short z;
		final short w0;
		final short w1;
		final int color;
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
	}

	final ArrayList<Quad> quads;

	public LodQuadBuilder(int initialSize) {
		quads = new ArrayList<Quad>(initialSize);
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
