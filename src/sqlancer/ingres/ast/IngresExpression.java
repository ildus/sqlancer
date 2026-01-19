package sqlancer.ingres.ast;

import sqlancer.common.ast.newast.Expression;
import sqlancer.ingres.IngresSchema.IngresColumn;
import sqlancer.ingres.IngresSchema.IngresDataType;

public interface IngresExpression extends Expression<IngresColumn> {

    default IngresDataType getExpressionType() {
        return null;
    }

    default IngresConstant getExpectedValue() {
        return null;
    }
}
