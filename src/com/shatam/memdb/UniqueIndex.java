/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.memdb;

import java.util.HashMap;

public class UniqueIndex extends IndexType
{
    private HashMap<String, Integer> indexSet = new HashMap<String, Integer>();

    public UniqueIndex(int indexBy)
    {
        super(indexBy);
    }

    @Override
    public boolean shouldAddRow(String[] rowStrings)
    {
        String key = rowStrings[indexBy];
        return !(indexSet.containsKey(key));
    }

    @Override
    public void addToIndex(String[] rowStrings, int dataAtIndex)
    {
        indexSet.put(rowStrings[indexBy], dataAtIndex);
    }

    @Override
    public int[] find(String val)
    {
        if (!indexSet.containsKey(val))
            return new int[0];

        int foundAt = indexSet.get(val);
        return new int[]
        { foundAt };
    }
}