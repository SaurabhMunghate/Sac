package com.shatam.data;

import java.util.ArrayList;
import java.util.HashSet;

import com.shatam.model.AddColumns;
import com.shatam.model.AddressStruct;
import com.shatam.util.StrUtil;
import com.shatam.util.U;

public class USPSStreetsModel
{
    @Override
    public String toString()
    {
        return dataKey;
    }

    @Override
    public int hashCode()
    {
        return dataKey.hashCode();
    }

    protected String                        zip;
    protected String                        preDirAbbr;
    protected String                        recordTypeCode;
    protected String                        streetName;
    protected String                        streetSuffixAbbr;
    protected String                        streetPostDirAbbr;
    protected ArrayList<USPSSecStreetModel> subDatas = new ArrayList<USPSSecStreetModel>();
    protected String                        dataKey;
    protected String                        stateAbbr;
    protected String                        stateCode;
    protected String                        actionCode;
    protected String                        countyno;

    public void removeData(byte[] data)
    {
        USPSSecStreetModel subData = new USPSSecStreetModel(data);
        for (USPSSecStreetModel sd : this.subDatas)
        {
            if (sd.serialize().equals(subData.serialize()))
            {
                System.err.println("DELETING " + sd.serialize());
                subDatas.remove(sd);
                break;
            }
        }
    }

    public void addData(byte[] data) throws Exception
    {
        USPSSecStreetModel subData = new USPSSecStreetModel(data);
        subDatas.add(subData);
    }
 
    public USPSStreetsModel(byte[] data) throws Exception
    {
        zip = new String(data, 1, 5).trim().toUpperCase();

        actionCode = new String(data, 16, 1).trim().toUpperCase();
        //01         17   17 A= ADD D= DELETE (ALWAYS "A" FOR BASE FILE)       

        recordTypeCode = new String(data, 17, 1).trim(); //F=FIRM  G=GENERAL DELIVERY    H=HIGHRISE    P=PO BOX    R=RURAL ROUTE/HIGHWAY CONTRACT    S=STREET
        preDirAbbr = new String(data, 22, 2).trim().toUpperCase();
        streetName = new String(data, 24, 28).trim().toUpperCase();
        streetSuffixAbbr = new String(data, 52, 4).trim().toUpperCase();

        //10         STREET POST DRCTN ABBREV  02        57   58
        streetPostDirAbbr = new String(data, 56, 2).trim().toUpperCase();

        stateAbbr = new String(data, 157, 2);
        stateCode = U.getStateCode(stateAbbr);
        countyno=new String(data, 159, 3).trim().toUpperCase();
//        U.log("COUNTYNUMBER="+countyno);
//        Thread.sleep(4000);
        StringBuffer writtenKey = new StringBuffer();
        writtenKey.append(preDirAbbr).append("-");
        writtenKey.append(streetName).append("-");
        writtenKey.append(streetSuffixAbbr).append("-");
        writtenKey.append(streetPostDirAbbr).append("-");
        writtenKey.append(zip).append("-");
        dataKey = writtenKey.toString().trim().toUpperCase();
      
    }
 

    public USPSStreetsModel(AddressStruct addStruct)
    {
        zip = addStruct.get(AddColumns.ZIP).trim().toUpperCase();

        actionCode = "A";
        recordTypeCode = "S";

        preDirAbbr = addStruct.get(AddColumns.PREDIRABRV).trim().toUpperCase();
        streetName = addStruct.get(AddColumns.NAME).trim().toUpperCase();
        streetSuffixAbbr = addStruct.get(AddColumns.SUFTYPABRV).trim().toUpperCase();

        streetPostDirAbbr = addStruct.get(AddColumns.SUFDIRABRV).trim().toUpperCase();

        stateAbbr = addStruct.getState().trim().toUpperCase();
        stateCode = U.getStateCode(stateAbbr);
countyno=addStruct.get(AddColumns.COUNTYNO).trim().toUpperCase();

        StringBuffer writtenKey = new StringBuffer();
        writtenKey.append(preDirAbbr).append("-");
        writtenKey.append(streetName).append("-");
        writtenKey.append(streetSuffixAbbr).append("-");
        writtenKey.append(streetPostDirAbbr).append("-");
        writtenKey.append(zip).append("-");
        dataKey = writtenKey.toString().trim().toUpperCase();
        
    }

    public String getStreetName()
    {
        // TODO Auto-generated method stub
        return streetName;
    }

    public String getKey()
    {
        // TODO Auto-generated method stub
        return dataKey;
    }

    public ArrayList<AddressStruct> getAddressStructs() throws Exception
    {
        ArrayList<AddressStruct> addStructs = new ArrayList<AddressStruct>();
        addStructs.addAll(getAddressStructs(zip));

        //for (String useZip: ZipCodes.getZoneSplitZips(zip)){
        //    addStructs.addAll(getAddressStructs(useZip));
        //}
        return addStructs;
    }//getAddressStructs()
    
    private ArrayList<AddressStruct> getAddressStructs(String useZip) throws Exception
    {
        HashSet<String> cities = ZipCodes.getCity(useZip);
        ArrayList<AddressStruct> list = new ArrayList<AddressStruct>();

        int i = 0;
        for (String city : cities)
        {
            
            AddressStruct fetStruct = new AddressStruct(stateCode);

            fetStruct.put(AddColumns.CITY, city);
            fetStruct.put(AddColumns.ZIP, useZip);

            fetStruct.put(AddColumns.PREDIRABRV, preDirAbbr);
            //fetStruct.put(AddColumns.PREQUALABR, 
            //fetStruct.put(AddColumns.PRETYPABRV, 
            fetStruct.put(AddColumns.NAME, streetName);
            fetStruct.put(AddColumns.SUFTYPABRV, streetSuffixAbbr);
            fetStruct.put(AddColumns.SUFDIRABRV, streetPostDirAbbr);
            fetStruct.put(AddColumns.COUNTYNO, countyno);
            StringBuffer bufSubData = new StringBuffer();

            //U.log("zip:"+zip);
            //if (zip.equals("97070") && city.equalsIgnoreCase("WILSONVILLE") && streetName.equalsIgnoreCase("MAXINE")){
            //    U.log("this.subDatas.size():"+this.subDatas.size());
            //}
            for (USPSSecStreetModel subData : this.subDatas)
            {
                bufSubData.append(subData.serialize()).append("\n");
            }

            fetStruct.put(AddColumns.DATA, bufSubData);

            list.add(fetStruct);
        }//for cities

        return list;

    }//getAddressStructs

    public String getStateCode()
    {
        return this.stateCode;
    }

    public String getActionCode()
    {
        return this.actionCode;
    }

    public String getZip()
    {
        return this.zip;
    }

}
