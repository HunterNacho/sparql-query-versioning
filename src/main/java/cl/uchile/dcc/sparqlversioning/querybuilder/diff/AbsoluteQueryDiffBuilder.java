package cl.uchile.dcc.sparqlversioning.querybuilder.diff;

import cl.uchile.dcc.sparqlversioning.querybuilder.AbsoluteVersionedQueryBuilder;
import cl.uchile.dcc.sparqlversioning.transform.DiffTransformBase;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.Transformer;
import org.apache.jena.sparql.algebra.op.*;

import java.util.ArrayList;
import java.util.Collection;

public class AbsoluteQueryDiffBuilder extends AbsoluteVersionedQueryBuilder implements IQueryDiffBuilder{

    public AbsoluteQueryDiffBuilder(Query query, int targetVersion, boolean baseOnLast) {
        super(query, targetVersion, baseOnLast);
    }

    @Override
    public Op processQuery(Op compiledQuery){
        if (targetIndex == 0)
            return compiledQuery;
        return processDiffQuery(compiledQuery, this);
    }

    @Override
    protected Collection<String> getFrom() {
        String namedDeltas = "FROM NAMED <http://wikidata.org/deltas/";
        if (targetIndex == 0)
            return super.getFrom();
        ArrayList<String> from = new ArrayList<>(super.getFrom());
        from.add("FROM NAMED <http://wikidata.org/" + baseVersion + ">");
        from.add(namedDeltas + VERSIONS[targetIndex - 1] + "-" + baseVersion + ">");
        from.add(namedDeltas + baseVersion + "-" + VERSIONS[targetIndex - 1] + ">");
        return from;
    }

    @Override
    public Op processPrevious(Op compiledQuery) {
        return Transformer.transform(new PrevAbsoluteTransform(), compiledQuery);
    }

    @Override
    public Op processCurrent(Op compiledQuery) {
        return super.processQuery(compiledQuery);
    }

    private class PrevAbsoluteTransform extends DiffTransformBase {
        @Override
        protected Op processTriple(Triple triple) {
            OpTriple opTriple = new OpTriple(triple);
            Node baseGraph = NodeFactory.createURI("http://wikidata.org/" + baseVersion);
            Node addGraph = NodeFactory.createURI("http://wikidata.org/deltas/" + VERSIONS[targetIndex - 1] + "-" + baseVersion);
            Node subGraph = NodeFactory.createURI("http://wikidata.org/deltas/" + baseVersion + "-" + VERSIONS[targetIndex - 1]);
            return OpMinus.create(
                    new OpUnion(
                            new OpGraph(baseGraph, opTriple),
                            new OpGraph(addGraph, opTriple)),
                    new OpGraph(subGraph, opTriple)
            );
        }
    }
}

