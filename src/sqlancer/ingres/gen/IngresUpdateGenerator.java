package sqlancer.ingres.gen;

import java.util.List;

import sqlancer.Randomly;
import sqlancer.common.gen.AbstractUpdateGenerator;
import sqlancer.common.query.SQLQueryAdapter;
import sqlancer.ingres.IngresProvider.IngresGlobalState;
import sqlancer.ingres.IngresSchema.IngresColumn;
import sqlancer.ingres.IngresSchema.IngresTable;
import sqlancer.ingres.IngresToStringVisitor;
import sqlancer.ingres.ast.IngresExpression;

public final class IngresUpdateGenerator extends AbstractUpdateGenerator<IngresColumn> {

    private final IngresGlobalState globalState;
    private IngresExpressionGenerator gen;

    private IngresUpdateGenerator(IngresGlobalState globalState) {
        this.globalState = globalState;
    }

    public static SQLQueryAdapter getQuery(IngresGlobalState globalState) {
        return new IngresUpdateGenerator(globalState).generate();
    }

    private SQLQueryAdapter generate() {
        IngresTable table = globalState.getSchema().getRandomTable(t -> !t.isView());
        List<IngresColumn> columns = table.getRandomNonEmptyColumnSubsetFilter(p -> !p.getName().equals("oid"));
        gen = new IngresExpressionGenerator(globalState).setColumns(table.getColumns());
        sb.append("UPDATE ");
        sb.append(table.getName());
        sb.append(" SET ");
        updateColumns(columns);
        return new SQLQueryAdapter(sb.toString(), errors);
    }

    @Override
    protected void updateValue(IngresColumn column) {
        IngresExpression expr;
        if (Randomly.getBooleanWithSmallProbability()) {
            expr = gen.generateExpression();
        } else {
            expr = gen.generateConstant();
        }
        sb.append(IngresToStringVisitor.asString(expr));
    }

}
