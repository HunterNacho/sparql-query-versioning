package cl.uchile.dcc.sparqlversioning.querybuilder;

import java.io.IOException;

public interface IVersionedQueryBuilder {
    void writeQueryToFile(String filename) throws IOException;
}
