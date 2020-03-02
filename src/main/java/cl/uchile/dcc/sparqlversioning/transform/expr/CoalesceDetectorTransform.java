package cl.uchile.dcc.sparqlversioning.transform.expr;

import org.apache.jena.sparql.algebra.op.OpExtend;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.core.VarExprList;
import org.apache.jena.sparql.expr.*;

public class CoalesceDetectorTransform extends ExprTransformBase {

    private boolean coalesce = false;

    public boolean isCoalesce() {
        return coalesce;
    }

    @Override
    public Expr transform(ExprFunctionN func, ExprList args) {
        if (func.getFunctionSymbol().getSymbol().equalsIgnoreCase("coalesce"))
            coalesce = true;
        return func;
    }

    public static boolean testCoalesce(Expr expr) {
        CoalesceDetectorTransform detectorTransform = new CoalesceDetectorTransform();
        ExprTransformer.transform(detectorTransform, expr);
        return detectorTransform.isCoalesce();
    }
}
