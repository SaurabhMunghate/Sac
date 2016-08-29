package com.shatam.data;

import com.shatam.util.StrUtil;

class USPSSecStreetModel
{
    public String addrPrimaryOddEvenCode;
    public String buildingOrFirmName;
    public String addrSecondaryAbbr;

    public String addrPrimaryLowNo;
    public String addrPrimaryHighNo;
    public String addrSecondaryLowNo;
    public String addrSecondaryHighNo;
    public String county;
    public String lowNoZipPlus4;
    public String highNoZipPlus4;

    public USPSSecStreetModel(byte[] data)
    {

        addrPrimaryLowNo = new String(data, 58, 10).trim().toUpperCase();
        addrPrimaryHighNo = new String(data, 68, 10).trim().toUpperCase();

        addrPrimaryOddEvenCode = new String(data, 78, 1).trim();
        buildingOrFirmName = new String(data, 79, 40).trim().replaceAll(",", " ");
        addrSecondaryAbbr = new String(data, 119, 4).trim().toUpperCase().replaceAll(",", " "); //ONLY REC-TYPES F,H
        addrSecondaryLowNo = new String(data, 123, 8).trim().replaceAll(",", " ");
        addrSecondaryHighNo = new String(data, 131, 8).trim().replaceAll(",", " ");
        county = new String(data, 159, 3).trim().replaceAll(",", " ");

        addrPrimaryLowNo = parseAddrNo(addrPrimaryLowNo);
        addrPrimaryHighNo = parseAddrNo(addrPrimaryHighNo);
        addrSecondaryLowNo = parseAddrNo(addrSecondaryLowNo);
        addrSecondaryHighNo = parseAddrNo(addrSecondaryHighNo);

        //---- 19         (ZIP ADD ON LOW NO)
        //ZIP SECTOR NO            02       141  142
        //ZIP SEGMENT NO           02       143  144
        lowNoZipPlus4 = new String(data, 140, 4).trim();

        //---- 20        ZIP ADD ON HIGH NO
        //ZIP SECTOR NO            02       145  146
        //ZIP SEGMENT NO           02       147  148
        highNoZipPlus4 = new String(data, 144, 4).trim();

    }

    public StringBuffer serialize()
    {
        StringBuffer buf = new StringBuffer();

        buf.append(addrPrimaryOddEvenCode).append(",");
        buf.append(buildingOrFirmName).append(",");
        buf.append(addrSecondaryAbbr).append(",");

        buf.append(addrPrimaryLowNo).append(",");
        buf.append(addrPrimaryHighNo).append(",");
        buf.append(addrSecondaryLowNo).append(",");
        buf.append(addrSecondaryHighNo).append(",");
        buf.append(county).append(",");
        buf.append(lowNoZipPlus4).append(",");
        buf.append(highNoZipPlus4).append(",");

        return buf;
    }

    private static String parseAddrNo(String s)
    {
        if (StrUtil.isEmpty(s))
            return "";
        try
        {
            return "" + Integer.parseInt(s);
        } catch (Exception ex)
        {
            return s;
        }
    }

}//SubData