package com.shatam.util;

import java.io.*;
import java.util.ArrayList;
import java.util.zip.*;

public class UnZip
{
    static final int BUFFER = 2048;

    public static ArrayList<File> extract(File zipFile, String extractFileType) throws IOException
    {

        ArrayList<File> list = new ArrayList<File>();

        BufferedOutputStream dest = null;
        FileInputStream fis = new FileInputStream(zipFile);
        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
        ZipEntry entry;
        while ((entry = getNextEntry(zis)) != null)
        {

            if (extractFileType != null)
            {

                if (!entry.getName().toLowerCase().endsWith(extractFileType))
                    continue;
            }

            File outputFile = new File(zipFile.getParent(), entry.getName());

            if (!outputFile.exists())
            {

                System.out.println("Extracting: " + entry);
                int count;
                byte data[] = new byte[BUFFER];
                // write the files to the disk
                FileOutputStream fos = new FileOutputStream(outputFile);
                dest = new BufferedOutputStream(fos, BUFFER);

                while ((count = zis.read(data, 0, BUFFER)) != -1)
                {
                    dest.write(data, 0, count);
                }

                dest.flush();
                dest.close();
            }

            list.add(outputFile);
        }
        zis.close();

        return list;

    }



    private static ZipEntry getNextEntry(ZipInputStream zis)
    {
        try
        {
            return zis.getNextEntry();
        } catch (Exception ex)
        {
            //U.log("Failed: " + ex.toString());
            //U.log("Trying again");
            //return getNextEntry(zis);
            return null;
        }

    }
}