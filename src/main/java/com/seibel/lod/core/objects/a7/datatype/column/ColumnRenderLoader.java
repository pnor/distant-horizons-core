package com.seibel.lod.core.objects.a7.datatype.column;

import com.seibel.lod.core.objects.a7.DHLevel;
import com.seibel.lod.core.objects.a7.data.DataFile;
import com.seibel.lod.core.objects.a7.data.LodDataSource;
import com.seibel.lod.core.objects.a7.datatype.full.FullDatatype;
import com.seibel.lod.core.objects.a7.pos.DhSectionPos;
import com.seibel.lod.core.objects.a7.render.RenderDataSource;
import com.seibel.lod.core.objects.a7.render.RenderDataSourceLoader;
import com.seibel.lod.core.util.DetailDistanceUtil;
import com.seibel.lod.core.util.LodUtil;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

class ColumnRenderLoader extends RenderDataSourceLoader  {
    public ColumnRenderLoader() {
        super(4);
    }

    @Override
    public RenderDataSource construct(List<LodDataSource> dataSources, DhSectionPos sectionPos, DHLevel level) {
        if (dataSources.size() == 0) return null;

        // Check for direct casting
        if (dataSources.size() == 1 && dataSources.get(0) instanceof ColumnDatatype
                && dataSources.get(0).getSectionPos().equals(sectionPos)
                && dataSources.get(0).getDataDetail() == sectionPos.sectionDetail - ColumnDatatype.SECTION_SIZE_OFFSET) {
            // Directly using the data source as the render data source is possible.
            return (ColumnDatatype) dataSources.get(0);
        }

        // Otherwise, we need to create a new render data source, and copy the data from the data sources.
        ColumnDatatype renderDataSource = new ColumnDatatype(sectionPos,
                DetailDistanceUtil.getMaxVerticalData(sectionPos.sectionDetail - ColumnDatatype.SECTION_SIZE_OFFSET),
                level.getMinY());
        boolean completeCopy = dataSources.get(0).getSectionPos().getWidth().toBlock() >= sectionPos.getWidth().toBlock();

        if (completeCopy) {
            // If there is only one data source, we need to insure on copy, we don't copy out of bounds as we
            //  may just need to copy partial section of the data source.
            LodUtil.assertTrue(dataSources.size() == 1, "Expected only one data source for complete copy");
            byte targetDataLevel = (byte) (sectionPos.sectionDetail - ColumnDatatype.SECTION_SIZE_OFFSET);
            byte sourceDataLevel = dataSources.get(0).getDataDetail();
            LodUtil.assertTrue(targetDataLevel >= sourceDataLevel);
            if (dataSources.get(0) instanceof IColumnDatatype) {
                IColumnDatatype dataSource = (IColumnDatatype) dataSources.get(0);
                DhSectionPos srcPos = dataSource.getSectionPos();

                // Note that in here, the source data level will be always < target section level
                int trgX = sectionPos.getCorner().getX().toBlock();
                int trgZ = sectionPos.getCorner().getZ().toBlock();
                int trgMaxX = trgX + sectionPos.getWidth().toBlock() - 1;
                int trgMaxZ = trgZ + sectionPos.getWidth().toBlock() - 1;
                int trgXSizeInSrc = (trgX >> sourceDataLevel) - (trgMaxX >> sourceDataLevel) + 1;
                int trgZSizeInSrc = (trgZ >> sourceDataLevel) - (trgMaxZ >> sourceDataLevel) + 1;
                int trgXInSrc = (trgX >> sourceDataLevel) % srcPos.getWidth(sourceDataLevel).value;
                int trgZInSrc = (trgZ >> sourceDataLevel) % srcPos.getWidth(sourceDataLevel).value;

                ColumnQuadView srcView = dataSource.getDataInQuad(trgXInSrc, trgZInSrc, trgXSizeInSrc, trgZSizeInSrc);
                ColumnQuadView trgView = renderDataSource.getFullQuad();
                trgView.mergeMultiColumnFrom(srcView);
            } else {
                if (!(dataSources.get(0) instanceof FullDatatype))
                    throw new IllegalArgumentException("Unsupported data source type: " + dataSources.get(0).getClass().getName());
                FullDatatype dataSource = (FullDatatype) dataSources.get(0);
                DhSectionPos srcPos = dataSource.getSectionPos();
                //TODO: Impl this
                LodUtil.assertTrue(false, "Not implemented yet");
            }
        } else {
            // If there are multiple data sources, we need to merge them into the target data source
            for (LodDataSource dataSource : dataSources) {
                byte targetDataLevel = (byte) (sectionPos.sectionDetail - ColumnDatatype.SECTION_SIZE_OFFSET);
                byte sourceDataLevel = dataSource.getDataDetail();
                DhSectionPos srcPos = dataSource.getSectionPos();

                if (dataSource instanceof IColumnDatatype) {
                    IColumnDatatype clDataSource = (IColumnDatatype) dataSource;

                    // Note that targetDataLevel can be > source section level
                    int srcX = srcPos.getCorner().getX().toBlock();
                    int srcZ = srcPos.getCorner().getZ().toBlock();
                    int srcMaxX = srcX + srcPos.getWidth().toBlock() - 1;
                    int srcMaxZ = srcZ + srcPos.getWidth().toBlock() - 1;
                    int srcXSizeInTrg = (srcX >> targetDataLevel) - (srcMaxX >> targetDataLevel) + 1;
                    int srcZSizeInTrg = (srcZ >> targetDataLevel) - (srcMaxZ >> targetDataLevel) + 1;
                    int srcXInTrg = (srcX >> targetDataLevel) % ColumnDatatype.SECTION_SIZE;
                    int srcZInTrg = (srcZ >> targetDataLevel) % ColumnDatatype.SECTION_SIZE;

                    ColumnQuadView srcView = clDataSource.getFullQuad();
                    ColumnQuadView trgView = renderDataSource.getDataInQuad(srcXInTrg, srcZInTrg, srcXSizeInTrg, srcZSizeInTrg);
                    trgView.mergeMultiColumnFrom(srcView);
                } else {
                    if (!(dataSource instanceof FullDatatype))
                        throw new IllegalArgumentException("Unsupported data source type: " + dataSource.getClass().getName());
                    FullDatatype flDataSource = (FullDatatype) dataSource;
                    //TODO: Impl this
                    LodUtil.assertTrue(false, "Not implemented yet");
                }
            }
        }

        return renderDataSource;
    }

    private static boolean IsColumnDatatype(Class<?> clazz) {
        return IColumnDatatype.class.isAssignableFrom(clazz);
    }

    @Override
    public List<DataFile> selectFiles(DhSectionPos sectionPos, DHLevel level, List<DataFile>[] availableFiles) {
        byte targetDataLevel = (byte) (sectionPos.sectionDetail - ColumnDatatype.SECTION_SIZE_OFFSET);
        //No support for loading higher than the target level yet.
        byte maxDataLevel = LodUtil.min((byte) (availableFiles.length - 1), targetDataLevel);
        byte topValidDataLevel = Byte.MIN_VALUE;
        List<DataFile> selectedFiles = new LinkedList<>();

        for (int detail = maxDataLevel; detail >= 0; detail--) {
            if (availableFiles[detail] == null) continue;
            if (topValidDataLevel == Byte.MIN_VALUE) {
                for (DataFile dataFile : availableFiles[detail]) {
                    if (dataFile.dataLevel > targetDataLevel) continue;
                    if (IsColumnDatatype(dataFile.dataType) || dataFile.dataType == FullDatatype.class
                            || dataFile.dataType == OldColumnDatatype.class) {
                        topValidDataLevel = LodUtil.max(topValidDataLevel, dataFile.dataLevel);
                        break;
                    }
                }
            }
            if (topValidDataLevel == Byte.MIN_VALUE) continue;


            DataFile singleCoveringColumnFile = null;
            DataFile singleCoveringFullFile = null;

            for (DataFile dataFile : availableFiles[detail]) {
                if (dataFile.pos.getWidth().toBlock() == sectionPos.getWidth().toBlock()) {
                    if (IsColumnDatatype(dataFile.dataType)) {
                        singleCoveringColumnFile = dataFile;
                        break;
                    } else if (dataFile.dataType == FullDatatype.class) {
                        singleCoveringFullFile = dataFile;
                        // Don't break as there may be a column file later.
                    }
                } else if (dataFile.pos.getWidth().toBlock() > sectionPos.getWidth().toBlock()) {
                    if (IsColumnDatatype(dataFile.dataType) && singleCoveringColumnFile == null)
                        singleCoveringColumnFile = dataFile;
                    else if (dataFile.dataType == FullDatatype.class && singleCoveringFullFile == null)
                        singleCoveringFullFile = dataFile;
                }
            }

            // First, try select single file that has enough width to cover the section
            if (singleCoveringColumnFile != null) return Collections.singletonList(singleCoveringColumnFile);
            if (singleCoveringFullFile != null) return Collections.singletonList(singleCoveringFullFile);

            // If no single file covers the section, try to select all files without any duplicates
            for (DataFile dataFile : availableFiles[detail]) {
                boolean isDuplicate = false;
                boolean isSet = false;
                for (int i = 0; i < selectedFiles.size(); i++) {
                    DataFile selectedFile = selectedFiles.get(i);
                    if (selectedFile == null) continue;
                    if (selectedFile.pos.overlaps(dataFile.pos)) {
                        // Now, the already selected file muct have same or higher data level
                        //  so, we just select the file with a position that covers the most area.
                        // Therefore, we choose the file with the higher section level.
                        if (selectedFile.pos.sectionDetail < dataFile.pos.sectionDetail) {
                            if (isSet) selectedFiles.set(i, null);
                            else selectedFiles.set(i, dataFile);
                            isSet = true;
                        } else {
                            LodUtil.assertTrue(!isSet); // We should not have encountered a smaller section level.
                            // This mean its completely covered by the selected file, so we can skip it.
                            isDuplicate = true;
                            break;
                        }
                    }
                }
                if (!isDuplicate && !isSet) selectedFiles.add(dataFile);
            }
        }
        if (topValidDataLevel == Byte.MIN_VALUE) return Collections.emptyList();
        selectedFiles.removeIf(Objects::isNull);
        return selectedFiles;
    }
}
