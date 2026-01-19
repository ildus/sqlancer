package sqlancer.ingres.ast;

import sqlancer.common.ast.newast.TableReferenceNode;
import sqlancer.ingres.IngresSchema;

public class IngresTableReference extends TableReferenceNode<IngresExpression, IngresSchema.IngresTable>
        implements IngresExpression {
    public IngresTableReference(IngresSchema.IngresTable table) {
        super(table);
    }
}
