/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.test;

import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class BuildFeature {

	SimpleFeatureCollection collection = FeatureCollections.newCollection();

	static GeometryFactory geometryFactory = JTSFactoryFinder
			.getGeometryFactory(null);

	public static SimpleFeature mainFeature() throws Exception {

		SimpleFeatureType TYPE = DataUtilities.createType("_GEOMETRY",
				"location:Point");
		SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(TYPE);

		Coordinate[] coor = new Coordinate[] {
				new Coordinate(-109.483857, 34.300252),
				new Coordinate(-109.465203, 34.300101) };
		Point point = geometryFactory.createPoint(new Coordinate(-109.483857,
				34.300252));

		featureBuilder.add(point);

		SimpleFeature feature = featureBuilder.buildFeature(null);
		return feature;
	}
}
