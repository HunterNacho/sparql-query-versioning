package cl.uchile.dcc.sparqlversioning;

import cl.uchile.dcc.sparqlversioning.querybuilder.*;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;

import java.io.File;
import java.io.IOException;

public class GeneralQueryVersioner {
    private static final String QUERY_FOLDER = "queries/";
    private static final String[] OUTPUT_FOLDERS = {
            "baseline/", "intervals/", "absolute-0/", "absolute-n/",
            "sequential-0/", "sequential-n/"
    };
    private static final String[] QUERY_NAMES = {"last", "first", "middle"};
    private static final int LENGTH = OUTPUT_FOLDERS.length;

    public static void main(String[] args) throws IOException{
        File folder = new File(QUERY_FOLDER);
        assert folder.isDirectory();
        File[] files = folder.listFiles();
        assert files != null;
        for (File file : files) {
            if (file.isDirectory()) {
                continue;
            }
            System.out.println("Processing " + file.getName());
            Query query;
            try {
               query = QueryFactory.read("file:" + file.getAbsolutePath());
            }
            catch (Exception e) {
                System.out.println("Skipping file");
                System.out.println(e.getLocalizedMessage());
                continue;
            }
            IVersionedQueryBuilder[] queryBuilders = {
                    new BaselineVersionedQueryBuilder(query, 7),
                    new IntervalVersionedQueryBuilder(query, 7),
                    new AbsoluteVersionedQueryBuilder(query, 7, false),
                    new AbsoluteVersionedQueryBuilder(query, 7, true),
                    new MetadataSequentialVersionedQueryBuilder(query, 7, false),
                    new MetadataSequentialVersionedQueryBuilder(query, 7, true),

                    new BaselineVersionedQueryBuilder(query, 0),
                    new IntervalVersionedQueryBuilder(query, 0),
                    new AbsoluteVersionedQueryBuilder(query, 0, false),
                    new AbsoluteVersionedQueryBuilder(query, 0, true),
                    new MetadataSequentialVersionedQueryBuilder(query, 0, false),
                    new MetadataSequentialVersionedQueryBuilder(query, 0, true),

                    new BaselineVersionedQueryBuilder(query, 4),
                    new IntervalVersionedQueryBuilder(query, 4),
                    new AbsoluteVersionedQueryBuilder(query, 4, false),
                    new AbsoluteVersionedQueryBuilder(query, 4, true),
                    new MetadataSequentialVersionedQueryBuilder(query, 4, false),
                    new MetadataSequentialVersionedQueryBuilder(query, 4, true)
            };
            for (int i = 0; i < queryBuilders.length; i++) {
                queryBuilders[i].writeQueryToFile(
                        QUERY_FOLDER
                                + OUTPUT_FOLDERS[i % LENGTH]
                                + QUERY_NAMES[i / LENGTH] + "/"
                                + file.getName()
                );
            }
        }
    }
}
