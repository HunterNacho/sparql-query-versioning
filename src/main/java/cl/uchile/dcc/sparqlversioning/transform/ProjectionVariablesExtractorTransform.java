package cl.uchile.dcc.sparqlversioning.transform;

import cl.uchile.dcc.sparqlversioning.transform.expr.CoalesceDetectorTransform;
import cl.uchile.dcc.sparqlversioning.transform.node.VariableExtractionNodeTransform;
import org.apache.jena.query.SortCondition;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.op.*;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.core.VarExprList;
import org.apache.jena.sparql.expr.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ProjectionVariablesExtractorTransform extends VersionedTransformBase {

    private ArrayList<Var> projectionVariables = new ArrayList<>();
    private ArrayList<Var> innerVariables = new ArrayList<>();
    private VarExprList extendedVariables = new VarExprList();
    private VarExprList groupVariables = new VarExprList();
    private HashMap<Var, List<Var>> extendMap = new HashMap<>();
    private boolean groupReplaced = false;
    private boolean innerAdded = false;
    private int projectionLevels = 0;

    ArrayList<Var> getProjectionVariables() {
        return projectionVariables;
    }

    public VarExprList getExtendedVariables() {
        return extendedVariables;
    }

    public int getProjectionLevels() {
        return projectionLevels;
    }

    public List<SortCondition> cleanSortConditions(List<SortCondition> sortConditions) {
        ArrayList<Var> checkedVariables = new ArrayList<>();
        ArrayList<SortCondition> cleanSortConditions = new ArrayList<>();
        for (SortCondition sortCondition : sortConditions) {
            if (sortCondition.getExpression().isVariable() ) {
                Var condition = sortCondition.getExpression().asVar();
                if (extendMap.containsKey(condition)) {
                    for (Var var : extendMap.get(condition)) {
                        if (checkedVariables.contains(var))
                            continue;
                        cleanSortConditions.add(new SortCondition(var, sortCondition.getDirection()));
                        checkedVariables.add(var);
                    }
                    continue;
                }
                else if (checkedVariables.contains(condition)) {
                    continue;
                }
                checkedVariables.add(condition);
            }
            cleanSortConditions.add(sortCondition);
        }
        return cleanSortConditions;
    }

    public ArrayList<Var> getInnerVariables() {
        if (innerAdded)
            return innerVariables;
        for (Var var : projectionVariables) {
            if (!innerVariables.contains(var))
                innerVariables.add(var);
        }
        innerVariables.removeAll(extendedVariables.getVars());
        innerAdded = true;
        return innerVariables;
    }

    public VarExprList getGroupVariables() {
        if (groupReplaced)
            return groupVariables;
        for (Var var : extendedVariables.getVars()) {
            if (groupVariables.contains(var)) {
                groupVariables.remove(var);
                for (Var variable : extendMap.get(var)) {
                    groupVariables.add(variable);
                }
            }
        }
        groupReplaced = true;
        return groupVariables;
    }

    @Override
    public Op transform(OpExtend opExtend, Op op) {
        if (GroupTestingTransform.testGroup(opExtend))
            return OpExtend.create(op, opExtend.getVarExprList());
        VarExprList varExprList = opExtend.getVarExprList();
        for (Var variable : varExprList.getVars()) {
            if (!CoalesceDetectorTransform.testCoalesce(varExprList.getExpr(variable)))
                continue;
            VariableExtractionNodeTransform transform = new VariableExtractionNodeTransform();
            varExprList.getExpr(variable).applyNodeTransform(transform);
            if (transform.getVariables().size() < 2)
                continue;
            extendMap.put(variable, transform.getVariables());
            extendedVariables.add(variable, varExprList.getExpr(variable));
            innerVariables.addAll(transform.getVariables());
        }
        return OpExtend.create(op, varExprList);
    }

    @Override
    public Op transform(OpBGP opBGP) {
        return opBGP;
    }

    @Override
    public Op transform(OpPath opPath) {
        return opPath;
    }

    @Override
    public Op transform(OpProject opProject, Op op) {
        projectionVariables = new ArrayList<>(opProject.getVars());
        projectionLevels++;
        return super.transform(opProject, op);
    }

    @Override
    public Op transform(OpGroup opGroup, Op op) {
        groupVariables = opGroup.getGroupVars();
        return new OpGroup(op, opGroup.getGroupVars(), opGroup.getAggregators());
    }

}