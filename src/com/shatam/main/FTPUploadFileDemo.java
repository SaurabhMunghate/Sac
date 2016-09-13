/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

public class FTPUploadFileDemo {

	public static void main(String[] args) {
		String server = "ftp.digmap.com";
		int port = 21;
		String user = "***********";
		String pass = "***********";

		FTPClient ftpClient = new FTPClient();
		try {
			ftpClient.connect(server, port);
			ftpClient.login(user, pass);
			ftpClient.enterLocalPassiveMode();
			ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
			File firstLocalFile = new File("c:/cache/Test.txt");
			String firstRemoteFile = "Test.txt";
			InputStream inputStream = new FileInputStream(firstLocalFile);
			System.out.println("Start uploading first file");
			boolean done = ftpClient.storeFile(firstRemoteFile, inputStream);
			inputStream.close();
			if (done) {
				System.out.println("The first file is uploaded successfully.");
			}
			File secondLocalFile = new File("c:/cache/Test.txt");
			String secondRemoteFile = "BuildersData/Test.txt";
			inputStream = new FileInputStream(secondLocalFile);
			System.out.println("Start uploading second file");
			OutputStream outputStream = ftpClient
					.storeFileStream(secondRemoteFile);
			byte[] bytesIn = new byte[4096];
			int read = 0;
			while ((read = inputStream.read(bytesIn)) != -1) {
				outputStream.write(bytesIn, 0, read);
			}
			inputStream.close();
			outputStream.close();

			boolean completed = ftpClient.completePendingCommand();
			if (completed) {
				System.out.println("The second file is uploaded successfully.");
			}
		} catch (IOException ex) {
			System.out.println("Error: " + ex.getMessage());
			ex.printStackTrace();
		} finally {
			try {
				if (ftpClient.isConnected()) {
					ftpClient.logout();
					ftpClient.disconnect();
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}

}