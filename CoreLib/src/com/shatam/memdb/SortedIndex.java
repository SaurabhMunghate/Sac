package com.shatam.memdb;

import java.util.ArrayList;
import java.util.HashMap;

public class SortedIndex extends IndexType
{
    private HashMap<String, ArrayList<Integer>> indexSet = new HashMap<String, ArrayList<Integer>>();

    public SortedIndex(int indexBy)
    {
        super(indexBy);
    }

    @Override
    public boolean shouldAddRow(String[] rowStrings)
    {
        //String key = rowStrings[indexBy];
        //return !(indexSet.containsKey(key));
        return true;
    }

    @Override
    public void addToIndex(String[] rowStrings, int dataAtIndex)
    {
        String key = rowStrings[indexBy];
        ArrayList<Integer> arrInt = indexSet.containsKey(key) ? indexSet.get(key) : new ArrayList<Integer>();

        arrInt.add(dataAtIndex);
        indexSet.put(key, arrInt);
    }

    @Override
    public int[] find(String val)
    {
        if (!indexSet.containsKey(val))
            return new int[0];

        ArrayList<Integer> arrInt = indexSet.get(val);
        int[] arr = new int[arrInt.size()];
        for (int i = 0; i < arrInt.size(); i++)
        {
            arr[i] = arrInt.get(i);
        }
        return arr;

    }
}