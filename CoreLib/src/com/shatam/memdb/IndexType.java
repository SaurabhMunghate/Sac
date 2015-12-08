package com.shatam.memdb;

public abstract class IndexType
{
    protected int indexBy = -1;

    public IndexType(int indexBy)
    {
        this.indexBy = indexBy;
    }

    public abstract boolean shouldAddRow(String[] rowStrings);

    public abstract void addToIndex(String[] rowStrings, int size);

    public abstract int[] find(String val);
}
