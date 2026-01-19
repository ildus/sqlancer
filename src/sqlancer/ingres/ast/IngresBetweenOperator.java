package sqlancer.ingres.ast;

import sqlancer.common.ast.newast.NewBetweenOperatorNode;

public class IngresBetweenOperator extends NewBetweenOperatorNode<IngresExpression> implements IngresExpression {
    public IngresBetweenOperator(IngresExpression left, IngresExpression middle, IngresExpression right,
            boolean isTrue) {
        super(left, middle, right, isTrue);
    }
}
