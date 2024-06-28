/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.util;

import java.util.ArrayList;

import com.shatam.io.AbstractIndexType;
import com.shatam.model.AddressStruct;

public class AddressCorrector {

	public static AddressStruct corrUsingAppropriateIndex(String address1,
			String address2, String city, String state, String zip)
			throws Exception {

		AddressStruct returnAddStruct = null;

		for (AbstractIndexType it : AbstractIndexType.TYPES) {

			for (final String dataSource : new String[] { U.USPS }) {

				ArrayList<AddressStruct> resultAdds = new ArrayList<>();
				if (resultAdds.size() == 0)
					continue;

				returnAddStruct = resultAdds.get(0);
				String foundStreet = returnAddStruct.getFoundName();

				{
					if (foundStreet.startsWith("RR")) {
						returnAddStruct
								.setHouseNumber(returnAddStruct.unitNumber);
						returnAddStruct.unitNumber = "";
						returnAddStruct.unitTypeFromInputAddress = "";
					}
				}

				DistanceMatchForResult matcher = new DistanceMatchForResult(
						returnAddStruct, it);
			}

		}

		{
			returnAddStruct.setBlank();
			return returnAddStruct;
		}

	}

}
