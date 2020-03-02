package cl.uchile.dcc.sparqlversioning.transform;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.SortCondition;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.Transformer;
import org.apache.jena.sparql.algebra.op.*;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.core.VarExprList;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.graph.NodeTransform;
import org.apache.jena.sparql.path.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class VariableCleanerTransform extends VersionedTransformBase {

    private ProjectionNodeTransform nodeTransform;
    private List<SortCondition> sortConditions;

    public VariableCleanerTransform(Query query) {
        this(Algebra.compile(query));
    }

    public List<SortCondition> getSortConditions() {
        return sortConditions;
    }

    public void clearHash() {
        nodeTransform.clearHash();
    }

    public VariableCleanerTransform(Op op) {
        ProjectionVariablesExtractorTransform transform = new ProjectionVariablesExtractorTransform();
        Transformer.transform(transform, op);
        this.nodeTransform = new ProjectionNodeTransform(transform.getProjectionVariables());
    }

    private Triple processTriple(Triple triple) {
        Node subject = nodeTransform.apply(triple.getSubject());
        Node predicate = nodeTransform.apply(triple.getPredicate());
        Node object = nodeTransform.apply(triple.getObject());
        return new Triple(subject, predicate, object);
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
        BasicPattern pattern = new BasicPattern();
        boolean changes = false;
        for (Triple triple : opBGP.getPattern()) {
            Triple processedTriple = processTriple(triple);
            if (!processedTriple.equals(triple))
                changes = true;
            pattern.add(processedTriple);
        }
        if (!changes)
            return opBGP;
        return new OpBGP(pattern);
    }

    @Override
    public Op transform(OpProject opProject, Op subOp) {
        return subOp;
    }

    @Override
    public Op transform(OpOrder opOrder, Op subOp) {
        sortConditions = opOrder.getConditions();
        return subOp;
    }

    @Override
    public Op transform(OpTriple opTriple) {
        Triple triple = processTriple(opTriple.getTriple());
        if (triple.equals(opTriple.getTriple()))
            return opTriple;
        return new OpTriple(triple);
    }

    @Override
    public Op transform(OpPath opPath) {
        TriplePath triplePath = opPath.getTriplePath();
        Triple triple = triplePath.asTriple();
        if (triple != null) {
            return new OpPath(new TriplePath(processTriple(triple)));
        }
        Path path = triplePath.getPath();
        Node subject = nodeTransform.apply(triplePath.getSubject());
        Node object = nodeTransform.apply(triplePath.getObject());
        if (subject.equals(triplePath.getSubject()) && object.equals(triplePath.getSubject()))
            return opPath;
        return new OpPath(new TriplePath(subject, path, object));
    }

    @Override
    public Op transform(OpFilter opFilter, Op op) {
        ExprList exprList = opFilter.getExprs().applyNodeTransform(nodeTransform);
        if (exprList.equals(opFilter.getExprs()))
            return opFilter;
        return OpFilter.filterBy(exprList, op);
    }

    @Override
    public Op transform(OpExtend opExtend, Op op) {
        VarExprList varExprList = opExtend.getVarExprList();
        VarExprList list = new VarExprList();
        List<Var> vars = varExprList.getVars();
        for (Var var : vars) {
            Expr expr = varExprList.getExpr(var);
            list.add(var, expr.applyNodeTransform(nodeTransform));
        }
        return OpExtend.create(op, list);
    }

    @Override
    public Op transform(OpLeftJoin opLeftJoin, Op left, Op right) {
        return OpLeftJoin.create(left, right, opLeftJoin.getExprs().applyNodeTransform(nodeTransform));
    }

    private class ProjectionNodeTransform implements NodeTransform {

        private ArrayList<Var> variables;
        private HashMap<Node, Node> blankNodeHash;

        ProjectionNodeTransform(ArrayList<Var> variables) {
            this.variables = variables;
            this.blankNodeHash = new HashMap<>();
        }

        private void clearHash() {
            blankNodeHash = new HashMap<>();
        }

        public Node apply(Node node) {
            if (blankNodeHash.containsKey(node))
                return blankNodeHash.get(node);
            if (node.isVariable()) {
                Var var = (Var) node;
                if (!variables.contains(var)) {
                    Node blankNode = NodeFactory.createBlankNode();
                    blankNodeHash.put(node, blankNode);
                    return blankNode;
                }
            }
            return node;
        }
    }
}
