package com.data.search;

import java.io.*;


public class FileUtil {

    public static void makeDir(String path){
        File folder = new File(path);
        if (!folder.exists())
            folder.mkdirs();
    }

    public static String readAllText(String path) throws IOException {

        File aFile = new File(path);

        StringBuilder contents = new StringBuilder();


        BufferedReader input = new BufferedReader(new FileReader(aFile));
        try {
            String line = null; //not declared within while loop

            while ((line = input.readLine()) != null) {
                contents.append(line);
                contents.append(System.getProperty("line.separator"));
            }
        } finally {
            input.close();
        }


        return contents.toString();
        
    }//readAllText()


    public static void writeAllText(String path, String aContents) throws FileNotFoundException, IOException {

        File aFile = new File(path);

        //use buffering
        Writer output = new BufferedWriter(new FileWriter(aFile));
        try {
            //FileWriter always assumes default encoding is OK!
            output.write(aContents);
        } finally {
            output.close();
        }
    }//writeAllText()

}//class FileUtil
