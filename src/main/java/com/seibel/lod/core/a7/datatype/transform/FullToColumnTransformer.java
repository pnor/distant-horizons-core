package com.seibel.lod.core.a7.datatype.transform;

import com.seibel.lod.core.a7.datatype.column.ColumnFormat;
import com.seibel.lod.core.a7.datatype.column.ColumnRenderSource;
import com.seibel.lod.core.a7.datatype.column.accessor.ColumnArrayView;
import com.seibel.lod.core.a7.datatype.full.FullDataSource;
import com.seibel.lod.core.a7.datatype.full.FullFormat;
import com.seibel.lod.core.a7.datatype.full.IdBiomeBlockStateMap;
import com.seibel.lod.core.a7.datatype.full.accessor.SingleFullArrayView;
import com.seibel.lod.core.a7.level.IClientLevel;
import com.seibel.lod.core.a7.pos.DhSectionPos;
import com.seibel.lod.core.config.Config;
import com.seibel.lod.core.wrapperInterfaces.block.IBlockStateWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IBiomeWrapper;

public class FullToColumnTransformer {

    /**
     * Creates a LodNode for a chunk in the given world.
     * @throws IllegalArgumentException thrown if either the chunk or world is null.
     */

    public static ColumnRenderSource transformFullDataToColumnData(IClientLevel level, FullDataSource data) {
        final DhSectionPos pos = data.getSectionPos();
        final byte dataDetail = data.getDataDetail();
        final int vertSize = Config.Client.Graphics.Quality.verticalQuality.get().calculateMaxVerticalData(data.getDataDetail());
        final ColumnRenderSource columnSource = new ColumnRenderSource(pos, vertSize, level.getMinY());

        if (dataDetail == pos.sectionDetail- columnSource.getDataDetail()) {
            for (int x = 0; x < pos.getWidth(dataDetail).value; x++) {
                for (int z = 0; z < pos.getWidth(dataDetail).value; z++) {
                    ColumnArrayView columnArrayView = columnSource.getVerticalDataView(x, z);
                    SingleFullArrayView fullArrayView = data.get(x, z);
                    convertColumnData(level, columnArrayView, fullArrayView);
                }
            }
        } else {
            throw new UnsupportedOperationException("To be implemented");
            //FIXME: Implement different size creation of renderData
        }


        return columnSource;
    }

    private static void convertColumnData(IClientLevel level, ColumnArrayView columnArrayView, SingleFullArrayView fullArrayView) {
        if (!fullArrayView.doesItExist()) return;
        // TODO: Set gen mode
        int genModeValue = 0;
        int dataTotalLength = fullArrayView.getSingleLength();
        if (dataTotalLength == 0) return;

        if (dataTotalLength > columnArrayView.verticalSize()) {
            ColumnArrayView totalColumnData = new ColumnArrayView(new long[dataTotalLength], dataTotalLength, 0, dataTotalLength);
            iterateAndConvert(level, genModeValue, totalColumnData, fullArrayView);
            columnArrayView.changeVerticalSizeFrom(totalColumnData);
        } else {
            iterateAndConvert(level, genModeValue, columnArrayView, fullArrayView); //Directly use the arrayView since it fits.
        }
    }

    private static void iterateAndConvert(IClientLevel level, int genMode, ColumnArrayView column, SingleFullArrayView data) {
        IdBiomeBlockStateMap mapping = data.getMapping();
        for (int i = 0; i < data.getSingleLength(); i++) {
            long fullData = data.getSingle(i);
            int y = FullFormat.getY(fullData);
            int depth = FullFormat.getDepth(fullData);
            int id = FullFormat.getId(fullData);
            byte light = FullFormat.getLight(fullData);
            IdBiomeBlockStateMap.Entry entry = mapping.get(id);
            IBiomeWrapper biome = entry.biome;
            IBlockStateWrapper block = entry.blockState;
            int color = level.computeBaseColor(biome, block);
            long columnData = ColumnFormat.createDataPoint(y, depth, color, light, genMode);
            column.set(i, columnData);
        }
    }

//
//
//    /** creates a vertical DataPoint */
//    private void writeVerticalData(long[] data, int dataOffset, int maxVerticalData,
//                                   IChunkWrapper chunk, LodBuilderConfig config, int chunkSubPosX, int chunkSubPosZ)
//    {
//
//        int totalVerticalData = (chunk.getHeight());
//        long[] dataToMerge = new long[totalVerticalData];
//
//        boolean hasCeiling = MC.getWrappedClientWorld().getDimensionType().hasCeiling();
//        boolean hasSkyLight = MC.getWrappedClientWorld().getDimensionType().hasSkyLight();
//        byte generation = config.distanceGenerationMode.complexity;
//        int count = 0;
//        // FIXME: This yAbs is just messy!
//        int x = chunk.getMinX() + chunkSubPosX;
//        int z = chunk.getMinZ() + chunkSubPosZ;
//        int y = chunk.getMaxY(x, z);
//
//        boolean topBlock = true;
//        if (y < chunk.getMinBuildHeight())
//            dataToMerge[0] = DataPointUtil.createVoidDataPoint(generation);
//        int maxConnectedLods = Config.Client.Graphics.Quality.verticalQuality.get().maxVerticalData[0];
//        while (y >= chunk.getMinBuildHeight()) {
//            int height = determineHeightPointFrom(chunk, config, x, y, z);
//            // If the lod is at the default height, it must be void data
//            if (height < chunk.getMinBuildHeight()) {
//                if (topBlock) dataToMerge[0] = DataPointUtil.createVoidDataPoint(generation);
//                break;
//            }
//            y = height - 1;
//            // We search light on above air block
//            int depth = determineBottomPointFrom(chunk, config, x, y, z,
//                    count < maxConnectedLods && (!hasCeiling || !topBlock));
//            if (hasCeiling && topBlock)
//                y = depth;
//            int light = getLightValue(chunk, x, y, z, hasCeiling, hasSkyLight, topBlock);
//            int color = generateLodColor(chunk, config, x, y, z);
//            int lightBlock = light & 0b1111;
//            int lightSky = (light >> 4) & 0b1111;
//            dataToMerge[count] = DataPointUtil.createDataPoint(height-chunk.getMinBuildHeight(), depth-chunk.getMinBuildHeight(),
//                    color, lightSky, lightBlock, generation);
//            topBlock = false;
//            y = depth - 1;
//            count++;
//        }
//        long[] result = DataPointUtil.mergeMultiData(dataToMerge, totalVerticalData, maxVerticalData);
//        if (result.length != maxVerticalData) throw new ArrayIndexOutOfBoundsException();
//        System.arraycopy(result, 0, data, dataOffset, maxVerticalData);
//    }
//
//    public static final ELodDirection[] DIRECTIONS = new ELodDirection[] {
//            ELodDirection.UP,
//            ELodDirection.DOWN,
//            ELodDirection.WEST,
//            ELodDirection.EAST,
//            ELodDirection.NORTH,
//            ELodDirection.SOUTH };
//
//    private boolean hasCliffFace(IChunkWrapper chunk, int x, int y, int z) {
//        for (ELodDirection dir : DIRECTIONS) {
//            IBlockDetailWrapper block = chunk.getBlockDetailAtFace(x, y, z, dir);
//            if (block == null || !block.hasFaceCullingFor(ELodDirection.OPPOSITE_DIRECTIONS[dir.ordinal()]))
//                return true;
//        }
//        return false;
//    }
//
//    /**
//     * Find the lowest valid point from the bottom.
//     * Used when creating a vertical LOD.
//     */
//    private int determineBottomPointFrom(IChunkWrapper chunk, LodBuilderConfig builderConfig, int xAbs, int yAbs, int zAbs, boolean strictEdge)
//    {
//        int depth = chunk.getMinBuildHeight();
//        IBlockDetailWrapper currentBlockDetail = null;
//        if (strictEdge)
//        {
//            IBlockDetailWrapper blockAbove = chunk.getBlockDetail(xAbs, yAbs + 1, zAbs);
//            if (blockAbove != null && Config.Client.WorldGenerator.tintWithAvoidedBlocks.get() && !blockAbove.shouldRender(Config.Client.WorldGenerator.blocksToAvoid.get()))
//            { // The above block is skipped. Lets use its skipped color for current block
//                currentBlockDetail = blockAbove;
//            }
//            if (currentBlockDetail == null) currentBlockDetail = chunk.getBlockDetail(xAbs, yAbs, zAbs);
//        }
//
//        for (int y = yAbs - 1; y >= chunk.getMinBuildHeight(); y--)
//        {
//            IBlockDetailWrapper nextBlock = chunk.getBlockDetail(xAbs, y, zAbs);
//            if (isLayerValidLodPoint(nextBlock)) {
//                if (!strictEdge) continue;
//                if (currentBlockDetail.equals(nextBlock)) continue;
//                if (!hasCliffFace(chunk, xAbs, y, zAbs)) continue;
//            }
//            depth = (y + 1);
//            break;
//        }
//        return depth;
//    }
//
//    /** Find the highest valid point from the Top */
//    private int determineHeightPointFrom(IChunkWrapper chunk, LodBuilderConfig config, int xAbs, int yAbs, int zAbs)
//    {
//        //TODO find a way to skip bottom of the world
//        int height = chunk.getMinBuildHeight()-1;
//        for (int y = yAbs; y >= chunk.getMinBuildHeight(); y--)
//        {
//            if (isLayerValidLodPoint(chunk, xAbs, y, zAbs))
//            {
//                height = (y + 1);
//                break;
//            }
//        }
//        return height;
//    }
//
//
//
//    // =====================//
//    // constructor helpers //
//    // =====================//
//
//    /**
//     * Generate the color for the given chunk using biome water color, foliage
//     * color, and grass color.
//     */
//    private int generateLodColor(IChunkWrapper chunk, LodBuilderConfig builderConfig, int x, int y, int z)
//    {
//        int colorInt;
//        if (builderConfig.useBiomeColors)
//        {
//            // I have no idea why I need to bit shift to the right, but
//            // if I don't the biomes don't show up correctly.
//            colorInt = chunk.getBiome(x, y, z).getColorForBiome(x, z);
//        }
//        else
//        {
//            // if we are skipping non-full and non-solid blocks that means we ignore
//            // snow, flowers, etc. Get the above block so we can still get the color
//            // of the snow, flower, etc. that may be above this block
//            colorInt = 0;
//            if (chunk.blockPosInsideChunk(x, y+1, z)) {
//                IBlockDetailWrapper blockAbove = chunk.getBlockDetail(x, y+1, z);
//                if (blockAbove != null && Config.Client.WorldGenerator.tintWithAvoidedBlocks.get() && !blockAbove.shouldRender(Config.Client.WorldGenerator.blocksToAvoid.get()))
//                {  // The above block is skipped. Lets use its skipped color for current block
//                    colorInt = blockAbove.getAndResolveFaceColor(null, chunk, new DHBlockPos(x, y+1, z));
//                }
//            }
//
//            // override this block's color if there was a block above this
//            // and we were avoiding non-full/non-solid blocks
//            if (colorInt == 0) {
//                IBlockDetailWrapper detail = chunk.getBlockDetail(x, y, z);
//                colorInt = detail.getAndResolveFaceColor(null, chunk, new DHBlockPos(x, y, z));
//            }
//        }
//
//        return colorInt;
//    }
//
//    /** Gets the light value for the given block position */
//    private int getLightValue(IChunkWrapper chunk, int x, int y, int z, boolean hasCeiling, boolean hasSkyLight, boolean topBlock)
//    {
//        int skyLight;
//        int blockLight;
//
//        int blockBrightness = chunk.getEmittedBrightness(x, y, z);
//        // get the air block above or below this block
//        if (hasCeiling && topBlock)
//            y--;
//        else
//            y++;
//
//        blockLight = chunk.getBlockLight(x, y, z);
//        skyLight = hasSkyLight ? chunk.getSkyLight(x, y, z) : 0;
//
//        if (blockLight == -1 || skyLight == -1)
//        {
//
//            ILevelWrapper world = MC.getWrappedServerWorld();
//
//            if (world != null)
//            {
//                // server world sky light (always accurate)
//                blockLight = world.getBlockLight(x, y, z);
//
//                if (topBlock && !hasCeiling && hasSkyLight)
//                    skyLight = DEFAULT_MAX_LIGHT;
//                else
//                    skyLight = hasSkyLight ? world.getSkyLight(x, y, z) : 0;
//
//                if (!topBlock && skyLight == 15)
//                {
//                    // we are on predicted terrain, and we don't know what the light here is,
//                    // lets just take a guess
//                    skyLight = 12;
//                }
//            }
//            else
//            {
//                world = MC.getWrappedClientWorld();
//                if (world == null)
//                {
//                    blockLight = 0;
//                    skyLight = 12;
//                }
//                else
//                {
//                    // client world sky light (almost never accurate)
//                    blockLight = world.getBlockLight(x, y, z);
//                    // estimate what the lighting should be
//                    if (hasSkyLight || !hasCeiling)
//                    {
//                        if (topBlock)
//                            skyLight = DEFAULT_MAX_LIGHT;
//                        else
//                        {
//                            if (hasSkyLight)
//                                skyLight = world.getSkyLight(x, y, z);
//                            //else
//                            //	skyLight = 0;
//                            if (!chunk.isLightCorrect() && (skyLight == 0 || skyLight == 15))
//                            {
//                                // we don't know what the light here is,
//                                // lets just take a guess
//                                skyLight = 12;
//                            }
//                        }
//                    }
//                }
//            }
//        }
//
//        blockLight = LodUtil.clamp(0, Math.max(blockLight, blockBrightness), DEFAULT_MAX_LIGHT);
//        return blockLight + (skyLight << 4);
//    }
//
//    /** Is the block at the given blockPos a valid LOD point? */
//    private boolean isLayerValidLodPoint(IBlockDetailWrapper blockDetail)
//    {
//        EBlocksToAvoid avoid = Config.Client.WorldGenerator.blocksToAvoid.get();
//        return blockDetail != null && blockDetail.shouldRender(avoid);
//    }
//
//    /** Is the block at the given blockPos a valid LOD point? */
//    private boolean isLayerValidLodPoint(IChunkWrapper chunk, int x, int y, int z) {
//        EBlocksToAvoid avoid = Config.Client.WorldGenerator.blocksToAvoid.get();
//        IBlockDetailWrapper block = chunk.getBlockDetail(x, y, z);
//        return block != null && block.shouldRender(avoid);
//    }
}
