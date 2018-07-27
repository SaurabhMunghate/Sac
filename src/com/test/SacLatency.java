package com.test;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

public class SacLatency {
	private SacLatency(){}
	
	public static void writeLatency(String time){
		try(Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("/home/glady/sac/25-April-2018/testcase/addresses/SAC_LATENCY_26_Apr.txt", true), "utf-8"));){
		    writer.write(time+"\n");
		    writer.flush();
		} catch (Exception ex) {}
	}
}
