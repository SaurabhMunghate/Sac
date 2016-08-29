package com.shatam.util;

import java.util.ArrayList;

import com.shatam.io.AbstractIndexType;
import com.shatam.io.ShatamIndexUtil;
import com.shatam.model.AddColumns;
import com.shatam.model.AddressStruct;

public class AddressCorrector
{



    public static AddressStruct corrUsingAppropriateIndex(String address1, String address2, String city, String state, String zip)
            throws Exception
    {

        //ArrayList<AddressStruct> arr = new ArrayList<AddressStruct>();
        AddressStruct returnAddStruct = null;

        for (AbstractIndexType it : AbstractIndexType.TYPES)
        {

            for (final String dataSource : new String[] { U.USPS})
            {
                //U.log("  *** " + it.getFieldName() + " / " + dataSource);

                ArrayList<AddressStruct> resultAdds =new ArrayList<>();//= ShatamIndexUtil.correctAddresses(address1, address2, city, zip, state, it, dataSource);
                if (resultAdds.size() == 0)
                    continue;

                returnAddStruct = resultAdds.get(0);

                //arr.add(t);
                String foundStreet = returnAddStruct.getFoundName(); //.split(StrUtil.WORD_DELIMETER)[0];

                //adjust for RR
                {
                    if (foundStreet.startsWith("RR"))
                    {
                        returnAddStruct.setHouseNumber(returnAddStruct.unitNumber);
                        returnAddStruct.unitNumber = "";
                        returnAddStruct.unitTypeFromInputAddress = "";
                    }
                }

                ////U.log("A shatamIndexQueryString:" + returnAddStruct.getshatamIndexQueryString());
                ////U.log("B foundStreet:" + foundStreet + "  score(t):" + score(returnAddStruct));
                ////U.log("C Found :"+foundStreet + " , "+foundCity);
                
                DistanceMatchForResult matcher = new DistanceMatchForResult(returnAddStruct, it);
             /*   if (matcher.isResultMatched()){
                    return returnAddStruct;
                }else{
                    U.disp(returnAddStruct);
                }*/
                
                /*
                if (DistanceMatchForResult.isMatchGoodEnough(foundStreet, returnAddStruct, it, score))
                {
                    final String foundCity = returnAddStruct.get(AddColumns.CITY).toUpperCase();
                    final String foundZip = returnAddStruct.get(AddColumns.ZIP).toUpperCase();

                    //U.log("C Found :" + foundStreet + " , " + foundCity);
                    if (DistanceMatchForResult.isMatchGoodEnough(foundCity, returnAddStruct, it, score) || DistanceMatchForResult.isMatchGoodEnough(foundZip, returnAddStruct, it, score))
                    {
                        //U.log("D Found :" + foundStreet + " , " + foundCity);
                        return returnAddStruct;
                    }

                }
                */

            }//for (final String dataSource

        }// for it

        //create empty struct
        {
            returnAddStruct.setBlank();
            //U.log("?????????? NOT FOUND ??????????/ returnAddStruct:" + returnAddStruct.toOnlyStreet());
            return returnAddStruct;
        }

    }//corrUsingAppropriateIndex()




}
