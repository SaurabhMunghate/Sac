package com.shatam.util;

import java.io.File;

public class Paths
{

    public static String       DATA_ROOT      = "C:\\SAC_Raw_Files\\";
    public static String       SETTINGS_PATH  = "C:\\SAC_Raw_Files\\settings\\";
    //public static final String SQLITE_ROOT    = DATA_ROOT+"sqlite\\";
    //public static final String ZIP_SHAPE_PATH = DATA_ROOT+"zip\\tl_2010_us_zcta510.shp";

    
   // public static String DATA_ROOT      = null;
   //public static String SETTINGS_PATH  = null;
    public static String SQLITE_ROOT    = null;
    public static String ZIP_SHAPE_PATH = null;
    public static String LOG_PATH = null;
    public static String READ_DATA= null;
    static
    {
        DATA_ROOT = System.getProperty("user.dir");
        LOG_PATH=new File(DATA_ROOT).getParent();
        LOG_PATH=combine(LOG_PATH, "LOG");
        DATA_ROOT = new File(DATA_ROOT).getParent();
        DATA_ROOT = combine(DATA_ROOT, "Data");
        
       // SETTINGS_PATH = combine(DATA_ROOT, "settings");
        SQLITE_ROOT = combine(DATA_ROOT, "sqlite");
        ZIP_SHAPE_PATH = combine(DATA_ROOT, "zip", "tl_2010_us_zcta510.shp");
       
        
        
    }
    

    public static String combine(String p1, String p2, String p3)
    {

        return combine(combine(p1, p2), p3);
    }

    public static String combine(String path1, String path2)
    {
        File file1 = new File(path1);
        File file2 = new File(file1, path2);
        return file2.getPath();
    }
}
