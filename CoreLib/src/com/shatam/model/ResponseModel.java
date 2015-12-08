package com.shatam.model;

import java.util.ArrayList;

import com.shatam.util.U;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.io.xml.XmlFriendlyReplacer;

public class ResponseModel
{

    public String                           status;
    public String                           message;
    public ArrayList<RawAddressStruct> addresses         = new ArrayList<RawAddressStruct>();
    public long                             totalProcessingTime;
    private long                            requestReceivedAt = System.currentTimeMillis();

    public String toXml()
    {
        totalProcessingTime = System.currentTimeMillis() - requestReceivedAt;

        XmlFriendlyReplacer replacer = new XmlFriendlyReplacer("ddd", "_");
        XStream xstream = new XStream(new DomDriver("UTF-8", replacer));

        // XStream xstream = new XStream();
        xstream.autodetectAnnotations(true);

        xstream.alias("response", ResponseModel.class);
        xstream.alias("address", RawAddressStruct.class);
        // xstream.alias("column", AddColumns.class);

        String xml = xstream.toXML(this);
        return xml;

    }

    public void add(AddressStruct as)
    {

        RawAddressStruct a = new RawAddressStruct();
        a.address = as.toOnlyStreet().toString().toUpperCase();
        
        a.house_number = as.getHouseNumber();
        a.prefix_direction = as.get(AddColumns.PREDIRABRV);
        a.prefix_qualifier = as.get(AddColumns.PREQUALABR);
        a.prefix_type = as.get(AddColumns.PRETYPABRV);
        a.street_name = as.get(AddColumns.NAME);
        a.suffix_type = as.get(AddColumns.SUFTYPABRV);
        a.suffix_direction = as.get(AddColumns.SUFDIRABRV);
        a.city = as.get(AddColumns.CITY);
        a.zip = as.get(AddColumns.ZIP);
        a.state = as.getState();
        a.score = as.hitScore;
        
        a.longitude = as.longitude;
        a.latitude = as.latitude;
        a._hnDistance = as._hnDistance;
        addresses.add(a);
    }

    public String toXml(ArrayList<AddressStruct> list)
    {
        for (int i = 0; i < list.size(); i++)
        {
            add(list.get(i));
        }

        return toXml();
    }
}
