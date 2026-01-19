package sqlancer.ingres.gen;

import java.util.List;
import java.util.stream.Collectors;

import sqlancer.Randomly;
import sqlancer.common.gen.AbstractInsertGenerator;
import sqlancer.common.query.ExpectedErrors;
import sqlancer.common.query.SQLQueryAdapter;
import sqlancer.ingres.IngresProvider.IngresGlobalState;
import sqlancer.ingres.IngresSchema.IngresColumn;
import sqlancer.ingres.IngresSchema.IngresTable;
import sqlancer.ingres.IngresToStringVisitor;

public class IngresInsertGenerator extends AbstractInsertGenerator<IngresColumn> {

    private final IngresGlobalState globalState;
    private final ExpectedErrors errors = new ExpectedErrors();

    public IngresInsertGenerator(IngresGlobalState globalState) {
        this.globalState = globalState;
    }

    public static SQLQueryAdapter getQuery(IngresGlobalState globalState) {
        return new IngresInsertGenerator(globalState).generate();
    }

    private SQLQueryAdapter generate() {
        sb.append("INSERT INTO ");
        IngresTable table = globalState.getSchema().getRandomTable(t -> !t.isView());
        List<IngresColumn> columns = table.getRandomNonEmptyColumnSubsetFilter(p -> !p.getName().equals("rowid"));
        sb.append(table.getName());
        sb.append("(");
        sb.append(columns.stream().map(c -> c.getName()).collect(Collectors.joining(", ")));
        sb.append(")");
        sb.append(" VALUES ");
        insertColumns(columns);
        return new SQLQueryAdapter(sb.toString(), errors);
    }

    @Override
    protected void insertValue(IngresColumn columnIngres) {
        // TODO: select a more meaningful value
        if (Randomly.getBooleanWithRatherLowProbability()) {
            sb.append("DEFAULT");
        } else {
            sb.append(IngresToStringVisitor.asString(new IngresExpressionGenerator(globalState).generateConstant()));
        }
    }

}
