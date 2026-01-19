package sqlancer.ingres.ast;

import sqlancer.common.ast.BinaryOperatorNode;
import sqlancer.common.ast.newast.NewUnaryPostfixOperatorNode;

public class IngresUnaryPostfixOperator extends NewUnaryPostfixOperatorNode<IngresExpression>
        implements IngresExpression {
    public IngresUnaryPostfixOperator(IngresExpression expr, BinaryOperatorNode.Operator op) {
        super(expr, op);
    }
}
