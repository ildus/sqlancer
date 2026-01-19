package sqlancer.ingres.ast;

import sqlancer.common.ast.BinaryOperatorNode.Operator;
import sqlancer.common.ast.newast.NewBinaryOperatorNode;

public class IngresBinaryOperator extends NewBinaryOperatorNode<IngresExpression> implements IngresExpression {
    public IngresBinaryOperator(IngresExpression left, IngresExpression right, Operator op) {
        super(left, right, op);
    }
}
