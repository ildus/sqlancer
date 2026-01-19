package sqlancer.ingres.gen;

import sqlancer.Randomly;
import sqlancer.common.query.ExpectedErrors;
import sqlancer.common.query.SQLQueryAdapter;
import sqlancer.ingres.IngresProvider.IngresGlobalState;
import sqlancer.ingres.IngresSchema.IngresTable;
import sqlancer.ingres.IngresToStringVisitor;

public final class IngresDeleteGenerator {

    private IngresDeleteGenerator() {
    }

    public static SQLQueryAdapter generate(IngresGlobalState globalState) {
        StringBuilder sb = new StringBuilder("DELETE FROM ");
        ExpectedErrors errors = new ExpectedErrors();
        IngresTable table = globalState.getSchema().getRandomTable(t -> !t.isView());
        sb.append(table.getName());
        if (Randomly.getBoolean()) {
            sb.append(" WHERE ");
            sb.append(IngresToStringVisitor.asString(
                    new IngresExpressionGenerator(globalState).setColumns(table.getColumns()).generateExpression()));
        }
        return new SQLQueryAdapter(sb.toString(), errors);
    }

}
