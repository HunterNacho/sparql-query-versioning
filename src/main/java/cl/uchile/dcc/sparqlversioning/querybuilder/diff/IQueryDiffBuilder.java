package cl.uchile.dcc.sparqlversioning.querybuilder.diff;

import org.apache.jena.sparql.algebra.Op;

public interface IQueryDiffBuilder {
    Op processPrevious(Op compiledQuery);
    Op processCurrent(Op compiledQuery);
}
