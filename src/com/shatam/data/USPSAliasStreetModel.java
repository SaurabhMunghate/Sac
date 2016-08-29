package com.shatam.data;

import java.util.Collection;
import java.util.HashMap;

import com.shatam.io.AbstractIndexType;
import com.shatam.model.AddColumns;
import com.shatam.model.AddressStruct;
import com.shatam.util.U;


public class USPSAliasStreetModel
{
    private String                                                        realStreetKey;
    private String                                                        aliasStreetKey;
    private static HashMap<String, HashMap<String, USPSAliasStreetModel>> realStreetToAliasesMap = new HashMap<String, HashMap<String, USPSAliasStreetModel>>();

    public static Collection<USPSAliasStreetModel> getAliases(USPSStreetsModel realStreetModel ) throws Exception
    {
        if (realStreetToAliasesMap.size() == 0)
        {
            _readAndStore();
        }

        HashMap<String, USPSAliasStreetModel> map = realStreetToAliasesMap.get(realStreetModel.getKey());
        if (map==null) map = new HashMap<String, USPSAliasStreetModel>();
        return map.values();
    }//getAliases

    private static void _readAndStore() throws Exception
    {
        USPSUtil.readFile(USPSUtil.CITY_STATE_FILE_NAME, 'A', 129, new USPSUtil._USPSFileCallback() {

            @Override
            public void callback(byte[] data) throws Exception
            {

                USPSAliasStreetModel rec = new USPSAliasStreetModel(data);
                //U.log("Alias Query :"+AbstractIndexType.NORMAL.buildQuery(rec.createAddressStruct("CA", "XXX")) );

                HashMap<String, USPSAliasStreetModel> aliasesMap = realStreetToAliasesMap.get(rec.realStreetKey);
                if (aliasesMap == null)
                {
                    aliasesMap = new HashMap<String, USPSAliasStreetModel>();
                    realStreetToAliasesMap.put(rec.realStreetKey, aliasesMap);
                }
                aliasesMap.put(rec.aliasStreetKey, rec);
            }
        });

    }//_readAndStore

    public USPSAliasStreetModel(byte[] data)
    {
        zip = new String(data, 1, 5).trim().toUpperCase();

        //Alias Info
        aliasPreDirAbbr = new String(data, 6, 2).trim().toUpperCase();
        aliasStreetName = new String(data, 8, 28).trim().toUpperCase();
        aliasStreetSuffixAbbr = new String(data, 36, 4).trim().toUpperCase();
        aliasStreetPostDirAbbr = new String(data, 40, 2).trim().toUpperCase();

        //Real Street Info
        realPreDirAbbr = new String(data, 42, 2).trim().toUpperCase();
        realStreetName = new String(data, 44, 28).trim().toUpperCase();
        realStreetSuffixAbbr = new String(data, 72, 4).trim().toUpperCase();
        realStreetPostDirAbbr = new String(data, 76, 2).trim().toUpperCase();

        aliasTypeCode = new String(data, 78, 1).trim().toUpperCase();

        //Real key will be returned as alias key
        {
            StringBuffer writtenKey = new StringBuffer();
            writtenKey.append(realPreDirAbbr).append("-");
            writtenKey.append(realStreetName).append("-");
            writtenKey.append(realStreetSuffixAbbr).append("-");
            writtenKey.append(realStreetPostDirAbbr).append("-");
            writtenKey.append(zip).append("-");
            realStreetKey = writtenKey.toString().trim().toUpperCase();

        }

        //Alias Key
        {
            StringBuffer writtenKey = new StringBuffer();
            writtenKey.append(aliasPreDirAbbr).append("-");
            writtenKey.append(aliasStreetName).append("-");
            writtenKey.append(aliasStreetSuffixAbbr).append("-");
            writtenKey.append(aliasStreetPostDirAbbr).append("-");
            writtenKey.append(zip).append("-");
            aliasStreetKey = writtenKey.toString().trim().toUpperCase();
        }
        

    }//USPSAliasStreetModel()

     String zip;
    String aliasPreDirAbbr;
    String aliasStreetName;
    String aliasStreetPostDirAbbr;
    String aliasStreetSuffixAbbr;

    String realPreDirAbbr;
    String realStreetName;
    String realStreetPostDirAbbr;
    String realStreetSuffixAbbr;

    private String aliasTypeCode;

    public AddressStruct createAddressStruct(String state, String city) throws Exception
    {
        AddressStruct s = new AddressStruct(state);
        s.put(AddColumns.PREDIRABRV, aliasPreDirAbbr );
        s.put(AddColumns.PREQUALABR, "" );
        s.put(AddColumns.PRETYPABRV, "" );
        s.put(AddColumns.NAME, aliasStreetName );
        s.put(AddColumns.SUFTYPABRV, aliasStreetSuffixAbbr );
        s.put(AddColumns.SUFDIRABRV, aliasStreetPostDirAbbr );
        s.put(AddColumns.SUFQUALABR, "" );
        s.put(AddColumns.CITY, city );
        s.put(AddColumns.ZIP, zip );
         
    
        return s;
    
    }

 

}//class USPSAliasStreetModel


/*
1         COPYRIGHT DETAIL CODE     01        01   01    A=ALIAS
2         ZIP CODE                  05        02   06

          ALIAS STREET PRE DRCTN ABBREV  02        07   08
          ALIAS STREET NAME              28        09   36
          ALIAS STREET SUFFIX ABBREV     04        37   40
          ALIAS STREET POST DRCTN ABBREV 02        41   42

          REAL STREET PRE DRCTN ABBREV  02        43   44
          REAL STREET NAME              28        45   72
          REAL STREET SUFFIX ABBREV     04        73   76
          REAL STREET POST DRCTN ABBREV 02        77   78
          
5         ALIAS TYPE CODE           01        79   79    P=PREFERRED STREET NAME
                                                         C=STREET NAME CHANGED
                                                         O=NICKNAME/ OTHER
6         ALIAS DATE
           ALIAS CENTURY            02        80   81
           ALIAS YEAR               02        82   83
           ALIAS MONTH              02        84   85
           ALIAS DAY                02        86   87
7         ALIAS DELIVERY ADDRESS RANGE
           DELIVERY ADDRESS LOW NO  10        88   97
           DELIVERY ADDRESS HIGH NO 10        98  107

*/