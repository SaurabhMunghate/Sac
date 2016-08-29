package com.shatam.model;

import java.util.Comparator;

import com.shatam.util.StrUtil;

public class SecondaryAddressComparator implements Comparator<_USPSSecondaryStruct>
{

    private String houseNumber = null;
    private String unitNumber  = null;

    public SecondaryAddressComparator(String houseNumber, String unitNumber)
    {
        this.houseNumber = houseNumber;
        this.unitNumber = unitNumber;
    }

    @Override
    public int compare(_USPSSecondaryStruct s1, _USPSSecondaryStruct s2)
    {
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

        //U.log("Compare:"+ v1 + " == "+v2);

        v1 = s1.isUnitInRange(unitNumber) ? 0 : 1;
        v2 = s2.isUnitInRange(unitNumber) ? 0 : 1;

        if (v1==v2)
        {
            //addrSecondaryAbbr
            v1 = s2.deltaInHouseNum(); //swapped intentionally
            v2 = s1.deltaInHouseNum();
        }        
        return v1-v2;
    }

    /*
    @Override
    public int compare(_USPSSecondaryStruct s1, _USPSSecondaryStruct s2)
    {
        String v1, v2;

        v1 = s1.isOnSameSideOfRoad(houseNumber) ? "A" : "X";
        v2 = s2.isOnSameSideOfRoad(houseNumber) ? "A" : "X";
        if (!v1.equals(v2))
            return v1.compareTo(v2);

        v1 = StrUtil.isEmpty(s1.addrSecondaryLowNo) ? "X" : "A";
        v2 = StrUtil.isEmpty(s2.addrSecondaryLowNo) ? "X" : "A";

        if (!v1.equals(v2))
            return v1.compareTo(v2);

        v1 = StrUtil.isEmpty(s1.addrSecondaryHighNo) ? "X" : "A";
        v2 = StrUtil.isEmpty(s2.addrSecondaryHighNo) ? "X" : "A";

        if (!v1.equals(v2))
            return v1.compareTo(v2);

        //U.log("Compare:"+ v1 + " == "+v2);

        {
            v1 = s1.isUnitInRange(unitNumber) ? "A" : "X";
            v2 = s2.isUnitInRange(unitNumber) ? "A" : "X";
        }
        if (v1.equals(v2))
        {

            //addrSecondaryAbbr
            v1 = s2.deltaInHouseNum(); //swapped intentionally
            v2 = s1.deltaInHouseNum();
        }

        return v1.compareTo(v2);
    }
    */
}
