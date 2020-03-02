package cl.uchile.dcc.sparqlversioning.transform.node;

import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.graph.NodeTransform;

import java.util.ArrayList;

public class VariableExtractionNodeTransform implements NodeTransform {

    private ArrayList<Var> variables = new ArrayList<>();

    @Override
    public Node apply(Node node) {
        if (node.isVariable()) {
            Var var = (Var) node;
            if (!variables.contains(var))
                variables.add(var);
        }
        return node;
    }

    public ArrayList<Var> getVariables() {
        return variables;
    }
}
