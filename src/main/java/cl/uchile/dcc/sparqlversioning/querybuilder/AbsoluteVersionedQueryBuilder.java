package cl.uchile.dcc.sparqlversioning.querybuilder;

import cl.uchile.dcc.sparqlversioning.transform.VersionedTransformBase;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.sparql.algebra.*;
import org.apache.jena.sparql.algebra.op.*;
import org.apache.jena.sparql.core.BasicPattern;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class AbsoluteVersionedQueryBuilder extends AbstractVersionedQueryBuilder {

    public AbsoluteVersionedQueryBuilder(Query query, int targetVersion, boolean baseOnLast) {
        super(query, targetVersion, baseOnLast);
    }

    @Override
    public Op processQuery(Op compiledQuery) {
        if (targetEqualsBase)
            return compiledQuery;
        Transform transform = new AbsoluteTransform();
        return Transformer.transform(transform, compiledQuery);
    }

    @Override
    protected Collection<String> getFrom() {
        if (targetEqualsBase)
            return Collections.singletonList("FROM <http://wikidata.org/" + baseVersion + ">");
        String[] from = new String[3];
        from[0] = "FROM <http://wikidata.org/" + baseVersion + ">";
        from[1] = "FROM <http://wikidata.org/deltas/" + targetVersion + "-" + baseVersion + ">";
        from[2] = "FROM NAMED <http://wikidata.org/deltas/" + baseVersion + "-" + targetVersion + ">";
        return Arrays.asList(from);
    }

    class AbsoluteTransform extends VersionedTransformBase {

        @Override
        public Op transform(OpBGP opBGP) {
            BasicPattern pattern = opBGP.getPattern();
            Op op = opBGP;
            String graph = "http://wikidata.org/deltas/" + baseVersion + "-" + targetVersion;
            for (Triple triple : pattern) {
                op = OpMinus.create(op, new OpGraph(NodeFactory.createURI(graph), new OpTriple(triple)));
            }
            return op;
        }
    }
}
