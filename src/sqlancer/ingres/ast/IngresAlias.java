package sqlancer.ingres.ast;

import sqlancer.common.visitor.UnaryOperation;

public class IngresAlias implements UnaryOperation<IngresExpression>, IngresExpression {

    private final IngresExpression expr;
    private final String alias;

    public IngresAlias(IngresExpression expr, String alias) {
        this.expr = expr;
        this.alias = alias;
    }

    @Override
    public IngresExpression getExpression() {
        return expr;
    }

    @Override
    public String getOperatorRepresentation() {
        return " as " + alias;
    }

    @Override
    public OperatorKind getOperatorKind() {
        return OperatorKind.POSTFIX;
    }

    @Override
    public boolean omitBracketsWhenPrinting() {
        return true;
    }

}
