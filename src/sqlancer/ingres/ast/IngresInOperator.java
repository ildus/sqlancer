package sqlancer.ingres.ast;

import java.util.List;

import sqlancer.common.ast.newast.NewInOperatorNode;

public class IngresInOperator extends NewInOperatorNode<IngresExpression> implements IngresExpression {
    public IngresInOperator(IngresExpression left, List<IngresExpression> right, boolean isNegated) {
        super(left, right, isNegated);
    }
}
