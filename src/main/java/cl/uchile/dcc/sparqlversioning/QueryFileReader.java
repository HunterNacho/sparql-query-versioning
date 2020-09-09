package cl.uchile.dcc.sparqlversioning;

import cl.uchile.dcc.sparqlversioning.querybuilder.*;
import cl.uchile.dcc.sparqlversioning.querybuilder.diff.*;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;

import java.io.IOException;

public class QueryFileReader {
    public static void main(String[] args) throws IOException {
        if (args.length < 6) {
            System.err.println("Usage: QueryFileReader <input_filename> <base_version (0|n)> <target_version> <delta_type (abs|seq|int)> <results (all|new)> <output_filename>");
            System.exit(1);
            return;
        }
        String input = args[0];
        String output = args[5];
        Query query = QueryFactory.read("file:" + input);
        IVersionedQueryBuilder versionedQueryBuilder;
        int target = Integer.parseInt(args[2]);
        boolean baseOnLast = args[1].startsWith("n");
        boolean onlyNew = args[4].startsWith("n");
        /*if (args[3].startsWith("s")) { // Deprecated
            if (onlyNew)
                versionedQueryBuilder = new SequentialQueryDiffBuilder(query, target, baseOnLast);
            else
                versionedQueryBuilder = new SequentialVersionedQueryBuilder(query, target, baseOnLast);
        }
        else */
        if (args[3].startsWith("s")) {
            if (onlyNew)
                versionedQueryBuilder = new MetadataSequentialQueryDiffBuilder(query, target, baseOnLast);
            else
                versionedQueryBuilder = new MetadataSequentialVersionedQueryBuilder(query, target, baseOnLast);
        }
        else if (args[3].startsWith("i")) {
            if (onlyNew)
                versionedQueryBuilder = new IntervalQueryDiffBuilder(query, target);
            else
                versionedQueryBuilder = new IntervalVersionedQueryBuilder(query, target);
        }
        else if (args[3].startsWith("a")) {
            if (onlyNew)
                versionedQueryBuilder = new AbsoluteQueryDiffBuilder(query, target, baseOnLast);
            else
                versionedQueryBuilder = new AbsoluteVersionedQueryBuilder(query, target, baseOnLast);
        }
        else {
            if (onlyNew)
                versionedQueryBuilder = new BaselineQueryDiffBuilder(query, target);
            else
                versionedQueryBuilder = new BaselineVersionedQueryBuilder(query, target);
        }
        versionedQueryBuilder.writeQueryToFile(output);
    }
}
