package cl.uchile.dcc.sparqlversioning.transform;

import org.apache.jena.query.Query;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.Transformer;
import org.apache.jena.sparql.algebra.op.OpBGP;
import org.apache.jena.sparql.algebra.op.OpPath;

public class ComplexTestTransform extends VersionedTransformBase {

    private boolean complex;

    public ComplexTestTransform(Query query) {
        Transformer.transform(this, Algebra.compile(query));
    }

    @Override
    public Op transform(OpBGP opBGP) {
        return opBGP;
    }

    @Override
    protected Op handleComplexPath(OpPath opPath) {
        complex = true;
        return super.handleComplexPath(opPath);
    }

    public boolean isComplex() {
        return complex;
    }
}
