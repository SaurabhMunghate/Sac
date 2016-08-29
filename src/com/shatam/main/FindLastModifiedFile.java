package com.shatam.main;

import org.apache.commons.io.comparator.LastModifiedFileComparator;
 
import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
 
public class FindLastModifiedFile {
    public static void main(String[] args) throws UnknownHostException {
        File dir = new File("C:\\SAC\\LOG\\");
        File[] files = dir.listFiles();
      /*  InetAddress IP=InetAddress.getLocalHost();
        System.out.println("IP of my system is := "+IP.getHostAddress());
        Arrays.sort(files, LastModifiedFileComparator.LASTMODIFIED_COMPARATOR);
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            System.out.println(file.getName());
        }
 System.out.println("==========================================================================");
 */
        Arrays.sort(files, LastModifiedFileComparator.LASTMODIFIED_REVERSE);
        System.out.println("**************"+files[0]);
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            System.out.printf("File %s - %2$tm %2$te,%2$tY%n= ", file.getName(),
                    file.lastModified());
        }
    }
}
