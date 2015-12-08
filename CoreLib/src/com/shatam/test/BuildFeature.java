package com.shatam.test;

import javax.sound.sampled.Line;

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
import com.vividsolutions.jts.geom.Polygon;

public class BuildFeature {
	/*
	 * We create a FeatureCollection into which we will put each Feature created
	 * from a record in the input csv data file
	 */
	SimpleFeatureCollection collection = FeatureCollections.newCollection();
	/*
	 * GeometryFactory will be used to create the geometry attribute of each
	 * feature (a Point object for the location)
	 */
	static GeometryFactory geometryFactory = JTSFactoryFinder
			.getGeometryFactory(null);

	public static SimpleFeature mainFeature() throws Exception {

		SimpleFeatureType TYPE = DataUtilities.createType("_GEOMETRY",
				"location:Point");
		SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(TYPE);
		// Line point = geometryFactory.createLineString(new Coordinate)
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
