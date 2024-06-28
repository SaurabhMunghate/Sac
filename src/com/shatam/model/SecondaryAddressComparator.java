/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.model;

import java.util.Comparator;

import com.shatam.util.StrUtil;

public class SecondaryAddressComparator implements
		Comparator<_USPSSecondaryStruct> {

	private String houseNumber = null;
	private String unitNumber = null;

	public SecondaryAddressComparator(String houseNumber, String unitNumber) {
		this.houseNumber = houseNumber;
		this.unitNumber = unitNumber;
	}

	@Override
	public int compare(_USPSSecondaryStruct s1, _USPSSecondaryStruct s2) {
		int v1, v2;

		v1 = s1.isOnSameSideOfRoad(houseNumber) ? 0 : 1;
		v2 = s2.isOnSameSideOfRoad(houseNumber) ? 0 : 1;

		if (v1 != v2)
			return v1 - v2;

		v1 = StrUtil.isEmpty(s1.addrSecondaryLowNo) ? 1 : 0;
		v2 = StrUtil.isEmpty(s2.addrSecondaryLowNo) ? 1 : 0;

		if (v1 != v2)
			return v1 - v2;

		v1 = StrUtil.isEmpty(s1.addrSecondaryHighNo) ? 1 : 0;
		v2 = StrUtil.isEmpty(s2.addrSecondaryHighNo) ? 1 : 0;

		if (v1 != v2)
			return v1 - v2;

		v1 = s1.isUnitInRange(unitNumber) ? 0 : 1;
		v2 = s2.isUnitInRange(unitNumber) ? 0 : 1;

		if (v1 == v2) {

			v1 = s2.deltaInHouseNum();
			v2 = s1.deltaInHouseNum();
		}
		return v1 - v2;
	}

}
