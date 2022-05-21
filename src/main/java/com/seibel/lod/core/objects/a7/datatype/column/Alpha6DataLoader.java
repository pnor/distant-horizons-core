package com.seibel.lod.core.objects.a7.datatype.column;

import com.seibel.lod.core.enums.config.VerticalQuality;
import com.seibel.lod.core.objects.a7.DHLevel;
import com.seibel.lod.core.objects.a7.data.DataFile;
import com.seibel.lod.core.objects.a7.data.DataFileHandler;
import com.seibel.lod.core.objects.a7.data.LodDataSource;
import com.seibel.lod.core.objects.a7.data.OldFileConverter;
import com.seibel.lod.core.objects.a7.pos.DhSectionPos;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class Alpha6DataLoader extends OldDataSourceLoader implements OldFileConverter {

    public Alpha6DataLoader() {
        super(ColumnDatatype.class, ColumnDatatype.DATA_TYPE_ID, (byte)0);
        DataFileHandler.CONVERTERS.add(this);
    }

    @Override
    public LodDataSource loadData(DHLevel level, DhSectionPos sectionPos, InputStream data) {
        return null;
    }

    @Override
    public DataSourceSaver getNewSaver() {
        return null;
    }

    private static DataFile convert(File file, int detailLevel, VerticalQuality quality) {
        String oldName = file.getName();
        String regionStr = oldName.substring("lod.".length(), oldName.length() - ".xz".length());
        String[] parts = regionStr.split("\\.");
        if (parts.length != 2) return null;
        int regionX = Integer.parseInt(parts[0]);
        int regionZ = Integer.parseInt(parts[1]);

        ColumnDatatype datatype;

        try (FileInputStream fileInStream = new FileInputStream(file)) {
            XZCompressorInputStream inputStream = new XZCompressorInputStream(fileInStream);
            int fileVersion = inputStream.read();

            datatype = new ColumnDatatype(fileVersion, quality, detailLevel);

            DhSectionPos pos;



            File newFilePath = ColumnDataLoader.INSTANCE.generateFilePathAndName(file,
                    detailLevel,
                    quality);



        } catch (Exception e) {
            e.printStackTrace();
        }
        int version




    }

    @Override
    public List<DataFile> scanAndConvert(File levelFolder, DHLevel level) {

        List<DataFile> files = new ArrayList<>();

        List<File> foldersToScan = new ArrayList<>(VerticalQuality.values().length);
        for (VerticalQuality q : VerticalQuality.values()) {
            File qualityFolder = new File(levelFolder, q.toString());
            for (int i = 0; i < 10; i++) {
                foldersToScan.add(new File(qualityFolder, "detail-"+i));
            }
        }

        for (VerticalQuality q : VerticalQuality.values()) {
            for (int i = 0; i < 10; i++) {
                File detailFolder = new File(levelFolder, q.toString() + File.pathSeparator + "detail-" + i);
                if (!detailFolder.exists() || !detailFolder.isDirectory()) continue;
                File[] filesToScan = detailFolder.listFiles();
                if (filesToScan == null) continue;
                for (File f : filesToScan) {
                    String fileName = f.getName();
                    if (!fileName.endsWith(".xz") || fileName.startsWith("lod.")) continue;
                    DataFile converted = convert(f, i, q);


                }
            }
        }

        return files;
    }
}
