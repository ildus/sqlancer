package sqlancer.ingres.ast;

import sqlancer.common.ast.newast.ColumnReferenceNode;
import sqlancer.ingres.IngresSchema;

public class IngresColumnReference extends ColumnReferenceNode<IngresExpression, IngresSchema.IngresColumn>
        implements IngresExpression {
    public IngresColumnReference(IngresSchema.IngresColumn column) {
        super(column);
    }

}
