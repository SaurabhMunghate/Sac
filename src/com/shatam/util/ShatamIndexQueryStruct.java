package com.shatam.util;

import com.shatam.io.AbstractIndexType;
import com.shatam.shatamindex.queryParser.QueryParser;
import com.shatam.shatamindex.search.Query;

public class ShatamIndexQueryStruct
{

    private String            houseNumber = "";
    private String            query       = null;
    private String            address     = null;
    private String            city        = null;
    private String            zip         = null;
    private String            state       = null;
    private AbstractIndexType indexType;

    private String normalizedStreetName;
    private String normalizedCity;
    private String normalizedZip;

    //--------- Normalized variables
    
    public void setHouseNumber(String hn)
    {
        houseNumber = hn;
    }

    public String getHouseNumber()
    {
        return houseNumber;
    }

    public String getQuery()
    {
        return query;
    }

    public String getAddress()
    {
        return address;
    }

    public String getState()
    {
        return state;
    }

    public AbstractIndexType getIndexType()
    {
        return indexType;
    }

    public ShatamIndexQueryStruct(String add, String city, String zip, String state2, AbstractIndexType indexType2)
    {
    	//U.log("indextype2****"+indexType2.getFieldName());
        address = add;
        this.city = city;
        this.zip = zip;
        state = state2;
        indexType = indexType2;
    }

    public ShatamIndexQueryStruct() {
		// TODO Auto-generated constructor stub
	}

	public Query createQueryObj(QueryParser parser) throws Exception
    {
        Query q = null;
        synchronized (parser) {
        try
        {
			
		
        	 //U.log("Query==555=="+query.toString());
            q = parser.parse(query);
            
           // U.log("Query==333333=="+q .toString());
            
            if(q==null){
				U.log("OMG queryyyyy@@@@@=null");
			}
            
            //query = new FuzzyQuery(new Term("body", "999"));
            //
        } catch (Exception e)
        {
        	 U.log("q======================="+q);
            U.log("ERROR:" + e);
            U.log("ERROR : queryString:" + query);
            U.log("ERROR : address:" + address);
          //  throw new Exception("ERROR : QueryParser.parse ('" + address + "')");
        }
       }
        
     //   U.log("*******@@@@@@@@@"+q);
        
        
        return q;
    }

    public void setQuery(String q) throws Exception
    {
        //if (StrUtil.isEmpty(q))
        //    throw new Exception("Blank query");
    	//U.log(q);
        this.query = q;

    }

    public String getCity()
    {
        // TODO Auto-generated method stub
        return city;
    }
    

    public String getZip()
    {
        // TODO Auto-generated method stub
        return zip;
    }

    public void setNormalizedStreetName(String stName)
    {
        this.normalizedStreetName = stName;
        
    }

    public void setNormalizedCity(String city)
    {
        this.normalizedCity = city;
        
    }

    public void setNormalizedZip(String zip)
    {
        this.normalizedZip = zip;
        
    }
    public String getNormalizedStreetName()
    {
        return normalizedStreetName;
    }

    public String getNormalizedCity()
    {
        return normalizedCity;
    }

    public String getNormalizedZip()
    {
        return normalizedZip;
    }

    
}