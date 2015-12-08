package com.shatam.interpolator;

import com.vividsolutions.jts.geom.Coordinate;

public class StreetSegment
{
    private Coordinate from;
    private Coordinate to;
    private double     length;

    public StreetSegment(Coordinate from, Coordinate to)
    {
        this.from = from;
        this.to = to;
        length = calculateLength();
    }

    double calculateLength()
    {
        double dy = to.y - from.y;
        double dx = to.x - from.x;
        return Math.sqrt(dy * dy + dx * dx);
    }

    public Coordinate getFrom()
    {
        return from;
    }

    public Coordinate getTo()
    {
        return to;
    }

    public double getLength()
    {
        return length;
    }

    @Override
    public String toString()
    {
        return new String("Segment from " + from + " to " + to + ", length  " + length);
    }

}