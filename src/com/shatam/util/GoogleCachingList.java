package com.shatam.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.shatam.model.AddressStruct;


public class GoogleCachingList {

	/**
	 * @param args
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	//static LoadingCache<ShatamIndexQueryStruct, AddressStruct> addressMap = null;
	static HashMap<String, List<AddressStruct>> addressMap = null;
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
	
	public static void put(String key, List<AddressStruct> addressStruct) {

		addressMap.put(key, addressStruct);
	}
	
	public static long size(){
		if(addressMap==null){
			return -1;
		}
		return addressMap.size();
	}
	
	public static String k1_reference = null;
	
	public static List<AddressStruct> get(String key) throws ExecutionException{
	return addressMap.get(key);
	}

	
	public static void main(String[] args) throws ExecutionException,
			InterruptedException {
		// TODO Auto-generated method stub

		long start = System.currentTimeMillis();

		List<String> list = new ArrayList<>();
		
		list.add("1");
		list.add("2");
		list.add("3");
		System.out.println(list);
		
		List<String> list1 = new ArrayList<>();
		list1.addAll(list);
		System.out.println(list1);
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
