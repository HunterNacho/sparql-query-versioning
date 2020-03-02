package cl.uchile.dcc.sparqlversioning.transform;

import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.op.OpBGP;
import org.apache.jena.sparql.algebra.op.OpSequence;
import org.apache.jena.sparql.algebra.op.OpTriple;

import java.util.ArrayList;

public abstract class DiffTransformBase extends VersionedTransformBase {

    protected abstract Op processTriple(Triple triple);

    @Override
    public Op transform(OpTriple opTriple) {
        return processTriple(opTriple.getTriple());
    }

    @Override
    public Op transform(OpBGP opBGP) {
        ArrayList<Op> processedTriples = new ArrayList<>();
        for (Triple triple : opBGP.getPattern())
            processedTriples.add(processTriple(triple));
        return OpSequence.create().copy(processedTriples);
    }
}
