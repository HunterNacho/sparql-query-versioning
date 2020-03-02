package cl.uchile.dcc.sparqlversioning.querybuilder;

import org.apache.jena.query.Query;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import java.util.Collection;
import java.util.Collections;

public class BaselineVersionedQueryBuilder extends AbstractVersionedQueryBuilder {

    public BaselineVersionedQueryBuilder(Query query, int targetVersion) {
        super(query, targetVersion, false);
    }

    protected Collection<String> getFrom() {
        return Collections.singletonList("FROM <http://wikidata.org/" + targetVersion + ">");
    }
}
