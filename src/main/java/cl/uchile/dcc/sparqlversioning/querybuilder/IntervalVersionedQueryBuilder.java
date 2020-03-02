package cl.uchile.dcc.sparqlversioning.querybuilder;

import org.apache.jena.query.Query;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;

import java.util.ArrayList;
import java.util.Collection;

public class IntervalVersionedQueryBuilder extends AbstractVersionedQueryBuilder {

    public IntervalVersionedQueryBuilder(Query query, int targetVersion) {
        super(query, targetVersion, false);
    }

    protected Collection<String> getFrom() {
        ArrayList<String> from = new ArrayList<>();
        for (int i = 0; i <= targetIndex; i++) {
            for (int j = targetIndex; j < VERSIONS.length; j++) {
                from.add("FROM <http://wikidata.org/intervals/" + VERSIONS[i] + "-" + VERSIONS[j] + ">");
            }
        }
        return from;
    }
}
