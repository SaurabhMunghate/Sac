package com.shatam.interpolator;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.shatam.model.AddColumns;
import com.shatam.model.AddressStruct;
import com.shatam.util.StrUtil;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.io.WKTReader;

public class Interpolate
{

    private static int _int(AddColumns col, AddressStruct addStruct)
    {
        String hn1 = addStruct.get(col);
        hn1 = hn1.replaceAll("[^\\d]", "");
        if (StrUtil.isEmpty(hn1))
        {
            return -1;
        } else
        {
            return Integer.parseInt(hn1);
        }
    }// _int

    public static int distanceBetweenHouseNums(String sHouseNumber, AddressStruct addStruct)
    {
        if (StrUtil.isEmpty(sHouseNumber))
            return 0;

        int hn = Integer.parseInt(sHouseNumber.replaceAll("[^\\d]", ""));

        // U.log("1 isBetweenHouseNums [" + addStruct.hitScore + "] add:" +
        // addStruct.toString(false));
        // U.log("2 isBetweenHouseNums HN:" + addStruct.get(AddColumns.FROMHN) +
        // "-" + addStruct.get(AddColumns.TOHN));

        {
            // Check with Left
            int lfrom = _int(AddColumns.LFROMADD, addStruct);
            int lto = _int(AddColumns.LTOADD, addStruct);

            if (lfrom != -1 && lfrom % 2 == hn % 2)
            {

                return isBetween(hn, lfrom, lto) ? 0 : Math.min(Math.abs(hn - lfrom), Math.abs(hn - lto));
            }
        }

        {
            // Check with Right
            int rfrom = _int(AddColumns.RFROMADD, addStruct);
            int rto = _int(AddColumns.RTOADD, addStruct);
            if (rfrom != -1 && rfrom % 2 == hn % 2)
            {

                int ret = isBetween(hn, rfrom, rto) ? 0 : Math.min(Math.abs(hn - rfrom), Math.abs(hn - rto));
                //U.log("B ret:" + ret);
                return ret;
            }
        }

        return Integer.MAX_VALUE;

    }

    private static boolean isBetween(int hn, int lfrom, int lto)
    {
        return (hn >= Math.min(lfrom, lto) && hn <= Math.max(lfrom, lto));
    }

    private static Logger log = Logger.getLogger(Interpolate.class.getName());

    public static Coordinate houseNumberOnStreetSegments(Coordinate[] vertices, int leftFrom, int leftTo, int rightFrom, int rightTo,
            int targetHouseNumber)
    {
        Coordinate target = null;

        // considering street center target, calculate based on left numbers
        double targetPercentOfTotalLength = 100 * (targetHouseNumber - leftFrom) / (leftTo - leftFrom);
        // log.info("targetPercentOfTotalLength = "+
        // targetPercentOfTotalLength);

        List<StreetSegment> segments = new ArrayList<StreetSegment>();
        double totalLength = 0;
        for (int i = 0; i < vertices.length - 1; i++)
        {
            StreetSegment seg = new StreetSegment(vertices[i], vertices[i + 1]);
            totalLength += seg.getLength();
            segments.add(seg);
        }

        /*
         * if target house not in the valid range return corresponding end point
         */
        if (targetPercentOfTotalLength <= 0 || targetPercentOfTotalLength >= 100)
        {
            Coordinate A = vertices[0];
            Coordinate B = vertices[vertices.length - 1];
            StreetSegment seg = new StreetSegment(A, B);
            double needToExtendBy = totalLength * targetPercentOfTotalLength / 100;
            double k = needToExtendBy / seg.getLength();
            double cx = B.x - k * (A.x - B.x);
            double cy = B.y - k * (A.y - B.y);

            return new Coordinate(cx, cy);

        }

        // log.info("Total length of all segments "+ totalLength);
        double targetPercent = targetPercentOfTotalLength;
        for (StreetSegment seg : segments)
        {
            double lengthPercent = seg.getLength() / totalLength * 100;
            if (targetPercent <= lengthPercent)
            {
                // log.info("Found segment "+ seg);
                return pointOnSegment(seg, targetPercent / lengthPercent);
            }
            targetPercent -= lengthPercent;
        }
        return target;

    }

    private static Coordinate pointOnSegment(StreetSegment seg, double lengthFraction)
    {
        double dy = seg.getTo().y - seg.getFrom().y;
        double dx = seg.getTo().x - seg.getFrom().x;

        double targetDy = dy * lengthFraction;
        double targetDx = dx * lengthFraction;

        return new Coordinate(seg.getFrom().x + targetDx, seg.getFrom().y + targetDy);
    }

    public static void findPoint(AddressStruct addStruct) throws Exception
    {
        WKTReader wkt = new WKTReader();
        MultiLineString geo = (MultiLineString) wkt.read(addStruct.get(AddColumns.GEO));
        Coordinate[] vertices = geo.getGeometryN(0).getCoordinates();

        // U.log("Left :" + addStruct.get(AddColumns.LFROMADD) + " - " +
        // addStruct.get(AddColumns.LTOADD));
        // U.log("Right :" + addStruct.get(AddColumns.RFROMADD) + " - " +
        // addStruct.get(AddColumns.RTOADD));

        int lfrom = 0;

        if (addStruct.contains(AddColumns.LFROMADD))
            lfrom = parseInt(addStruct.get(AddColumns.LFROMADD));

        int lto = lfrom + 1;
        if (addStruct.contains(AddColumns.LTOADD))
            lto = parseInt(addStruct.get(AddColumns.LTOADD));

        int rfrom = lfrom;
        if (addStruct.contains(AddColumns.RFROMADD))
            rfrom = parseInt(addStruct.get(AddColumns.RFROMADD));

        int rto = rfrom + 1;
        if (addStruct.contains(AddColumns.RTOADD))
            lto = parseInt(addStruct.get(AddColumns.RTOADD));

        if (lfrom == 0 && lto == 0)
        {
            lfrom = rfrom;
            lto = rto;
        }

        Coordinate foundPt = null;
        if (StrUtil.isEmpty(addStruct.getHouseNumber()))
        {

            foundPt = vertices[vertices.length/2];
        } else
        {
            int hn = parseInt(addStruct.getHouseNumber());

            foundPt = houseNumberOnStreetSegments(vertices, lfrom, lto, rfrom, rto, hn);

        }
        addStruct.put(AddColumns.GEO, foundPt);
        addStruct.longitude = foundPt.x;
        addStruct.latitude = foundPt.y;
    }


    private static int parseInt(String hn1)
    {
        hn1 = hn1.replaceAll("[^\\d]", "");
        if (StrUtil.isEmpty(hn1))
        {
            return 0;
        } else
        {
            return Integer.parseInt(hn1);
        }

    }

    
    
    
    
    private static int getMin(String hn1, String hn2, String hn3, String hn4)
    {
        // U.log("hn1:"+hn1);
        int ihn1 = parseInt(hn1);
        int ihn2 = parseInt(hn2);
        int ihn3 = parseInt(hn3);
        int ihn4 = parseInt(hn4);

        int min = Math.min(Math.min(Math.min(ihn1, ihn2), ihn3), ihn4);

        if (min == 0)
            min = Integer.MAX_VALUE;

        return min;
    }

    
    private static int getMax(String hn1, String hn2, String hn3, String hn4)
    {
        int ihn1 = parseInt(hn1);
        int ihn2 = parseInt(hn2);
        int ihn3 = parseInt(hn3);
        int ihn4 = parseInt(hn4);

        int max = Math.max(Math.max(Math.max(ihn1, ihn2), ihn3), ihn4);

        if (max == 0)
            max = Integer.MIN_VALUE;
        return max;
    }

    private  static int parseFromHN(String hn)
    {
        if (!StrUtil.isNum(hn))
        {
            String[] arr = hn.split("[^\\d]");
            int min = Integer.MAX_VALUE;
            for (String a : arr)
            {
                if (a.length() == 0)
                    continue;
                min = Math.min(min, Integer.parseInt(a));
            }
            return min;
        }
        return Integer.parseInt(hn);
    }

    private static int parseToHN(String hn)
    {
        if (!StrUtil.isNum(hn))
        {
            String[] arr = hn.split("[^\\d]");
            int max = Integer.MIN_VALUE;
            for (String a : arr)
            {
                max = Math.max(max, Integer.parseInt(a));
            }
            return max;
        }
        return Integer.parseInt(hn);
    }
}