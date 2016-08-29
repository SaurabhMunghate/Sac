package com.shatam.geo;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Observer;

import org.geotools.data.FeatureEvent;
import org.geotools.data.FeatureListener;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShpFiles;
import org.geotools.data.shapefile.shp.ShapefileReader;
import org.geotools.data.shapefile.shp.ShapefileReader.Record;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.algorithm.ConvexHull;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.index.ItemVisitor;
import com.vividsolutions.jts.index.quadtree.Quadtree;

public class ReadShapeFile
{
    private Quadtree           quad = new Quadtree();
    private ShapefileDataStore sfds;
    private String keepField; 

    public ArrayList<String> query(final Object oGeo)
    {
        final Geometry geo = (Geometry)oGeo;
        final ArrayList<String> arr = new ArrayList<String>();
        quad.query(geo.getEnvelopeInternal(), new ItemVisitor() {

            @Override
            public void visitItem(Object arrObj)
            {
                Geometry geo2 = (Geometry)((Object [])arrObj)[0];
                
                if (geo.intersects(geo2))
                {
                    String zip = (String)((Object [])arrObj)[1];
                    int index = Integer.parseInt( ((Object [])arrObj)[1].toString());
                    arr.add(zip);
                }
            }
        });

        return arr;

    }// query()

    
    
    public static void iterate(ArrayList<File> files, String [] getFields, Observer observer) throws Exception
    {
        File file =  null;
        for (File f: files){
            if (f.getName().endsWith("shp")){
                file = f;
                break;
            }
        }

        
        ShapefileDataStore sfds = new ShapefileDataStore(file.toURL());
        SimpleFeatureIterator itr = sfds.getFeatureSource().getFeatures().features();
        int i = 0;
        HashMap<String, Object> map = new HashMap<String, Object>();
        while (itr.hasNext())
        {
            SimpleFeature feature = itr.next();
            Geometry geo = (Geometry) feature.getDefaultGeometry();
            map.put("_GEOMETRY", geo);
            map.put("_ROW", i);
            i++;
            
            for (String field: getFields){
                map.put(field, feature.getAttribute(field)); 
            }//for 
            
            observer.update(null, map);
        }//while
    }
    
    public ReadShapeFile(File file, String keepField ) throws Exception
    {
        this.keepField = keepField;

        sfds = new ShapefileDataStore(file.toURL());

        SimpleFeatureIterator itr = sfds.getFeatureSource().getFeatures().features();

        int i = 0;
        while (itr.hasNext())
        {
            SimpleFeature feature = itr.next();
            Geometry geo = (Geometry) feature.getDefaultGeometry();
            Envelope env = geo.getEnvelopeInternal();
            Geometry convexHullGeo = (new ConvexHull(geo)).getConvexHull();
            quad.insert(env, new Object[]{convexHullGeo, feature.getAttribute(this.keepField), ""+i });

            i++;
            // for (String name: keepFields){
            // feature.getAttribute(name);
            // }
        }

        // f = new ShapefileReader(new ShpFiles(shp), true, true, new
        // GeometryFactory());

    }// ReadShapeFile


}
