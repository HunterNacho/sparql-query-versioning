package cl.uchile.dcc.sparqlversioning.querybuilder;

import cl.uchile.dcc.sparqlversioning.transform.VersionedTransformBase;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.Transformer;
import org.apache.jena.sparql.algebra.op.*;
import org.apache.jena.sparql.core.BasicPattern;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class SequentialVersionedQueryBuilder extends AbstractVersionedQueryBuilder {

    final static String DELTA = "http://wikidata.org/deltas/";

    public SequentialVersionedQueryBuilder(Query query, int targetVersion, boolean baseOnLast) {
        super(query, targetVersion, baseOnLast);
    }

    @Override
    public Op processQuery(Op compiledQuery) {
        if (targetEqualsBase)
            return compiledQuery;
        return Transformer.transform(new SequentialTransform(), compiledQuery);

    }

    protected Collection<String> getFrom() {
        if (targetEqualsBase)
            return Collections.singletonList("FROM <http://wikidata.org/" + baseVersion + ">");
        ArrayList<String> from = new ArrayList<>();
        ArrayList<Integer> range = getDeltaRange();
        String previous = baseVersion;
        String current;
        from.add("FROM <http://wikidata.org/" + baseVersion + ">");
        for (int index = 1; index < range.size(); index++) {
            current = VERSIONS[range.get(index)];
            from.add("FROM NAMED <" + DELTA + current + "-" + previous + ">");
            from.add("FROM NAMED <" + DELTA + previous + "-" + current + ">");
            previous = current;
        }
        return from;
    }

    protected class SequentialTransform extends VersionedTransformBase {

        @Override
        public Op transform(OpBGP opBGP) {
            BasicPattern pattern = opBGP.getPattern();
            if (pattern.isEmpty())
                return opBGP;
            OpSequence opSequence = OpSequence.create();
            for (Triple triple : pattern) {
                opSequence.add(createUnionMinusChain(triple));
            }
            return opSequence;
        }
    }

    private Op createUnionMinusChain(Triple triple) {
        return createUnionMinusChain(triple, targetIndex);
    }

    protected Op createUnionMinusChain(Triple triple, int targetIndex) {
        Op op = new OpTriple(triple);
        OpTriple opTriple = new OpTriple(triple);
        if (this.targetIndex != targetIndex) {
            Node baseGraph = NodeFactory.createURI("http://wikidata.org/" + baseVersion);
            op = new OpGraph(baseGraph, op);
        }
        ArrayList<Integer> range = getDeltaRange(targetIndex);
        for (int i = 1; i < range.size(); i++) {
            String current = VERSIONS[range.get(i)];
            String previous = VERSIONS[range.get(i - 1)];
            Node positiveGraph = NodeFactory.createURI(DELTA + current + "-" + previous);
            Node negativeGraph = NodeFactory.createURI(DELTA + previous + "-" + current);
            op = OpUnion.create(op, new OpGraph(positiveGraph, opTriple));
            op = OpMinus.create(op, new OpGraph(negativeGraph, opTriple));
        }
        return op;
    }

    private ArrayList<Integer> getDeltaRange(int targetIndex) {
        ArrayList<Integer> list = new ArrayList<>();
        if (targetIndex == baseIndex)
            return list;
        if (baseIndex > targetIndex) {
            for (int i = baseIndex; i >= targetIndex; i--)
                list.add(i);
        }
        else {
            for (int i = baseIndex; i <= targetIndex; i++)
                list.add(i);
        }
        return list;
    }

    private ArrayList<Integer> getDeltaRange() {
        return getDeltaRange(targetIndex);
    }
}
