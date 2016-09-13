/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.test;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;

import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.opengis.feature.simple.SimpleFeature;

import com.shatam.data.ZipCodes;
import com.shatam.util.StrUtil;
import com.shatam.util.U;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateFilter;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.CoordinateSequenceComparator;
import com.vividsolutions.jts.geom.CoordinateSequenceFactory;
import com.vividsolutions.jts.geom.CoordinateSequenceFilter;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryComponentFilter;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.GeometryFilter;
import com.vividsolutions.jts.io.WKTReader;

class skc {
}

public class ReadShapeFile {

	GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory(null);

	public static void main(String args[]) throws Exception {

		File file = new File(
				"C:\\Users\\Apex\\WorksBuilder\\Data\\edges\\04\\tl_2010_04001_edges\\tl_2010_04001_edges.shp");
		ShapefileDataStore sfds = new ShapefileDataStore(file.toURL());
		SimpleFeatureIterator itr = sfds.getFeatureSource().getFeatures()
				.features();
		int i = 0;
		HashMap<String, Object> map = new HashMap<String, Object>();
		int counter = 0;
		ArrayList<String> zipCodes = new ArrayList<String>();
		while (itr.hasNext()) {
			SimpleFeature feature = itr.next();

			Geometry geo = (Geometry) feature.getDefaultGeometry();
			Geometry g1 = new WKTReader()
					.read("LINESTRING (-109.1609527 35.9385926 , -109.1595589 35.9389133)");

			map.put("_GEOMETRY", geo);
			map.put("_ROW", i);
			i++;
			String latS = "((-109.483857 34.300252, -109.465203 34.300101))";

			U.log("_Geometry : " + geo);

			Object[] geoObj = new Object[4];

			Geometry buildGeo = (Geometry) BuildFeature.mainFeature()
					.getDefaultGeometry();

			zipCodes = ZipCodes.getZips(buildGeo);

			break;

		}

		for (String str : zipCodes) {

			for (String city : ZipCodes.getCity(str)) {
				U.log(city);
			}
		}

	}
}
