package cl.uchile.dcc.sparqlversioning.querybuilder.diff;

import cl.uchile.dcc.sparqlversioning.querybuilder.MetadataSequentialVersionedQueryBuilder;
import org.apache.jena.query.Query;
import org.apache.jena.sparql.algebra.Op;

import java.io.IOException;


public class MetadataSequentialQueryDiffBuilder extends MetadataSequentialVersionedQueryBuilder implements IQueryDiffBuilder{
    private MetadataSequentialVersionedQueryBuilder previousBuilder;

    public MetadataSequentialQueryDiffBuilder(Query query, int targetVersion, boolean baseOnLast) {
        super(query, targetVersion, baseOnLast, "_curr");
        if (targetVersion > 0)
            previousBuilder = new MetadataSequentialVersionedQueryBuilder(query, targetVersion - 1, baseOnLast, "_prev");
    }

    @Override
    public Op processQuery(Op compiledQuery) {
        return super.processQuery(compiledQuery);
    }

    @Override
    public void writeQueryToFile(String filename) throws IOException {
        String name = filename.substring(0, filename.lastIndexOf("."));
        String extension = filename.substring(filename.lastIndexOf("."));
        super.writeQueryToFile(name + "_curr" + extension);
        previousBuilder.writeQueryToFile(name + "_prev" + extension);
    }

    @Override
    public Op processPrevious(Op compiledQuery) {
        return previousBuilder.processQuery(compiledQuery);
    }

    @Override
    public Op processCurrent(Op compiledQuery) {
        return super.processQuery(compiledQuery);
    }
}
