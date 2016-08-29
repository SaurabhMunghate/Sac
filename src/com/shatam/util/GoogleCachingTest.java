package com.shatam.util;

import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.shatam.model.AddressStruct;


public class GoogleCachingTest {

	/**
	 * @param args
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	//static LoadingCache<ShatamIndexQueryStruct, AddressStruct> addressMap = null;
	static HashMap<String, AddressStruct> addressMap = null;
	public static void newBuilder() {

//		if (addressMap == null) {
//			addressMap = CacheBuilder.newBuilder().maximumSize(100000)
//					.expireAfterWrite(10, TimeUnit.MINUTES)
//					.build(new CacheLoader<ShatamIndexQueryStruct, AddressStruct>() {
//
//						@Override
//						public AddressStruct load(ShatamIndexQueryStruct arg0)
//								throws Exception {
//							// TODO Auto-generated method stub
//							return null;
//						}
//						
//					});
//		}
		if (addressMap == null) {
			addressMap = new HashMap<>();
		}
	//CacheBuilder<Object, Object> cach	  = CacheBuilder.newBuilder();
	//Cach = cach.build();
	}

	public static void cleanup(){
		
		addressMap = null;
	}
	
	public static void put(String key, AddressStruct addressStruct) {

		addressMap.put(key, addressStruct);
	}
	
	public static long size(){
		if(addressMap==null){
			return -1;
		}
		return addressMap.size();
	}
	
	public static String k1_reference = null;
	
	public static AddressStruct get(String key) throws ExecutionException{
	return addressMap.get(key);
		
	}

	
	public static void main(String[] args) throws ExecutionException,
			InterruptedException {
		// TODO Auto-generated method stub

		long start = System.currentTimeMillis();

		//LoadingCache<String, String> addressMap =

		//addressMap.put("rakesh", "chaudhari");
		//addressMap.put("rakesh1", "chaudhari1");
		//String name = addressMap.get("rakesh");
		// Thread.sleep(1000);
				
		long end = System.currentTimeMillis();

		//System.out.println(addressMap.get("rakesh1"));
	//	System.out.println(end - start);

	}

}
