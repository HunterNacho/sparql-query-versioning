package cl.uchile.dcc.sparqlversioning.querybuilder;

import cl.uchile.dcc.sparqlversioning.transform.VersionedTransformBase;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.Transformer;
import org.apache.jena.sparql.algebra.op.*;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.core.VarExprList;
import org.apache.jena.sparql.expr.*;
import org.apache.jena.sparql.expr.aggregate.AggregatorFactory;
import org.apache.jena.sparql.expr.nodevalue.NodeValueDT;
import org.apache.jena.sparql.expr.nodevalue.NodeValueNode;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;

import java.util.ArrayList;
import java.util.Collection;

public class MetadataSequentialVersionedQueryBuilder extends SequentialVersionedQueryBuilder {

    private static Node DIRECTION_PREDICATE = NodeFactory.createURI(DELTA + "direction");
    private static Node VERSION_PREDICATE = NodeFactory.createURI(DELTA + "version");
    private int graphId = 0;
    private String varSuffix;

    public MetadataSequentialVersionedQueryBuilder(Query query, int targetVersion, boolean baseOnLast) {
        this(query, targetVersion, baseOnLast, "");
    }

    public MetadataSequentialVersionedQueryBuilder(Query query, int targetVersion, boolean baseOnLast, String varSuffix) {
        super(query, targetVersion, baseOnLast);
        this.varSuffix = varSuffix;
    }

    @Override
    public Op processQuery(Op compiledQuery) {
        return Transformer.transform(new MetadataSequentialTransform(), compiledQuery);
    }

    @Override
    protected Collection<String> getFrom() {
        return new ArrayList<>();
    }


    private class MetadataSequentialTransform extends VersionedTransformBase {

        @Override
        protected Op handleComplexPath(OpPath opPath) {
            return transformTriplePath(opPath);
        }

        @Override
        public Op transform(OpBGP opBGP) {
            OpSequence opSequence = OpSequence.create();
            for (Triple triple : opBGP.getPattern()) {
                opSequence.add(transform(new OpTriple(triple)));
            }
            return opSequence;
        }

        @Override
        public Op transform(OpTriple opTriple) {
            return transformTriplePath(opTriple);
        }

        private Op transformTriplePath(Op op) {
            GraphVariables leftVariables = new GraphVariables();
            GraphVariables rightVariables = new GraphVariables();
            Op left = getSubQuery(op, leftVariables, forwards);
            Op right = getSubQuery(op, rightVariables, !forwards);
            Expr expr = new E_LogicalOr(
                    new E_LogicalNot(new E_Bound(new ExprVar(rightVariables.maxVersionVar))),
                    new E_GreaterThan(
                            new E_Str(new ExprVar(leftVariables.maxVersionVar)),
                            new E_Str(new ExprVar(rightVariables.maxVersionVar))
                    ));
            return OpLeftJoin.create(left, right, expr);
        }

        private Op getSubQuery(Op baseOp, GraphVariables graphVariables, boolean direction) {
            Node subject;
            Node object;
            Node predicate;

            if (baseOp instanceof OpTriple) {
                Triple triple = ((OpTriple) baseOp).getTriple();
                subject = triple.getSubject();
                object = triple.getObject();
                predicate = triple.getPredicate();
            }
            else if (baseOp instanceof OpPath) {
                TriplePath triplePath = ((OpPath) baseOp).getTriplePath();
                subject = triplePath.getSubject();
                object = triplePath.getObject();
                predicate = triplePath.getPredicate();
            }
            else {
                return baseOp;
            }

            Node directionNode;
            if (direction) {
                directionNode = NodeFactory.createLiteral("forwards", new XSDDatatype("string"));
            }
            else {
                directionNode = NodeFactory.createLiteral("backwards", new XSDDatatype("string"));
            }
            OpSequence opSequence = OpSequence.create();
            opSequence.add(new OpGraph(graphVariables.graphVar, baseOp));
            opSequence.add(new OpTriple(new Triple(graphVariables.graphVar, DIRECTION_PREDICATE, directionNode)));
            opSequence.add(new OpTriple(new Triple(graphVariables.graphVar, VERSION_PREDICATE, graphVariables.versionVar)));
            Expr expr = new E_LessThanOrEqual(
                    new E_Str(new ExprVar(graphVariables.versionVar)),
                    new NodeValueString(targetVersion)
            );
            Op op;
            op = OpExtend.create(opSequence, graphVariables.extraVersionVar, new E_Str(new ExprVar(graphVariables.versionVar)));
            op = OpFilter.filter(expr, op);
            VarExprList groupVars = new VarExprList();
            if (subject.isVariable()) {
                groupVars.add(Var.alloc(subject));
            }
            if (object.isVariable()) {
                groupVars.add(Var.alloc(object));
            }
            if (predicate != null && predicate.isVariable()) {
                groupVars.add(Var.alloc(predicate));
            }
            ArrayList<Var> projectionVariables = new ArrayList<>(groupVars.getVars());
            projectionVariables.add(graphVariables.maxVersionVar);
            ArrayList<ExprAggregator> exprAggregators = new ArrayList<>();
            Var tempVar = Var.alloc(".0");
            exprAggregators.add(
                    new ExprAggregator(
                            tempVar,
                            AggregatorFactory.createMax(false, new ExprVar(graphVariables.extraVersionVar)
                            )));
            op = OpGroup.create(op, groupVars, exprAggregators);
            op = OpExtend.create(op, graphVariables.maxVersionVar,
                    new E_StrDatatype(
                            new ExprVar(tempVar),
                            new NodeValueNode(NodeFactory.createURI("http://www.w3.org/2001/XMLSchema#string"))));
            if (object.isVariable() && predicate != null && predicate.isURI() && predicate.hasURI("http://www.w3.org/2000/01/rdf-schema#label")) {
                op = OpFilter.filter(
                        new E_Equals(
                                new E_Lang(new ExprVar(Var.alloc(object))),
                                new NodeValueString("en")),
                        op);
            }
            op = new OpProject(op, projectionVariables);
            return op;
        }

        /*
        @Override
        public Op transform(OpTriple opTriple) {
            Triple triple = opTriple.getTriple();
            Node subject = triple.getSubject();
            Node predicate = triple.getPredicate();
            Node object = triple.getObject();
            GraphVariables variables = new GraphVariables();
            OpSequence maxSequence = OpSequence.create();
            maxSequence.add(new OpGraph(variables.maxGraphVar, opTriple));
            maxSequence.add(new OpTriple(new Triple(variables.maxGraphVar, VERSION_PREDICATE, variables.versionVar)));
            Expr expr = new E_LessThanOrEqual(
                    new E_Str(new ExprVar(variables.versionVar)),
                    new NodeValueString(targetVersion)
            );
            Op maxOp;
            maxOp = OpExtend.create(maxSequence, variables.extraVersionVar, new E_Str(new ExprVar(variables.versionVar)));
            maxOp = OpFilter.filter(expr, maxOp);
            VarExprList groupVars = new VarExprList();
            if (subject.isVariable()) {
                groupVars.add(Var.alloc(subject));
            }
            if (object.isVariable()) {
                groupVars.add(Var.alloc(object));
            }
            if (predicate.isVariable()) {
                groupVars.add(Var.alloc(predicate));
            }
            ArrayList<Var> projectionVariables = new ArrayList<>(groupVars.getVars());
            projectionVariables.add(variables.maxVersionVar);
            ArrayList<ExprAggregator> exprAggregators = new ArrayList<>();
            Var tempVar = Var.alloc(".0");
            exprAggregators.add(
                    new ExprAggregator(
                            tempVar,
                            AggregatorFactory.createMax(false, new ExprVar(variables.extraVersionVar)
                            )));
            maxOp = OpGroup.create(maxOp, groupVars, exprAggregators);
            maxOp = OpExtend.create(maxOp, variables.maxVersionVar,
                    new E_StrDatatype(
                            new ExprVar(tempVar),
                            new NodeValueNode(NodeFactory.createURI("http://www.w3.org/2001/XMLSchema#string"))));
            maxOp = new OpProject(maxOp, projectionVariables);

            OpSequence filterSequence = OpSequence.create();
            filterSequence.add(new OpGraph(variables.graphVar, opTriple));
            filterSequence.add(new OpTriple(new Triple(variables.graphVar, DIRECTION_PREDICATE, variables.directionVar)));
            filterSequence.add(new OpTriple(new Triple(variables.graphVar, VERSION_PREDICATE, variables.maxVersionVar)));
            Expr directionExpr = new E_Equals(
                    new E_Str(new ExprVar(variables.directionVar)),
                    new NodeValueString(forwards ? "forwards" : "backwards")
            );
            Op filterOp = OpFilter.filter(directionExpr, filterSequence);
            //filterOp = new OpProject(filterOp, projectionVariables);
            return OpJoin.create(maxOp, filterOp);
        }
        */

        /*
        @Override
        public Op transform(OpTriple opTriple) {
            GraphVariables addVariables = new GraphVariables();
            Op addSequence = generateGraphFilterSequence(opTriple, addVariables, forwards);
            Expr addExpr = new E_LessThanOrEqual(
                    new E_Str(new ExprVar(addVariables.versionVar)),
                    new NodeValueString(targetVersion)
            );
            addSequence = OpFilter.filter(addExpr, addSequence);

            GraphVariables filterVariables = new GraphVariables();
            Op filterSequence = generateGraphFilterSequence(opTriple, filterVariables, !forwards);
            Expr filterExpr = new E_LessThanOrEqual(
                    new E_Str(new ExprVar(filterVariables.versionVar)),
                    new NodeValueString(targetVersion)
            );
            filterSequence = OpFilter.filter(filterExpr, filterSequence);
            //return OpFilter.filter(new E_NotExists(filterSequence), addSequence);
            Expr diffExpr = new E_GreaterThan(
                    new E_Str(new ExprVar(filterVariables.versionVar)),
                    new E_Str(new ExprVar(addVariables.versionVar))
            );
            filterSequence = OpFilter.filter(diffExpr, filterSequence);
            //return OpFilter.filter(diffExpr, OpMinus.create(addSequence, filterSequence));
            return OpMinus.create(addSequence, filterSequence);
        }

        private Op generateGraphFilterSequence(OpTriple opTriple, GraphVariables variables, boolean direction) {
            OpSequence opSequence = OpSequence.create();
            opSequence.add(new OpGraph(variables.graphVar, opTriple));
            opSequence.add(new OpTriple(new Triple(variables.graphVar, DIRECTION_PREDICATE, variables.directionVar)));
            opSequence.add(new OpTriple(new Triple(variables.graphVar, VERSION_PREDICATE, variables.versionVar)));
            NodeValueString nodeValueString;
            if (direction)
                nodeValueString = new NodeValueString("forwards");
            else
                nodeValueString = new NodeValueString("backwards");
            Expr directionFilterExpr = new E_Equals(new E_Str(new ExprVar(variables.directionVar)), nodeValueString);
            return OpFilter.filter(directionFilterExpr, opSequence);
        }
        */

        private class GraphVariables {
            private final Var maxGraphVar = Var.alloc("max_g" + varSuffix + graphId);
            private final Var graphVar = Var.alloc("g" + varSuffix + graphId);
            private final Var versionVar = Var.alloc("v" + varSuffix + graphId);
            private final Var extraVersionVar = Var.alloc("v" + varSuffix + graphId + "_");
            private final Var directionVar = Var.alloc("d" + varSuffix + graphId);
            private final Var maxVersionVar = Var.alloc("max_v" + varSuffix + graphId);

            GraphVariables() {
                graphId++;
            }
        }
    }
}
