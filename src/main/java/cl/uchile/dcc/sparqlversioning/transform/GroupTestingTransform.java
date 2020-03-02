package cl.uchile.dcc.sparqlversioning.transform;

import cl.uchile.dcc.sparqlversioning.transform.node.VariableExtractionNodeTransform;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.TransformBase;
import org.apache.jena.sparql.algebra.Transformer;
import org.apache.jena.sparql.algebra.op.OpExtend;
import org.apache.jena.sparql.algebra.op.OpGroup;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.ExprAggregator;

import java.util.ArrayList;
import java.util.List;

class GroupTestingTransform extends TransformBase {

    private boolean group = false;
    private List<ExprAggregator> groupAggregators = new ArrayList<>();

    public boolean getGroup() {
        return group;
    }

    @Override
    public Op transform(OpExtend opExtend, Op subOp) {
        for (Var variable : opExtend.getVarExprList().getVars()) {
            VariableExtractionNodeTransform transform = new VariableExtractionNodeTransform();
            opExtend.getVarExprList().getExpr(variable).applyNodeTransform(transform);
            List<Var> extendVars = transform.getVariables();

            for (ExprAggregator exprAggregator : groupAggregators) {
                VariableExtractionNodeTransform nodeTransform = new VariableExtractionNodeTransform();
                exprAggregator.applyNodeTransform(nodeTransform);
                for (Var var : nodeTransform.getVariables()) {
                    if (extendVars.contains(var)) {
                        group = true;
                        return OpExtend.create(subOp, opExtend.getVarExprList());
                    }
                }
            }
        }
        group = false;
        return OpExtend.create(subOp, opExtend.getVarExprList());
    }

    @Override
    public Op transform(OpGroup opGroup, Op subOp) {
        group = true;
        groupAggregators = opGroup.getAggregators();
        return opGroup;
    }

    static boolean testGroup(OpExtend opExtend) {
        GroupTestingTransform transform = new GroupTestingTransform();
        Transformer.transform(transform, opExtend);
        return transform.getGroup();
    }
}
