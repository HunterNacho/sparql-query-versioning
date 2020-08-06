package cl.uchile.dcc.sparqlversioning.querybuilder;

import cl.uchile.dcc.sparqlversioning.querybuilder.diff.IQueryDiffBuilder;
import cl.uchile.dcc.sparqlversioning.transform.OperationReorderTransform;
import cl.uchile.dcc.sparqlversioning.transform.ProjectionVariablesExtractorTransform;
import org.apache.jena.query.Query;
import org.apache.jena.sparql.algebra.*;
import org.apache.jena.sparql.algebra.op.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public abstract class AbstractVersionedQueryBuilder implements IVersionedQueryBuilder {

    protected static final String[] VERSIONS = new String[] {
            "20170809",
            "20170816",
            "20170823",
            "20170830",
            "20170907",
            "20170913",
            "20170920",
            "20170927"
    };
    private static final String FIRST_VERSION = VERSIONS[0];
    private static final String LAST_VERSION = VERSIONS[VERSIONS.length - 1];

    private Query rawQuery;
    private Op processedQuery;
    protected final String targetVersion;
    protected final String baseVersion;
    final boolean targetEqualsBase;
    protected final int targetIndex;
    final int baseIndex;
    protected boolean forwards;

    public Op processQuery(Op compiledQuery) {
        return compiledQuery;
    }

    protected AbstractVersionedQueryBuilder(Query query, int targetVersion, boolean baseOnLast){
        this.targetVersion = VERSIONS[targetVersion];
        this.targetIndex = targetVersion;
        if (baseOnLast) {
            this.baseVersion = LAST_VERSION;
            this.baseIndex = VERSIONS.length - 1;
        }
        else {
            this.baseVersion = FIRST_VERSION;
            this.baseIndex = 0;
        }
        this.targetEqualsBase = baseVersion.equals(this.targetVersion);
        forwards = targetVersion >= baseIndex;
        rawQuery = query;
    }

    public void writeQueryToFile(String filename) throws IOException {
        ArrayList<String> queryLines = this.getQueryLines();
        BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
        for (String line : queryLines) {
            if (line.contains("# Empty BGP"))
                line = line.replace("# Empty BGP", "");
            writer.write(line);
            writer.newLine();
        }
        writer.close();
    }

    private ArrayList<String> getQueryLines() {
        processedQuery = this.processQuery(Algebra.compile(rawQuery));
        ArrayList<String> queryLines = new ArrayList<>();
        queryLines.addAll(Arrays.asList(this.toString().split("\n")));
        queryLines.addAll(queryLines.indexOf("WHERE"), this.getFrom());
        //noinspection SpellCheckingInspection
        queryLines.add(0, "SPARQL");
        queryLines.add(";");
        return queryLines;
    }

    protected abstract Collection<String> getFrom();

    @Override
    public String toString() {
        if (processedQuery == null)
            return rawQuery.toString();
        return OpAsQuery.asQuery(processedQuery).toString();
    }

    private Op getOpDiff(Op left, Op right) {
        return OpMinus.create(left, right);
    }

    protected Op processDiffQuery(Op compiledQuery, IQueryDiffBuilder builder) {
        ProjectionVariablesExtractorTransform extractorTransform = new ProjectionVariablesExtractorTransform();
        Transformer.transform(extractorTransform, compiledQuery);
        /*ArrayList<Var> allVariables = AllVariablesExtractorTransform.extractVariables(compiledQuery);
        boolean allUsed = true;
        for (Var var : allVariables) {
            ArrayList<Var> innerVariables = extractorTransform.getInnerVariables();
            if (!innerVariables.contains(var)) {
                allUsed = false;
                break;
            }
        }*/
        OperationReorderTransform transform = new OperationReorderTransform(extractorTransform);
        compiledQuery = Transformer.transform(transform, compiledQuery);
        if (transform.isGroup() && !transform.getSortConditions().isEmpty()) {
            compiledQuery = new OpOrder(compiledQuery, transform.getInnerSortConditions());
        }
        if (!transform.getProjectionVariables().isEmpty() && !extractorTransform.getInnerVariables().isEmpty()) {
            compiledQuery = new OpProject(compiledQuery, extractorTransform.getInnerVariables());
        }
        if (transform.isDistinct()) {
            compiledQuery = new OpDistinct(compiledQuery);
        }
        if (transform.isSlice()) {
            compiledQuery = new OpSlice(compiledQuery, transform.getSliceStart(), transform.getSliceLength());
        }
        Op current = builder.processCurrent(compiledQuery);
        Op previous = builder.processPrevious(compiledQuery);
        Op diff = getOpDiff(current, previous);
        if (!extractorTransform.getExtendedVariables().isEmpty())
            diff = OpExtend.create(diff, extractorTransform.getExtendedVariables());
        if (!transform.getSortConditions().isEmpty())
            diff = new OpOrder(diff, transform.getSortConditions());
        if (!transform.getProjectionVariables().isEmpty())
            diff = new OpProject(diff, transform.getProjectionVariables());
        return diff;
    }
}
