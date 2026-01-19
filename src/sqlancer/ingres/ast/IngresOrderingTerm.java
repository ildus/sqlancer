package sqlancer.ingres.ast;

import sqlancer.common.ast.newast.NewOrderingTerm;

public class IngresOrderingTerm extends NewOrderingTerm<IngresExpression> implements IngresExpression {
    public IngresOrderingTerm(IngresExpression expr, Ordering ordering) {
        super(expr, ordering);
    }
}
