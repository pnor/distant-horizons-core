package com.seibel.lod.core.objects.opengl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;

import com.seibel.lod.core.builders.lodBuilding.LodBuilder;
import com.seibel.lod.core.enums.LodDirection;
import com.seibel.lod.core.enums.LodDirection.Axis;
import com.seibel.lod.core.enums.config.GpuUploadMethod;
import com.seibel.lod.core.handlers.dependencyInjection.SingletonHandler;
import com.seibel.lod.core.util.ColorUtil;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton;

import static com.seibel.lod.core.render.LodRenderer.EVENT_LOGGER;

public class LodQuadBuilder {
	static final int MAX_BUFFER_SIZE = (1024 * 1024 * 1);
	static final int QUAD_BYTE_SIZE = (12 * 6);
	static final int MAX_QUADS_PER_BUFFER = MAX_BUFFER_SIZE / QUAD_BYTE_SIZE;
	//static final int MAX_MERGED_QUAD_SIZE = 64;

	static final ILodConfigWrapperSingleton CONFIG = SingletonHandler.get(ILodConfigWrapperSingleton.class);

	public final boolean skipSkylight0Quads;
	public final short skyLightCullingBelow;

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
			if (w0 == 0 || w1 == 0) throw new IllegalArgumentException("Size 0 quad!");
			if (w0 < 0 || w1 < 0) throw new IllegalArgumentException("Negative sized quad!");
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
		
		private static int _compondCompare(short a0, short a1, short a2, short b0, short b1, short b2) {
			long a = (long)a0<<48 | (long)a1<<32 | (long)a2 << 16;
			long b = (long)b0<<48 | (long)b1<<32 | (long)b2 << 16;
			return Long.compare(a, b);
		}
		
		public int compareTo1(Quad o) {
			if (dir != o.dir) throw new IllegalArgumentException("The other quad is not in the same direction: " + o.dir + " vs "+dir);
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
			if (dir != o.dir) throw new IllegalArgumentException("The other quad is not in the same direction: " + o.dir + " vs "+dir);
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
					EVENT_LOGGER.warn("Overlapping quads detected!");
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
					EVENT_LOGGER.warn("Overlapping quads detected!");
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
					EVENT_LOGGER.warn("Overlapping quads detected!");
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
					EVENT_LOGGER.warn("Overlapping quads detected!");
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
					EVENT_LOGGER.warn("Overlapping quads detected!");
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
					EVENT_LOGGER.warn("Overlapping quads detected!");
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

	final ArrayList<Quad>[] quads = new ArrayList[6];

	public LodQuadBuilder(int initialSize, boolean enableSkylightCulling, int skyLightCullingBelow) {
		for (int i=0; i<6; i++) quads[i] = new ArrayList<Quad>();
		this.skipSkylight0Quads = enableSkylightCulling;
		this.skyLightCullingBelow = (short) (skyLightCullingBelow - LodBuilder.MIN_WORLD_HEIGHT);
	}

	public void addQuadAdj(LodDirection dir, short x, short y, short z, short w0, short wy, int color, byte skylight,
			byte blocklight) {
		if (dir.ordinal() <= LodDirection.DOWN.ordinal())
			throw new IllegalArgumentException("addQuadAdj() is only for adj direction! Not UP or Down!");
		if (skipSkylight0Quads && skylight==0 && y < skyLightCullingBelow) return;
		quads[dir.ordinal()].add(new Quad(x, y, z, w0, wy, color, skylight, blocklight, dir));
	}

	// XZ
	public void addQuadUp(short x, short y, short z, short wx, short wz, int color, byte skylight, byte blocklight) {
		if (skipSkylight0Quads && skylight==0 && y < skyLightCullingBelow) return;
		quads[LodDirection.UP.ordinal()].add(new Quad(x, y, z, wx, wz, color, skylight, blocklight, LodDirection.UP));
	}

	public void addQuadDown(short x, short y, short z, short wx, short wz, int color, byte skylight, byte blocklight) {
		if (skipSkylight0Quads && skylight==0 && y < skyLightCullingBelow) return;
		quads[LodDirection.DOWN.ordinal()].add(new Quad(x, y, z, wx, wz, color, skylight, blocklight, LodDirection.DOWN));
	}

	// XY
	public void addQuadN(short x, short y, short z, short wx, short wy, int color, byte skylight, byte blocklight) {
		if (skipSkylight0Quads && skylight==0 && y < skyLightCullingBelow) return;
		quads[LodDirection.NORTH.ordinal()].add(new Quad(x, y, z, wx, wy, color, skylight, blocklight, LodDirection.NORTH));
	}

	public void addQuadS(short x, short y, short z, short wx, short wy, int color, byte skylight, byte blocklight) {
		if (skipSkylight0Quads && skylight==0 && y < skyLightCullingBelow) return;
		quads[LodDirection.SOUTH.ordinal()].add(new Quad(x, y, z, wx, wy, color, skylight, blocklight, LodDirection.SOUTH));
	}

	// ZY
	public void addQuadW(short x, short y, short z, short wz, short wy, int color, byte skylight, byte blocklight) {
		if (skipSkylight0Quads && skylight==0 && y < skyLightCullingBelow) return;
		quads[LodDirection.WEST.ordinal()].add(new Quad(x, y, z, wz, wy, color, skylight, blocklight, LodDirection.WEST));
	}

	public void addQuadE(short x, short y, short z, short wz, short wy, int color, byte skylight, byte blocklight) {
		if (skipSkylight0Quads && skylight==0 && y < skyLightCullingBelow) return;
		quads[LodDirection.EAST.ordinal()].add(new Quad(x, y, z, wz, wy, color, skylight, blocklight, LodDirection.EAST));
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
	
	public void sort(double dPlayerPosX, double dPlayerPosY, double dPlayerPosZ) {
		
	}

	private long mergeQuadsPass1(int dir) {
		if (quads[dir].size()<=1) return 0;
		quads[dir].sort(Quad::compareTo1);
		ListIterator<Quad> iter = quads[dir].listIterator();
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
		quads[dir].removeIf(o -> o==null);
		return mergeCount;
	}

	private long mergeQuadsPass2(int dir) {
		if (quads[dir].size()<=1) return 0;
		quads[dir].sort(Quad::compareTo2);
		ListIterator<Quad> iter = quads[dir].listIterator();
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
		quads[dir].removeIf(o -> o==null);
		return mergeCount;
	}
	
	

	public void mergeQuads() {
		long mergeCount = 0;
		long preQuadsCount = getCurrentQuadsCount();
		if (preQuadsCount<=1) return;
		for (int i=0; i<6; i++) {
			mergeCount += mergeQuadsPass1(i);
			if (i>=2) {
				continue;
				//long pass2 = mergeQuadsPass2(i);
				//mergeCount += pass2;
				//skipperMerge += pass2;
			} else {
				long pass2 = mergeQuadsPass2(i);
				mergeCount += pass2;
			}
		}
		long postQuadsCount = getCurrentQuadsCount();
		//if (mergeCount != 0)
		EVENT_LOGGER.debug("Merged {}/{}({}) quads", mergeCount, preQuadsCount, mergeCount/(double)preQuadsCount);
	}
	
	public Iterator<ByteBuffer> makeVertexBuffers() {
		return new Iterator<ByteBuffer>() {
			final ByteBuffer bb = ByteBuffer.allocateDirect(MAX_QUADS_PER_BUFFER * QUAD_BYTE_SIZE)
					.order(ByteOrder.nativeOrder());
			int dir = skipEmpty(0);
			int quad = 0;
			
			private int skipEmpty(int d) {
				while(d<6 && quads[d].isEmpty()) d++;
				return d;
			}
			
			@Override
			public boolean hasNext() {
				return dir < 6;
			}

			@Override
			public ByteBuffer next() {
				if (dir >= 6) {
					return null;
				}
				bb.clear();
				bb.limit(MAX_QUADS_PER_BUFFER * QUAD_BYTE_SIZE);
				while (bb.hasRemaining() && dir < 6) {
					writeData();
				}
				bb.limit(bb.position());
				bb.rewind();
				return bb;
			}

			private void writeData() {
				int startQ = quad;
				
				int i = startQ;
				for (i = startQ; i<quads[dir].size(); i++) {
					if (!bb.hasRemaining()) {
						break;
					}
					putQuad(bb, quads[dir].get(i));
				}
				
				if (i >= quads[dir].size()) {
					quad = 0;
					dir++;
					dir = skipEmpty(dir);
				} else {
					quad = i;
				}
			}
		};
	}
	
	public interface BufferFiller {
		boolean fill(LodVertexBuffer vbo); // If true: means more data is needed to be filled
	}

	public BufferFiller makeBufferFiller(GpuUploadMethod method) {
		return new BufferFiller() {
			int dir = 0;
			int quad = 0;
			public boolean fill(LodVertexBuffer vbo) {
				if (dir >= 6) {
					vbo.vertexCount = 0;
					return false;
				}
				
				int numOfQuads = _countRemainingQuads();
				if (numOfQuads > MAX_QUADS_PER_BUFFER) numOfQuads = MAX_QUADS_PER_BUFFER;
				if (numOfQuads == 0) {
					vbo.vertexCount = 0;
					return false;
				}
				ByteBuffer bb = vbo.mapBuffer(numOfQuads*QUAD_BYTE_SIZE, method, MAX_QUADS_PER_BUFFER * QUAD_BYTE_SIZE);
				if (bb == null) throw new NullPointerException("mapBuffer returned null");
				bb.clear();
				bb.limit(numOfQuads * QUAD_BYTE_SIZE);
				while (bb.hasRemaining() && dir < 6) {
					writeData(bb);
				}
				bb.rewind();
				vbo.unmapBuffer(method);
				vbo.vertexCount = numOfQuads*6;
				return dir < 6;
			}
			private int _countRemainingQuads() {
				int a = quads[dir].size() - quad;
				for (int i=dir+1; i<quads.length; i++) {
					a+=quads[i].size();
				}
				return a;
			}
			
			private void writeData(ByteBuffer bb) {
				int startQ = quad;
				
				int i = startQ;
				for (i = startQ; i<quads[dir].size(); i++) {
					if (!bb.hasRemaining()) {
						break;
					}
					putQuad(bb, quads[dir].get(i));
				}
				
				if (i >= quads[dir].size()) {
					quad = 0;
					dir++;
					while (dir<6 && quads[dir].isEmpty()) dir++;
				} else {
					quad = i;
				}
			}
		};
	}
	
	public int getCurrentQuadsCount() {
		int i = 0;
		for (ArrayList<Quad> qs : quads) i+=qs.size();
		return i;
	}

	public int getCurrentNeededVertexBuffers() {
		return LodUtil.ceilDiv(getCurrentQuadsCount(), MAX_QUADS_PER_BUFFER);
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
