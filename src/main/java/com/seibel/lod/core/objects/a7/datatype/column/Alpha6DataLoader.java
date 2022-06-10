package com.seibel.lod.core.objects.a7.datatype.column;

import com.seibel.lod.core.enums.config.EVerticalQuality;
import com.seibel.lod.core.objects.a7.DHLevel;
import com.seibel.lod.core.objects.a7.data.*;
import com.seibel.lod.core.objects.a7.pos.DhSectionPos;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Alpha6DataLoader extends OldDataSourceLoader implements OldFileConverter {

    public static final Alpha6DataLoader INSTANCE = new Alpha6DataLoader();

    private Alpha6DataLoader() {
        super(OldColumnDatatype.class, OldColumnDatatype.DATA_TYPE_ID, new byte[]{0});
        DataFileHandler.CONVERTERS.add(this);
    }

    @Override
    public LodDataSource loadData(DataFile dataFile, DHLevel level) {
        //TODO: Add decompressor here
        try (
                FileInputStream fin = dataFile.getDataContent();
                XZCompressorInputStream xzIn = new XZCompressorInputStream(fin);
                DataInputStream dis = new DataInputStream(xzIn);
        ) {
            return new OldColumnDatatype(dataFile.pos, dis, dataFile.loaderVersion, level, 1);
        } catch (IOException e) {
            //FIXME: Log error
            return null;
        }
    }

    @Override
    public DataSourceSaver getNewSaver() {
        return null; // No re-saving of old datatype as any data should be converted to new format before saving
    }




    private static DataFile convert(File file, int detailLevel, EVerticalQuality quality) {
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


            DhSectionPos pos;

            //TODO: Implement
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<DataFile> scanAndConvert(File levelFolder, DHLevel level) {

        List<DataFile> files = new ArrayList<>();

        List<File> foldersToScan = new ArrayList<>(EVerticalQuality.values().length);
        for (EVerticalQuality q : EVerticalQuality.values()) {
            File qualityFolder = new File(levelFolder, q.toString());
            for (int i = 0; i < 10; i++) {
                foldersToScan.add(new File(qualityFolder, "detail-"+i));
            }
        }

        for (EVerticalQuality q : EVerticalQuality.values()) {
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
