package cl.uchile.dcc.sparqlversioning.querybuilder.diff;

import cl.uchile.dcc.sparqlversioning.querybuilder.AbstractVersionedQueryBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Query;
import org.apache.jena.sparql.algebra.*;
import org.apache.jena.sparql.algebra.op.OpGraph;

import java.util.*;

public class BaselineQueryDiffBuilder extends AbstractVersionedQueryBuilder implements IQueryDiffBuilder{

    public BaselineQueryDiffBuilder(Query query, int targetVersion) {
        super(query, targetVersion, false);
    }

    @Override
    public Op processQuery(Op compiledQuery) {
        if (targetIndex == 0)
            return compiledQuery;
        return processDiffQuery(compiledQuery, this);
    }

    protected Collection<String> getFrom() {
        ArrayList<String> from = new ArrayList<>();
        from.add("FROM <http://wikidata.org/" + targetVersion + ">");
        if (targetIndex > 0) {
            from.add("FROM NAMED <http://wikidata.org/" + VERSIONS[targetIndex - 1] + ">");
        }
        return from;
    }

    @Override
    public Op processPrevious(Op compiledQuery) {
        Node previousGraph = NodeFactory.createURI("http://wikidata.org/" + VERSIONS[targetIndex - 1]);
        return new OpGraph(previousGraph, compiledQuery);
    }

    @Override
    public Op processCurrent(Op compiledQuery) {
        return compiledQuery;
    }
}
