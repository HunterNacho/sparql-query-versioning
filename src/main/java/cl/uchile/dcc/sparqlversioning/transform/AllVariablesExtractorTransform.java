package cl.uchile.dcc.sparqlversioning.transform;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.TransformBase;
import org.apache.jena.sparql.algebra.Transformer;
import org.apache.jena.sparql.algebra.op.*;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.path.*;

import java.util.ArrayList;

public class AllVariablesExtractorTransform extends TransformBase {

    private ArrayList<Var> variables = new ArrayList<>();


    public static ArrayList<Var> extractVariables(Op op) {
        AllVariablesExtractorTransform transform = new AllVariablesExtractorTransform();
        Transformer.transform(transform, op);
        return transform.getVariables();
    }

    private ArrayList<Var> getVariables() {
        return variables;
    }

    private void addNode (Node node) {
        if (node.isVariable()) {
            Var var = Var.alloc(node);
            if (!variables.contains(var))
                variables.add(var);
        }
    }

    private void addTriple(Triple triple) {
        addNode(triple.getSubject());
        addNode(triple.getPredicate());
        addNode(triple.getObject());
    }

    @Override
    public Op transform(OpBGP opBGP) {
        for (Triple triple : opBGP.getPattern())
            addTriple(triple);
        return opBGP;
    }

    @Override
    public Op transform(OpTriple opTriple) {
        addTriple(opTriple.getTriple());
        return opTriple;
    }

    @Override
    public Op transform(OpPath opPath) {
        TriplePath triplePath = opPath.getTriplePath();
        if (triplePath.isTriple()) {
            addTriple(triplePath.asTriple());
            return opPath;
        }
        Node object = triplePath.getObject();
        Node subject = triplePath.getSubject();
        Path path = triplePath.getPath();

        if (path instanceof P_Path0) {
            addNode(((P_Path0) path).getNode());
            addNode(object);
            addNode(subject);
        }
        else if (path instanceof P_Path1) {
            Path subPath = ((P_Path1) path).getSubPath();
            transform(new OpPath(new TriplePath(subject, subPath, object)));
        }
        else if (path instanceof P_Path2) {
            Path leftPath = ((P_Path2) path).getLeft();
            Path rightPath = ((P_Path2) path).getRight();
            transform(new OpPath(new TriplePath(subject, leftPath, object)));
            transform(new OpPath(new TriplePath(subject, rightPath, object)));
        }
        else if (path instanceof P_NegPropSet) {
            for (P_Path0 subPath :((P_NegPropSet) path).getNodes())
                transform(new OpPath(new TriplePath(subject, subPath, object)));
        }
        return opPath;
    }
}
