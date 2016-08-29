package com.shatam.data;

import java.io.FileInputStream;
import java.util.Observer;

public class USPSUtil
{
    public interface _USPSFileCallback
    {
        void callback(byte[] data) throws Exception;
    }

    /*public static final String USPS_ZIP4_DIR        ="C:\\ZIP4ANATL\\";//"D:\\SAC_WORKSPACE\\2015_ZIP4NATL\\";//"D:\\SAC_WORKSPACE\\ZIP4NATL__10\\";// "C:\\ZIP4ANATL\\";
    public static final String CITY_STATE_FILE_NAME = USPS_ZIP4_DIR + "ctystate\\ctystate.txt";*/
    
    public static final String USPS_ZIP4_DIR        ="D:/USPS_20160524/";//"D:\\SAC_WORKSPACE\\ZIP4NATL__10\\";// "C:\\ZIP4ANATL\\";
    public static final String CITY_STATE_FILE_NAME = USPS_ZIP4_DIR + "ctystate\\ctystate.txt";

    public static void readFile(String f, char recordType, int recSize, _USPSFileCallback callback) throws Exception
    {

        FileInputStream fstream = new FileInputStream(f);

        byte[] data = new byte[recSize];

        while (fstream.read(data) > 0)
        {
            if (data[0] != recordType)
                continue;

            callback.callback(data);

        }//while 
        fstream.close();

    }
}
