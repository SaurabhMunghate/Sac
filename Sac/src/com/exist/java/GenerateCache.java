package com.exist.java;

import java.util.Iterator;

import javax.servlet.http.HttpServletResponse;

import com.shatam.io.AbstractIndexType;
import com.shatam.io.ShatamIndexReader;
import com.shatam.io.ShatamIndexUtil;
import com.shatam.util.U;

public class GenerateCache {
	public static void main(String args[]) throws Exception {

		doCache();
	}

	public static void doCache() throws Exception {
		// AbstractIndexType it = AbstractIndexType.NORMAL;
		Iterator iterator = U.STATE_MAP.keySet().iterator();

		while (iterator.hasNext()) {
			String state = (String) iterator.next();
		//	 String state="CA";

			for (AbstractIndexType it : AbstractIndexType.TYPES) {

				for (final String dataSource : new String[] { U.USPS }) {

					String readerKey = state + it.getFieldName() + "-"
							+ dataSource;
					ShatamIndexReader reader = new ShatamIndexReader(it, state,
							dataSource);
					ShatamIndexUtil.readerMap.put(readerKey, reader);
				}
			}
			// // break;
		}

	}

}
