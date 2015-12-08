package com.shatam.io;

import java.util.Collection;
import java.util.HashMap;

import com.shatam.data.ZipCodes;
import com.shatam.model.AddressStruct;
import com.shatam.util.StrUtil;
import com.shatam.util.TLIDModel;
import com.shatam.util.U;

public class AddrIndex
{
    private HashMap<String, HashMap<String, TLIDModel>> map   = new HashMap<String, HashMap<String, TLIDModel>>();

    private String                                      state = null;

    public String getState()
    {
        return state;
    }

    public AddrIndex(String state)
    {
        this.state = state;
    }

    public String toDispString(){
        String st1r = "------ Map Size: "+map.size()+" ------------- \n";
        /*
        for (Entry<String, HashMap<String, TLIDModel>> pair: map.entrySet()){
            StringBuffer  s2 = new StringBuffer(); 
            s2.append( "tlid:"+pair.getKey() );
            s2.append( " = \n" );
            
            for (Entry<String, TLIDModel> subpair: pair.getValue().entrySet()){
                s2.append("   ").append( subpair.getValue().getCity()).append(", ").append( subpair.getValue().getZip());
                s2.append("\n");
                s2.append( "   ").append( subpair.getValue().getFromToRanges().size());
                s2.append("\n");
            }
            U.log(s2);
        }
        */
        return st1r;
    }//toDispString()

    private void add(String tlid, String zip, String city, String from, String to)
    {
        
        
        tlid = tlid.toLowerCase().trim();
        HashMap<String, TLIDModel> models = map.get(tlid);

        if (models == null)
        {
            models = new HashMap<String, TLIDModel>();
            map.put(tlid, models);
        }

        String key = city + " -- " + zip;
        
        TLIDModel m = models.get(key);
        if (m == null)
        {
            m = new TLIDModel(city, zip);
            models.put(key, m);
        }

        m.addFromTo(from, to);

    }// add

    public Collection<TLIDModel> get(String tlid)
    {
        tlid = tlid.toLowerCase().trim();
        if (!map.containsKey(tlid)) return null;
        return map.get(tlid).values();
    }


    public boolean load(Object[] addrRow) throws Exception
    {

        // ==== addr ================
        // 0)TLID 1)FROMHN 2)TOHN 3)SIDE 4)ZIP 5)PLUS4 6)FROMTYP 7)TOTYP
        // 8)ARID
        // 9)MTFCC
        // 187064856 919 721 L 98022 I 400837067383 D1000
        String tlid = U._toStr(addrRow[0]);
        String from = U._toStr(addrRow[1]);
        String to = U._toStr(addrRow[2]);
        String zip = U._toStr(addrRow[4]);


        if (StrUtil.isEmpty(zip))
            return false;
        for (String city : ZipCodes.getCity(zip))
        {
            this.add(tlid, zip, city, from, to);
        }

        return true;
    }// load

    public void close()
    {
        map.clear();
        map = null;
    }

    private HashMap<StringBuffer, String> tlid2AddrKey = new HashMap<StringBuffer, String>();
    public void setKey(StringBuffer addrKey, String tlid)
    {
        tlid2AddrKey.put(addrKey, tlid);
    }

    public boolean containsKey(StringBuffer writtenKey)
    {
        
        return tlid2AddrKey.containsKey(writtenKey);
    }

    public void expandRange(String tlid, AddressStruct addStruct)
    {
        //
        
    }
    
    
}// class AddrIndex
