package com.shatam.geo;

import java.io.File;
import java.io.FileInputStream;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.Observer;

import org.geotools.data.shapefile.dbf.DbaseFileReader;

import com.shatam.util.U;

public class ReadDbfFile
{

    public static void iterate(File file, Observer observer) throws Exception
    {

        FileChannel in = new FileInputStream(file).getChannel();
        DbaseFileReader r = new DbaseFileReader(in, false, Charset.defaultCharset());
        // int fields = r.getHeader().getNumFields();

        while (r.hasNext())
        {
        	//try{
            Object[] objRow = r.readEntry();
            observer.update(null, objRow);
//        	}
//            catch(Exception e){
//            	U.log("SIZE ERROR");
//            }
        }

        r.close();


    }
}
