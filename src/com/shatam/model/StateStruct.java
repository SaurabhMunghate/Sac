/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.model;

public class StateStruct
{
    public StateStruct(int countyStart, int countEnd, String state)
    {
        this.countyStart = countyStart;
        this.countEnd = countEnd;
        this.state = state;
    }
    

    public int countyStart = -1;
    public int countEnd    = -1;
    public String state;
}