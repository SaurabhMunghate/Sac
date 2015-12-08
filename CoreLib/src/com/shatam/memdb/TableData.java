package com.shatam.memdb;

import java.util.ArrayList;

import com.shatam.util.StrUtil;
import com.shatam.util.U;

public class TableData
{
    private ArrayList<String[]> data = new ArrayList<String[]>();

    // int indexBy = 0;
    // private HashMap<String, Integer> indexSet = new HashMap<String,
    // Integer>();
    private IndexType           indexType;

    public TableData(IndexType indexType)
    {
        this.indexType = indexType;
    }

    public void add(Object[] rowObjects, int maxColns)
    {

        String[] rowStrings = StrUtil.convertToStringArr(rowObjects, maxColns);

        if (this.indexType != null)
        {
            if (!indexType.shouldAddRow(rowStrings))
            {
                // U.log("Exists key:" + key);
                return;
            }

            indexType.addToIndex(rowStrings, data.size());

        }

        data.add(rowStrings);

    }// add()

    public ArrayList<String[]> searchAndFind(String val) throws Exception
    {
        ArrayList<String[]> retList = new ArrayList<String[]>();
        if (val == null)
            return retList;

        if (this.indexType == null)
            throw new Exception("Not indexed");

        val = val.trim().toLowerCase();

        int[] foundAt = indexType.find(val);
        for (int i : foundAt)
        {
            retList.add(data.get(i));
        }

        return retList;
    }

    public int size()
    {
        
        return data.size();
    }

    public String[] getRowAt(int rowNumber)
    {
        
        return data.get(rowNumber);
    }

    public void close()
    {
        /*
        while (data.size() > 0)
        {
            //U.log("clear data.size():" + data.size());
            String[] arr = data.remove(0);
            for (int j = 0; j < arr.length; j++)
            {
                arr[j] = null;
            }
            arr = null;
        }*/
        data.clear();
        System.gc();
    }
}// class TableData
