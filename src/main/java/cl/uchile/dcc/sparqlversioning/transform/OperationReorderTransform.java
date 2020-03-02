package cl.uchile.dcc.sparqlversioning.transform;

import cl.uchile.dcc.sparqlversioning.transform.expr.CoalesceDetectorTransform;
import org.apache.jena.query.SortCondition;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.op.*;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.core.VarExprList;

import java.util.ArrayList;
import java.util.List;

public class OperationReorderTransform extends VersionedTransformBase {

    private List<SortCondition> sortConditions;
    private List<Var> projectionVariables;
    /*private List<VarExprList> extensionVarExprLists;
    private List<ExprAggregator> groupAggregators;*/
    private boolean group;
    private boolean slice;
    private long sliceStart;
    private long sliceLength;
    private boolean distinct;
    private ProjectionVariablesExtractorTransform transform;
    private int projectionDepth;

    public OperationReorderTransform(ProjectionVariablesExtractorTransform projectionVariablesExtractorTransform) {
        sortConditions = new ArrayList<>();
        projectionVariables = new ArrayList<>();
        /*extensionVarExprLists = new ArrayList<>();
        groupAggregators = new ArrayList<>();*/
        group = false;
        slice = false;
        distinct = false;
        transform = projectionVariablesExtractorTransform;
        projectionDepth = transform.getProjectionLevels();
    }

    public boolean isDistinct() {
        return distinct;
    }

    public boolean isSlice() {
        return slice;
    }

    public long getSliceStart() {
        return sliceStart;
    }

    public long getSliceLength() {
        return sliceLength;
    }

    public boolean isGroup() {
        return group;
    }

    /*public List<ExprAggregator> getGroupAggregators() {
        return groupAggregators;
    }

    public List<VarExprList> getExtensionVarExprLists() {
        return extensionVarExprLists;
    }*/

    public List<SortCondition> getInnerSortConditions() {
        return transform.cleanSortConditions(sortConditions);
    }
    public List<SortCondition> getSortConditions() {
        return sortConditions;
    }

    public List<Var> getProjectionVariables() {
        return projectionVariables;
    }

    @Override
    public Op transform(OpMinus opMinus, Op left, Op right) {
        return OpMinus.create(left, right);
    }

    @Override
    public Op transform(OpGraph opGraph, Op op) {
        return new OpGraph(opGraph.getNode(), op);
    }

    @Override
    public Op transform(OpBGP opBGP) {
        return opBGP;
    }

    @Override
    public Op transform(OpExtend opExtend, Op subOp) {
        if (GroupTestingTransform.testGroup(opExtend)) {
            //extensionVarExprLists.add(opExtend.getVarExprList());
            return OpExtend.create(subOp, opExtend.getVarExprList());
        }
        VarExprList varExprList = opExtend.getVarExprList();
        VarExprList cleanVarExprList = new VarExprList();
        for (Var var : varExprList.getVars()) {
            if (CoalesceDetectorTransform.testCoalesce(varExprList.getExpr(var)))
                continue;
            cleanVarExprList.add(var, varExprList.getExpr(var));
        }
        if (cleanVarExprList.isEmpty())
            return subOp;
        return OpExtend.create(subOp, cleanVarExprList);
    }

    @Override
    public Op transform(OpGroup opGroup, Op subOp) {
        group = true;
        /*groupAggregators = opGroup.getAggregators();*/
        return new OpGroup(subOp, transform.getGroupVariables(), opGroup.getAggregators());
    }

    @Override
    public Op transform(OpProject opProject, Op subOp) {
        projectionDepth--;
        if (projectionDepth == 0) {
            projectionVariables = opProject.getVars();
            return subOp;
        }
        return new OpProject(subOp, opProject.getVars());
    }

    @Override
    public Op transform(OpOrder opOrder, Op subOp) {
        if (projectionDepth > 1)
            return new OpOrder(subOp, opOrder.getConditions());
        sortConditions = opOrder.getConditions();
        return subOp;
    }

    @Override
    public Op transform(OpSlice opSlice, Op subOp) {
        if (projectionDepth > 1)
            return new OpSlice(subOp, opSlice.getStart(), opSlice.getLength());
        slice = true;
        sliceStart = opSlice.getStart();
        sliceLength = opSlice.getLength();
        return subOp;
    }

    @Override
    public Op transform(OpTriple opTriple) {
        return opTriple;
    }

    @Override
    public Op transform(OpPath opPath) {
        return opPath;
    }

    @Override
    public Op transform(OpDistinct opDistinct, Op subOp) {
        if (projectionDepth > 1)
            return new OpDistinct(subOp);
        distinct = true;
        return subOp;
    }
}
