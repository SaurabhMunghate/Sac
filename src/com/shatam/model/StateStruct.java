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