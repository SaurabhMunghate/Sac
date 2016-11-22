/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.shatamindex.search.spans;



import java.io.IOException;


import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;



import com.shatam.shatamindex.index.IndexReader;
import com.shatam.shatamindex.index.Term;
import com.shatam.shatamindex.search.Query;
import com.shatam.shatamindex.util.ToStringUtils;


public class SpanNearQuery extends SpanQuery implements Cloneable {
  protected List<SpanQuery> clauses;
  protected int slop;
  protected boolean inOrder;

  protected String field;
  private boolean collectPayloads;

  
  public SpanNearQuery(SpanQuery[] clauses, int slop, boolean inOrder) {
    this(clauses, slop, inOrder, true);     
  }
  
  public SpanNearQuery(SpanQuery[] clauses, int slop, boolean inOrder, boolean collectPayloads) {

    
    this.clauses = new ArrayList<SpanQuery>(clauses.length);
    for (int i = 0; i < clauses.length; i++) {
      SpanQuery clause = clauses[i];
      if (i == 0) {                               
        field = clause.getField();
      } else if (!clause.getField().equals(field)) {
        throw new IllegalArgumentException("Clauses must have same field.");
      }
      this.clauses.add(clause);
    }
    this.collectPayloads = collectPayloads;
    this.slop = slop;
    this.inOrder = inOrder;
  }

  
  public SpanQuery[] getClauses() {
    return clauses.toArray(new SpanQuery[clauses.size()]);
  }

  
  public int getSlop() { return slop; }

  
  public boolean isInOrder() { return inOrder; }

  @Override
  public String getField() { return field; }
  
  @Override
  public void extractTerms(Set<Term> terms) {
	    for (final SpanQuery clause : clauses) {
	      clause.extractTerms(terms);
	    }
  }  
  

  @Override
  public String toString(String field) {
    StringBuilder buffer = new StringBuilder();
    buffer.append("spanNear([");
    Iterator<SpanQuery> i = clauses.iterator();
    while (i.hasNext()) {
      SpanQuery clause = i.next();
      buffer.append(clause.toString(field));
      if (i.hasNext()) {
        buffer.append(", ");
      }
    }
    buffer.append("], ");
    buffer.append(slop);
    buffer.append(", ");
    buffer.append(inOrder);
    buffer.append(")");
    buffer.append(ToStringUtils.boost(getBoost()));
    return buffer.toString();
  }

  @Override
  public Spans getSpans(final IndexReader reader) throws IOException {
    if (clauses.size() == 0)                      
      return new SpanOrQuery(getClauses()).getSpans(reader);

    if (clauses.size() == 1)                      
      return clauses.get(0).getSpans(reader);

    return inOrder
            ? (Spans) new NearSpansOrdered(this, reader, collectPayloads)
            : (Spans) new NearSpansUnordered(this, reader);
  }

  @Override
  public Query rewrite(IndexReader reader) throws IOException {
    SpanNearQuery clone = null;
    for (int i = 0 ; i < clauses.size(); i++) {
      SpanQuery c = clauses.get(i);
      SpanQuery query = (SpanQuery) c.rewrite(reader);
      if (query != c) {                     
        if (clone == null)
          clone = (SpanNearQuery) this.clone();
        clone.clauses.set(i,query);
      }
    }
    if (clone != null) {
      return clone;                        
    } else {
      return this;                         
    }
  }
  
  @Override
  public Object clone() {
    int sz = clauses.size();
    SpanQuery[] newClauses = new SpanQuery[sz];

    for (int i = 0; i < sz; i++) {
      newClauses[i] = (SpanQuery) clauses.get(i).clone();
    }
    SpanNearQuery spanNearQuery = new SpanNearQuery(newClauses, slop, inOrder);
    spanNearQuery.setBoost(getBoost());
    return spanNearQuery;
  }

  
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof SpanNearQuery)) return false;

    final SpanNearQuery spanNearQuery = (SpanNearQuery) o;

    if (inOrder != spanNearQuery.inOrder) return false;
    if (slop != spanNearQuery.slop) return false;
    if (!clauses.equals(spanNearQuery.clauses)) return false;

    return getBoost() == spanNearQuery.getBoost();
  }

  @Override
  public int hashCode() {
    int result;
    result = clauses.hashCode();
    
    
    
    result ^= (result << 14) | (result >>> 19);  
    result += Float.floatToRawIntBits(getBoost());
    result += slop;
    result ^= (inOrder ? 0x99AFD3BD : 0);
    return result;
  }
}
