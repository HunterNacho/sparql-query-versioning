package cl.uchile.dcc.sparqlversioning;

import cl.uchile.dcc.sparqlversioning.querybuilder.IVersionedQueryBuilder;
import cl.uchile.dcc.sparqlversioning.querybuilder.diff.*;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class GeneralQueryDiffGenerator {
    private static final String QUERY_FOLDER = "queries/";
    private static final String[] OUTPUT_FOLDERS = {
            "baseline/", "intervals/", "absolute-0/", "absolute-n/",
            "sequential-0/", "sequential-n/"
    };
    private static final String[] QUERY_NAMES = {"last", "second", "middle"};
    private static final int LENGTH = OUTPUT_FOLDERS.length;

    public static void main(String[] args) throws IOException{
        File folder = new File(QUERY_FOLDER);
        assert folder.isDirectory();
        File[] files = folder.listFiles();
        assert files != null;
        Arrays.sort(files);
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
                    new BaselineQueryDiffBuilder(query, 7),
                    new IntervalQueryDiffBuilder(query, 7),
                    new AbsoluteQueryDiffBuilder(query, 7, false),
                    new AbsoluteQueryDiffBuilder(query, 7, true),
                    new MetadataSequentialQueryDiffBuilder(query, 7, false),
                    new MetadataSequentialQueryDiffBuilder(query, 7, true),

                    new BaselineQueryDiffBuilder(query, 1),
                    new IntervalQueryDiffBuilder(query, 1),
                    new AbsoluteQueryDiffBuilder(query, 1, false),
                    new AbsoluteQueryDiffBuilder(query, 1, true),
                    new MetadataSequentialQueryDiffBuilder(query, 1, false),
                    new MetadataSequentialQueryDiffBuilder(query, 1, true),

                    new BaselineQueryDiffBuilder(query, 4),
                    new IntervalQueryDiffBuilder(query, 4),
                    new AbsoluteQueryDiffBuilder(query, 4, false),
                    new AbsoluteQueryDiffBuilder(query, 4, true),
                    new MetadataSequentialQueryDiffBuilder(query, 4, false),
                    new MetadataSequentialQueryDiffBuilder(query, 4, true)
            };
            for (int i = 0; i < queryBuilders.length; i++) {
                queryBuilders[i].writeQueryToFile(
                        QUERY_FOLDER
                                + "diff/"
                                + OUTPUT_FOLDERS[i % LENGTH]
                                + QUERY_NAMES[i / LENGTH] + "/"
                                + file.getName()
                );
            }
        }
    }
}
