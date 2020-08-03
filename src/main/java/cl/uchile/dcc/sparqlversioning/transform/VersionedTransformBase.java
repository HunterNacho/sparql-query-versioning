package cl.uchile.dcc.sparqlversioning.transform;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.Transform;
import org.apache.jena.sparql.algebra.Transformer;
import org.apache.jena.sparql.algebra.op.*;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.path.*;
import org.apache.jena.sparql.util.NodeIsomorphismMap;

import java.util.ArrayList;
import java.util.List;

public abstract class VersionedTransformBase implements Transform {

    private long varNumber = 0;

    public Op transform(OpTable opTable) {
        return opTable;
    }

    public abstract Op transform(OpBGP opBGP);

    public Op transform(OpTriple opTriple) {
        OpBGP opBGP = new OpBGP();
        opBGP.getPattern().add(opTriple.getTriple());
        return transform(opBGP);
    }

    public Op transform(OpQuad opQuad) {
        return opQuad;
    }

    public Op transform(OpProject opProject, Op subOp) {
        return new OpProject(subOp, opProject.getVars());
    }

    public Op transform(OpDistinct opDistinct, Op subOp) {
        return new OpDistinct(subOp);
    }

    public Op transform(OpReduced opReduced, Op subOp) {
        return OpReduced.create(subOp);
    }

    public Op transform(OpSlice opSlice, Op op) {
        return new OpSlice(op, opSlice.getStart(), opSlice.getLength());
    }

    public Op transform(OpGroup opGroup, Op op) {
        return new OpGroup(op, opGroup.getGroupVars(), opGroup.getAggregators());
    }

    public Op transform(OpOrder opOrder, Op subOp) {
        return new OpOrder(subOp, opOrder.getConditions());
    }

    public Op transform(OpTopN opTopN, Op op) {
        return new OpTopN(op, opTopN.getLimit(), opTopN.getConditions());
    }

    public Op transform(OpExtend opExtend, Op subOp) {
        return OpExtend.create(subOp, opExtend.getVarExprList());
    }

    public Op transform(OpJoin opJoin, Op op, Op op1) {
        return OpJoin.create(op, op1);
    }

    public Op transform(OpLeftJoin opLeftJoin, Op left, Op right) {
        return OpLeftJoin.create(left, right, opLeftJoin.getExprs());
    }

    public Op transform(OpDiff opDiff, Op op, Op op1) {
        return OpDiff.create(op, op1);
    }

    public Op transform(OpMinus opMinus, Op left, Op right) {
        if (right instanceof OpGraph)
            return opMinus;
        if (opMinus.getRight() instanceof OpGraph)
            return opMinus;
        if (opMinus.equalTo(left, new NodeIsomorphismMap()))
            return left;
        if (opMinus.equalTo(right, new NodeIsomorphismMap()))
            return right;
        return OpMinus.create(left, right);
    }

    public Op transform(OpUnion opUnion, Op left, Op right) {
        return new OpUnion(left, right);
    }

    public Op transform(OpConditional opConditional, Op op, Op op1) {
        return new OpConditional(op, op1);
    }

    public Op transform(OpSequence opSequence, List<Op> subOps) {
        BasicPattern triples = new BasicPattern();
        ArrayList<Op> rest =  new ArrayList<>();
        for (Op op : subOps) {
            if (op instanceof OpTriple)
                triples.add(((OpTriple) op).getTriple());
            else
                rest.add(op);
        }
        if (rest.isEmpty()) {
            return transform(new OpBGP(triples));
        }
        if (triples.isEmpty()) {
            return opSequence.copy(subOps);
        }
        OpBGP opBGP = new OpBGP(triples);
        OpSequence newOp = OpSequence.create();
        newOp.add(transform(opBGP));
        for (Op op : rest)
            newOp.add(Transformer.transform(this, op));
        return newOp;
    }

    public Op transform(OpDisjunction opDisjunction, List<Op> list) {
        ArrayList<Op> newList = new ArrayList<>();
        for (Op op : list) {
            newList.add(Transformer.transform(this, op));
        }
        return opDisjunction.copy(newList);
    }

    public Op transform(OpExt opExt) {
        return opExt;
    }

    public Op transform(OpList opList, Op op) {
        return new OpList(op);
    }

    public Op transform(OpPath opPath) {
        TriplePath triplePath = opPath.getTriplePath();
        Triple triple = triplePath.asTriple();
        if (triple != null) {
            return new OpTriple(triple);
        }
        Path path = triplePath.getPath();
        Node subject = triplePath.getSubject();
        Node object = triplePath.getObject();
        if (path instanceof P_Alt) {
            Path leftPath = ((P_Alt) path).getLeft();
            Path rightPath = ((P_Alt) path).getRight();
            return new OpUnion(
                    transform(new OpPath(new TriplePath(subject, leftPath, object))),
                    transform(new OpPath(new TriplePath(subject, rightPath, object)))
            );
        }
        else if (path instanceof P_Distinct) {
            return new OpDistinct(new OpPath(new TriplePath(subject, ((P_Distinct) path).getSubPath(), object))); // ???
        }
        else if (path instanceof P_FixedLength) {
            Path subPath = ((P_FixedLength) path).getSubPath();
            long count = ((P_FixedLength) path).getCount();
            if (count == 1)
                return new OpPath(new TriplePath(subject, subPath, object));
            Node current;
            Node previous = subject;
            ArrayList<Op> ops = new ArrayList<>();
            for (int i = 0; i < count - 1; i++) {
                current = NodeFactory.createVariable("var" + varNumber++);
                ops.add(new OpPath(new TriplePath(previous, subPath, current)));
                previous = current;
            }
            ops.add(new OpPath(new TriplePath(previous, subPath, object)));
            return OpSequence.create().copy(ops);
        }
        else if (path instanceof P_Inverse) {
            return new OpPath(new TriplePath(object, ((P_Inverse) path).getSubPath(), subject));
        }
        else if (path instanceof P_ReverseLink) {
            return new OpPath(new TriplePath(new Triple(object, ((P_ReverseLink) path).getNode(), subject)));
        }
        else if (path instanceof P_Seq) {
            Node variable =  NodeFactory.createVariable("var" + varNumber++);
            Path leftPath = ((P_Seq) path).getLeft();
            Path rightPath = ((P_Seq) path).getRight();
            ArrayList<Op> triples;
            triples = extractTriples(subject, (P_Seq) path, object);
            if (triples != null && !triples.isEmpty()) {
                return OpSequence.create().apply(this, triples);
            }
            return OpSequence.create(
                    new OpPath(new TriplePath(subject, leftPath, variable)),
                    new OpPath(new TriplePath(variable, rightPath, object))
            );
        }
        else if (isComplex(path)) {
            return handleComplexPath(opPath);
        }

        return opPath;
    }

    private ArrayList<Op> extractTriples(Node subject, P_Seq seq, Node object) {
        ArrayList<Op> ops = new ArrayList<>();
        Node variable = NodeFactory.createVariable("var" + varNumber++);
        Node left = subject;
        Node right = variable;
        Path[] paths = {seq.getLeft(), seq.getRight()};
        for (Path path : paths) {
            if (path instanceof P_Link) {
                ops.add(new OpTriple(new Triple(left, ((P_Link) path).getNode(), right)));
            } else if (path instanceof P_ReverseLink) {
                ops.add(new OpTriple(new Triple(right, ((P_ReverseLink) path).getNode(), left)));
            } else if (path instanceof P_Seq) {
                ops.addAll(extractTriples(left, (P_Seq) path, right));
            } else if (path instanceof P_Alt) {
                ops.add(transform(new OpPath(new TriplePath(left, path, right))));
            } else if (isComplex(path)) {
                ops.add(handleComplexPath(new OpPath(new TriplePath(left, path, right))));
            } else {
                // Unhandled
                return new ArrayList<>();
            }
            left = variable;
            right = object;
        }
        return ops;
    }

    // To be implemented
    protected Op handleComplexPath(OpPath opPath){
        return opPath;
    }

    private boolean isComplex(Path path) {
        return path instanceof P_ZeroOrOne ||
                // path instanceof P_Mod || // Unsupported
                path instanceof P_ZeroOrMoreN ||
                path instanceof P_ZeroOrMore1 ||
                path instanceof P_OneOrMore1 ||
                path instanceof P_OneOrMoreN;
    }

    public Op transform(OpDatasetNames opDatasetNames) {
        return opDatasetNames;
    }

    public Op transform(OpQuadPattern opQuadPattern) {
        return opQuadPattern;
    }

    public Op transform(OpQuadBlock opQuadBlock) {
        return opQuadBlock;
    }

    public Op transform(OpNull opNull) {
        return opNull;
    }

    public Op transform(OpFilter opFilter, Op op) {
        OpFilter newFilter = OpFilter.ensureFilter(op);
        newFilter.getExprs().addAll(opFilter.getExprs());
        return newFilter;
    }

    public Op transform(OpGraph opGraph, Op op) {
        return opGraph;
    }

    public Op transform(OpService opService, Op op) {
        return opService.copy(op);
    }

    public Op transform(OpProcedure opProcedure, Op op) {
        return opProcedure.copy(op);
    }

    public Op transform(OpPropFunc opPropFunc, Op op) {
        return opPropFunc.copy(op);
    }

    public Op transform(OpLabel opLabel, Op op) {
        return opLabel.copy(op);
    }

    public Op transform(OpAssign opAssign, Op op) {
        return opAssign.copy(op);
    }

}
