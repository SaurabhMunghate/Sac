package com.shatam.io;

public class NormalIndexType extends AbstractIndexType
{
    String fielName = null;
    public NormalIndexType(String field)
    {
        fielName = field;
    }

    public String getFieldName()
    {
        return fielName;

    }
    public String innerEncode(String v){
        return v;
    }
    
}//NormalIndexType