package sqlancer.ingres.gen;

import java.util.ArrayList;
import java.util.List;

import sqlancer.Randomly;
import sqlancer.common.query.ExpectedErrors;
import sqlancer.common.query.SQLQueryAdapter;
import sqlancer.ingres.IngresProvider.IngresGlobalState;
import sqlancer.ingres.IngresSchema.IngresColumn;
import sqlancer.ingres.IngresSchema.IngresDataType;

public class IngresTableGenerator {

    public SQLQueryAdapter getQuery(IngresGlobalState globalState) {
        ExpectedErrors errors = new ExpectedErrors();
        StringBuilder sb = new StringBuilder();
        String tableName = globalState.getSchema().getFreeTableName();
        sb.append("CREATE TABLE ");
        sb.append(tableName);
        sb.append("(");
        List<IngresColumn> columns = getNewColumns();

        for (int i = 0; i < columns.size(); i++) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(columns.get(i).getName());
            sb.append(" ");
            sb.append(columns.get(i).getType());

            if (columns.get(i).length > 0)
                sb.append(String.format("(%s)", columns.get(i).length));

            if (Randomly.getBooleanWithRatherLowProbability()) {
                sb.append(" NOT NULL");
            }
        }
        sb.append(")");
        return new SQLQueryAdapter(sb.toString(), errors, true);
    }

    public static String getRandomCollate() {
        return Randomly.fromOptions("C", "POSIX");
    }

    private static List<IngresColumn> getNewColumns() {
        List<IngresColumn> columns = new ArrayList<>();
        for (int i = 0; i < Randomly.smallNumber() + 1; i++) {
            String columnName = String.format("c%d", i);
            IngresDataType columnType = IngresDataType.getRandomType();

            columns.add(new IngresColumn(columnName, columnType, 0));
        }
        return columns;
    }

}
