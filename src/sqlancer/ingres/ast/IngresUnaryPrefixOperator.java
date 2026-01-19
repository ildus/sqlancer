package sqlancer.ingres.ast;

import sqlancer.common.ast.BinaryOperatorNode;
import sqlancer.common.ast.newast.NewUnaryPrefixOperatorNode;

public class IngresUnaryPrefixOperator extends NewUnaryPrefixOperatorNode<IngresExpression>
        implements IngresExpression {
    public IngresUnaryPrefixOperator(IngresExpression expr, BinaryOperatorNode.Operator operator) {
        super(expr, operator);
    }
}
