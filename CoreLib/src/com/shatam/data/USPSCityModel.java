package com.shatam.data;

import java.io.FileInputStream;
import java.io.IOException;

import com.shatam.util.U;

public class USPSCityModel
{

    public String zip;
    public String cityStateKey;
    public String zipClassificationCode;
    public String cityStateName;
    public String cityStateNameAbbr;

    
    public USPSCityModel(byte[] data)
    {
         

        zip = new String(data, 1, 5).trim().toUpperCase();

        cityStateKey = new String(data, 1, 6).trim().toUpperCase();

        zipClassificationCode = new String(data, 12, 1).trim().toUpperCase();

        cityStateName = new String(data, 13, 28).trim().toUpperCase();
        cityStateNameAbbr = new String(data, 41, 13).trim().toUpperCase();
    }
    /*
     *   1         COPYRIGHT DETAIL CODE     01        01   01    D=DETAIL
    2         ZIP CODE                  05        02   06
    3         CITY STATE KEY            06        07   12
    4         ZIP CLASSIFICATION CODE   01        13   13    BLANK=NON-UNIQUE
                                                                 ZIP
                                                           M=APO/FPO MILITARY
                                                             ZIP
                                                           P=PO BOX ZIP
                                                           U=UNIQUE ZIP
    5         CITY STATE NAME           28        14   41
    6         CITY STATE NAME ABBREV    13        42   54

     */

}
