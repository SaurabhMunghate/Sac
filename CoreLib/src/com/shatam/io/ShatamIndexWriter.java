package com.shatam.io;

import java.io.File;

import com.shatam.data.USPSAliasStreetModel;
import com.shatam.model.AddColumns;
import com.shatam.model.AddressStruct;
import com.shatam.shatamindex.document.Document;
import com.shatam.shatamindex.document.Field;
import com.shatam.shatamindex.index.IndexWriter;
import com.shatam.shatamindex.index.IndexWriterConfig;
import com.shatam.shatamindex.store.FSDirectory;
import com.shatam.shatamindex.util.Version;
import com.shatam.util.FileUtil;
import com.shatam.util.U;

public class ShatamIndexWriter
{
    IndexWriterConfig iwc;
    IndexWriter       indexWriter;

    public ShatamIndexWriter(String state, final String dataSource) throws Exception
    {
        File shatamIndexPath = ShatamIndexUtil.createIndexPath(state, dataSource);
        if (shatamIndexPath.exists())
        {
            //FileUtil.deleteDir(shatamIndexPath);
        }
        shatamIndexPath.mkdirs();

        iwc = new IndexWriterConfig(Version.SHATAM_35, ShatamIndexUtil.getAnalyzer());
        indexWriter = new IndexWriter(FSDirectory.open(shatamIndexPath), iwc);
    }
 
    
    //private void addDoc(String k1, String k2, AddressStruct addStruct) throws CorruptIndexException, IOException
    public void write(AddressStruct addStruct) throws Exception
    {

        Document doc = new Document();

        // ---- index
        for (AbstractIndexType it : AbstractIndexType.TYPES){
            String query = it.buildQuery(addStruct);
            doc.add(new Field(it.getFieldName(), query, Field.Store.YES, Field.Index.ANALYZED));
        }
        //doc.add(new Field("k1", k1, Field.Store.YES, Field.Index.ANALYZED));
        //doc.add(new Field("k2", k2, Field.Store.YES, Field.Index.ANALYZED));

        // --- non index -----
        for (AddColumns col : AddColumns.values())
        {
            String v = addStruct.get(col);
            
            //U.log("col:" + col + "  :" + v);
            
            if (v == null)
                U.log("col:" + col + "  :" + v);
            doc.add(new Field(col.name(), v, Field.Store.YES, Field.Index.NO));
        }

        // U.log("k1: " + k1);
        // U.log("k2: " + k2);
        indexWriter.addDocument(doc);

    }//write

    public void close() throws Exception
    {
        indexWriter.close();
    }


    public void write(USPSAliasStreetModel aliasRec, final AddressStruct realStructWithCity, final String dataSource) throws Exception
    {
        AddressStruct aliasForQuery = aliasRec.createAddressStruct(realStructWithCity.getState(), realStructWithCity.get(AddColumns.CITY));
        U.log("\t Writing ALIAS: " + AbstractIndexType.NORMAL.buildQuery(aliasForQuery));

        
        Document doc = new Document();

        // ---- index
        for (AbstractIndexType it : AbstractIndexType.TYPES){
            String query = it.buildQuery(aliasForQuery);
            doc.add(new Field(it.getFieldName(), query, Field.Store.YES, Field.Index.ANALYZED));
        } 

        // --- non index -----
        for (AddColumns col : AddColumns.values())
        {
            String v = realStructWithCity.get(col);
            
            //U.log("col:" + col + "  :" + v);
            
            if (v == null)
                U.log("col:" + col + "  :" + v);
            doc.add(new Field(col.name(), v, Field.Store.YES, Field.Index.NO));
        }

        doc.add(new Field("DATASOURCE", dataSource, Field.Store.YES, Field.Index.NO));
        
        // U.log("k1: " + k1);
        // U.log("k2: " + k2);
        indexWriter.addDocument(doc);
        
    }

}
