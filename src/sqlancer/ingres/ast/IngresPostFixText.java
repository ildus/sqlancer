package sqlancer.ingres.ast;

import sqlancer.common.ast.newast.NewPostfixTextNode;

public class IngresPostFixText extends NewPostfixTextNode<IngresExpression> implements IngresExpression {
    public IngresPostFixText(IngresExpression expr, String string) {
        super(expr, string);
    }
}
