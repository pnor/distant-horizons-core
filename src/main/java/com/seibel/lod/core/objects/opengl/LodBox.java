package com.seibel.lod.core.objects.opengl;

import com.seibel.lod.core.enums.LodDirection;
import com.seibel.lod.core.util.ColorUtil;
import com.seibel.lod.core.util.DataPointUtil;
import com.seibel.lod.core.util.SingletonHandler;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftWrapper;

public class LodBox {
	private static final IMinecraftWrapper MC = SingletonHandler.get(IMinecraftWrapper.class);

	public static void addBoxQuadsToBuilder(LodQuadBuilder builder, short xSize, short ySize, short zSize, short x,
			short y, short z, int color, byte skyLight, byte blockLight, long topData, long botData, long[][] adjData) {
		short maxX = (short) (x + xSize);
		short maxY = (short) (y + ySize);
		short maxZ = (short) (z + zSize);
		byte skyLightTop = skyLight;
		byte skyLightBot = DataPointUtil.doesItExist(botData) ? DataPointUtil.getLightSky(botData) : 0;

		// Up direction case
		boolean skipTop = DataPointUtil.doesItExist(topData) && DataPointUtil.getDepth(topData) == maxY;// &&
																										// DataPointUtil.getAlpha(singleAdjDataPoint)
																										// == 255;
		boolean skipBot = DataPointUtil.doesItExist(botData) && DataPointUtil.getHeight(botData) == y;// &&
																										// DataPointUtil.getAlpha(singleAdjDataPoint)
																										// == 255;

		if (!skipTop)
			builder.addQuadUp(x, maxY, z, xSize, zSize, ColorUtil.applyShade(color, MC.getShade(LodDirection.UP)), skyLightTop, blockLight);
		if (!skipBot)
			builder.addQuadDown(x, y, z, xSize, zSize, ColorUtil.applyShade(color, MC.getShade(LodDirection.DOWN)), skyLightBot, blockLight);
		makeAdjQuads(builder, adjData[LodDirection.NORTH.ordinal() - 2], LodDirection.NORTH, x, y, z, xSize, ySize,
				color, skyLightTop, blockLight);
		makeAdjQuads(builder, adjData[LodDirection.SOUTH.ordinal() - 2], LodDirection.SOUTH, x, y, maxZ, xSize, ySize,
				color, skyLightTop, blockLight);
		makeAdjQuads(builder, adjData[LodDirection.WEST.ordinal() - 2], LodDirection.WEST, x, y, z, zSize, ySize, color,
				skyLightTop, blockLight);
		makeAdjQuads(builder, adjData[LodDirection.EAST.ordinal() - 2], LodDirection.EAST, maxX, y, z, zSize, ySize,
				color, skyLightTop, blockLight);
	}

	private static void makeAdjQuads(LodQuadBuilder builder, long[] adjData, LodDirection direction, short x, short y,
			short z, short w0, short wy, int color, byte upSkyLight, byte blockLight) {
		color = ColorUtil.applyShade(color, MC.getShade(direction));
		long[] dataPoint = adjData;
		if (dataPoint == null || DataPointUtil.isVoid(dataPoint[0])) {
			builder.addQuadAdj(direction, x, y, z, w0, wy, color, (byte) 15, blockLight);
			return;
		}

		int i;
		boolean firstFace = true;
		boolean allAbove = true;
		short nextStartingHeight = -1;
		byte nextSkyLight = upSkyLight;

		// TODO transparency ocean floor fix
		// boolean isOpaque = ((colorMap[0] >> 24) & 0xFF) == 255;
		for (i = 0; i < dataPoint.length && DataPointUtil.doesItExist(adjData[i])
				&& !DataPointUtil.isVoid(adjData[i]); i++) {
			long adjPoint = adjData[i];

			// TODO transparency ocean floor fix
			// if (isOpaque && DataPointUtil.getAlpha(singleAdjDataPoint) != 255)
			// continue;

			short height = DataPointUtil.getHeight(adjPoint);
			short depth = DataPointUtil.getDepth(adjPoint);

			// If the depth of said block is higher then our max Y, continue
			// Basically: y < maxY <= _____ height
			// _______&&: y < maxY <= depth
			if (y + wy <= depth)
				continue;
			// Now: depth < maxY
			allAbove = false;

			if (height < y) {
				// Basically: _____ height < y < maxY
				// _______&&: depth ______ < y < maxY
				if (firstFace) {
					builder.addQuadAdj(direction, x, y, z, w0, wy, color, DataPointUtil.getLightSky(adjPoint),
							blockLight);
				} else {
					// Now: depth < height < y < previousDepth < maxY
					if (nextStartingHeight == -1)
						throw new RuntimeException("Loop error");
					builder.addQuadAdj(direction, x, y, z, w0, (short) (nextStartingHeight - y), color,
							DataPointUtil.getLightSky(adjPoint), blockLight);
					nextStartingHeight = -1;
				}
				break;
			}

			if (depth <= y) { // AND y <= height
				if (y + wy <= height) {
					// Basically: ________ y < maxY <= height
					// _______&&: depth <= y < maxY
					// The face is inside adj face completely. Don't draw.
					break;
				}
				// Otherwise: ________ y <= Height < maxY
				// _______&&: depth <= y _________ < maxY
				// the adj data intersects the lower part of the current data
				// if this is the only face, use the maxY and break,
				// if there was another face we finish the last one and break
				if (firstFace) {
					builder.addQuadAdj(direction, x, height, z, w0, (short) (y + wy - height), color,
							DataPointUtil.getLightSky(adjPoint), blockLight);
				} else {
					// Now: depth <= y <= height < previousDepth < maxY
					if (nextStartingHeight == -1)
						throw new RuntimeException("Loop error");
					builder.addQuadAdj(direction, x, height, z, w0, (short) (nextStartingHeight - height), color,
							DataPointUtil.getLightSky(adjPoint), blockLight);
					nextStartingHeight = -1;
				}
				break;
			}

			// In here always true: y < depth < maxY
			// _________________&&: y < _____ (height and maxY)

			if (y + wy <= height) {
				// Basically: y _______ < maxY <= height
				// _______&&: y < depth < maxY
				// the adj data intersects the higher part of the current data
				// we start the creation of a new face
			} else {
				// Otherwise: y < _____ height < maxY
				// _______&&: y < depth ______ < maxY
				if (firstFace) {
					builder.addQuadAdj(direction, x, height, z, w0, (short) (y + wy - height), color,
							DataPointUtil.getLightSky(adjPoint), blockLight);
				} else {
					// Now: y < depth < height < previousDepth < maxY
					if (nextStartingHeight == -1)
						throw new RuntimeException("Loop error");
					builder.addQuadAdj(direction, x, height, z, w0, (short) (nextStartingHeight - height), color,
							DataPointUtil.getLightSky(adjPoint), blockLight);
					nextStartingHeight = -1;
				}
			}
			// set next top as current depth
			nextStartingHeight = depth;
			firstFace = false;
			nextSkyLight = upSkyLight;
			if (i + 1 < adjData.length && DataPointUtil.doesItExist(adjData[i + 1]))
				nextSkyLight = DataPointUtil.getLightSky(adjData[i + 1]);
		}

		if (allAbove) {
			builder.addQuadAdj(direction, x, y, z, w0, wy, color, upSkyLight, blockLight);
		} else if (nextStartingHeight != -1) {
			// We need to finish the last quad.
			builder.addQuadAdj(direction, x, y, z, w0, (short) (nextStartingHeight - y), color, nextSkyLight,
					blockLight);
		}
	}

}
