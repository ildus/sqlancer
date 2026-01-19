package sqlancer.ingres.ast;

import sqlancer.common.ast.newast.NewTernaryNode;

public class IngresTernary extends NewTernaryNode<IngresExpression> implements IngresExpression {
    public IngresTernary(IngresExpression left, IngresExpression middle, IngresExpression right, String leftString,
            String rightString) {
        super(left, middle, right, leftString, rightString);
    }
}
