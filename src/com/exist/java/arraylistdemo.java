package com.exist.java;

import java.security.KeyStore.Entry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;

public class arraylistdemo {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// create multimap to store key and values
		
		MultiMap multiMap = new MultiValueMap();
		
		 
		
		// put values into map for A
		
		multiMap.put("A", "Apple");
		
		multiMap.put("A", "Aeroplane");
		multiMap.put("A", "Kirti");
		 
		
		// put values into map for B
		
		multiMap.put("B", "Bat");
		
		multiMap.put("B", "Banana");
		
		
		// put values into map for C
		
		multiMap.put("C", "Cat");
		
		multiMap.put("C", "Car");
		
		 
		
		// retrieve and display values
		
		System.out.println("Fetching Keys and corresponding [Multiple] Values n");
		
		 
		
		// get all the set of keys
	
		Set<String> keys = multiMap.keySet();
		for (String key : keys) {
            List list = (List) multiMap.get(key);
            for (int i = 0; i < list.size(); i++) {
               // for (int j = i + 1; j < list.size(); j++) {
            	String kk=(String) list.get(i);
                    System.out.println(kk );
                //}
            }
            System.out.println();
        }
    
		 
		
		// iterate through the key set and display key and values
		
		for (String key : keys) {
		
//		System.out.println("Key = " + key);
//		for(Entry e : multiMap.entries()) {
//			  System.out.println(e.getKey()+": "+e.getValue());
//			}
//		Collections value=(Collections) multiMap.get(key);
//		 Iterator valuesIterator = ((Set<String>) value).iterator( );  
////		for(String kk:valuesIterator){
////			System.out.println("Values 1= " +kk);
////		}
//		 while( valuesIterator.hasNext( ) ) {  
//	            System.out.print( "Value:1 " + valuesIterator.next( ) + ". " );  
//	        }  
		String value=multiMap.get(key).toString();
		System.out.println("Values = " + value + "n");
		
		}
	
		 
          
		
	}

}
