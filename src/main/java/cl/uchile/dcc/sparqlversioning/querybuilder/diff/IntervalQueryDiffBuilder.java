package cl.uchile.dcc.sparqlversioning.querybuilder.diff;

import cl.uchile.dcc.sparqlversioning.querybuilder.IntervalVersionedQueryBuilder;
import cl.uchile.dcc.sparqlversioning.transform.DiffTransformBase;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.Transformer;
import org.apache.jena.sparql.algebra.op.*;
import org.apache.jena.sparql.core.Var;

import java.util.ArrayList;
import java.util.Collection;

public class IntervalQueryDiffBuilder extends IntervalVersionedQueryBuilder implements IQueryDiffBuilder {

    public IntervalQueryDiffBuilder(Query query, int targetVersion) {
        super(query, targetVersion);
    }

    @Override
    public Op processQuery(Op compiledQuery) {
        if (targetIndex == 0)
            return compiledQuery;
        return processDiffQuery(compiledQuery, this);
    }

    protected Collection<String> getFrom() {
        if (targetIndex == 0)
            return super.getFrom();
        ArrayList<String> from = new ArrayList<>(super.getFrom());
        for (int i = 0; i < targetIndex; i++) {
            for (int j = targetIndex - 1; j < VERSIONS.length; j++) {
                from.add("FROM NAMED <http://wikidata.org/intervals/" + VERSIONS[i] + "-" + VERSIONS[j] + ">");
            }
        }
        return from;
    }

    @Override
    public Op processPrevious(Op compiledQuery) {
        return Transformer.transform(new PrevIntervalTransform(), compiledQuery);
    }

    @Override
    public Op processCurrent(Op compiledQuery) {
        return compiledQuery;
    }

    private class PrevIntervalTransform extends DiffTransformBase {

        private int graphVarId = 0;

        @Override
        protected Op processTriple(Triple triple) {
            Var graphVar = Var.alloc("g" + graphVarId++);
            return new OpGraph(graphVar, new OpTriple(triple));
        }

        @Override
        public Op handleComplexPath(OpPath opPath) {
            Var graphVar = Var.alloc("g" + graphVarId++);
            return new OpGraph(graphVar, opPath);
        }
    }
}
