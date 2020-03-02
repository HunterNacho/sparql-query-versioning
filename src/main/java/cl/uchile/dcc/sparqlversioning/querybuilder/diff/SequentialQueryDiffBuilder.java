package cl.uchile.dcc.sparqlversioning.querybuilder.diff;

import cl.uchile.dcc.sparqlversioning.querybuilder.SequentialVersionedQueryBuilder;
import cl.uchile.dcc.sparqlversioning.transform.DiffTransformBase;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.Transformer;

import java.util.ArrayList;
import java.util.Collection;

public class SequentialQueryDiffBuilder extends SequentialVersionedQueryBuilder implements IQueryDiffBuilder{

    public SequentialQueryDiffBuilder(Query query, int targetVersion, boolean baseOnLast) {
        super(query, targetVersion, baseOnLast);
    }

    @Override
    public Op processQuery(Op compiledQuery) {
        if (targetIndex == 0)
            return super.processQuery(compiledQuery);
        return processDiffQuery(compiledQuery, this);
    }

    @Override
    protected Collection<String> getFrom() {
        if (targetIndex == 0)
            return super.getFrom();
        ArrayList<String> from = new ArrayList<>(super.getFrom());
        from.add("FROM NAMED <http://wikidata.org/" + baseVersion + ">");
        return from;
    }

    @Override
    public Op processPrevious(Op compiledQuery) {
        return Transformer.transform(new PrevSequentialTransform(), compiledQuery);
    }

    @Override
    public Op processCurrent(Op compiledQuery) {
        return super.processQuery(compiledQuery);
    }

    private class PrevSequentialTransform extends DiffTransformBase {

        @Override
        protected Op processTriple(Triple triple) {
            return createUnionMinusChain(triple, targetIndex - 1);
        }
    }

}
