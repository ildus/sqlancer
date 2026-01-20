package sqlancer.ingres;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sqlancer.IgnoreMeException;
import sqlancer.Randomly;
import sqlancer.SQLConnection;
import sqlancer.common.DBMSCommon;
import sqlancer.common.schema.AbstractRelationalTable;
import sqlancer.common.schema.AbstractRowValue;
import sqlancer.common.schema.AbstractSchema;
import sqlancer.common.schema.AbstractTableColumn;
import sqlancer.common.schema.AbstractTables;
import sqlancer.common.schema.TableIndex;
import sqlancer.ingres.IngresSchema.IngresTable;
import sqlancer.ingres.IngresSchema.IngresTable.TableType;
import sqlancer.ingres.IngresProvider.IngresGlobalState;
import sqlancer.ingres.ast.IngresConstant;

public class IngresSchema extends AbstractSchema<IngresGlobalState, IngresTable> {

    private final String databaseName;

    public enum IngresDataType {
        BOOLEAN, INT, VARCHAR, FLOAT;

        public static IngresDataType getRandomType() {
            List<IngresDataType> dataTypes = new ArrayList<>(Arrays.asList(values()));
            return Randomly.fromList(dataTypes);
        }
    }

    public static class IngresColumn extends AbstractTableColumn<IngresTable, IngresDataType> {
        public int length;

        public IngresColumn(String name, IngresDataType columnType, int columnLength) {
            super(name, null, columnType);
            if (columnLength == 0 && columnType == IngresDataType.BOOLEAN)
                columnLength = 1;
            if (columnLength == 0 && columnType == IngresDataType.INT)
                columnLength = 4;
            if (columnLength == 0 && columnType == IngresDataType.FLOAT)
                columnLength = 4;
            if (columnLength == 0 && columnType == IngresDataType.VARCHAR)
                columnLength = 255;

            length = columnLength;
        }

        public static IngresColumn createDummy(String name) {
            return new IngresColumn(name, IngresDataType.INT, 0);
        }

    }

    public static class IngresTables extends AbstractTables<IngresTable, IngresColumn> {

        public IngresTables(List<IngresTable> tables) {
            super(tables);
        }

        public IngresRowValue getRandomRowValue(SQLConnection con) throws SQLException {
            // Ingres uses 'SELECT FIRST 1' instead of 'LIMIT 1'
            // and 'ORDER BY RANDOM()' or 'ORDER BY RANDOM_BIT()' 
            String randomRow = String.format("SELECT FIRST 1 %s FROM %s ORDER BY RANDOM()", 
                    columnNamesAsString(c -> c.getTable().getName() + "." + c.getName() + 
                                       " AS " + c.getTable().getName() + c.getName()),
                    tableNamesAsString());

            Map<IngresColumn, IngresConstant> values = new HashMap<>();
            try (Statement s = con.createStatement()) {
                ResultSet randomRowValues = s.executeQuery(randomRow);

                if (!randomRowValues.next()) {
                    throw new AssertionError("could not find random row! " + randomRow + "\n");
                }

                for (int i = 0; i < getColumns().size(); i++) {
                    IngresColumn column = getColumns().get(i);
                    // Construct the alias name we created in the SELECT clause
                    String aliasName = column.getTable().getName() + column.getName();
                    int columnIndex = randomRowValues.findColumn(aliasName);

                    IngresConstant constant;
                    if (randomRowValues.getObject(columnIndex) == null) {
                        constant = IngresConstant.createNullConstant();
                    } else {
                        switch (column.getType()) {
                        case BOOLEAN:
                            constant = IngresConstant.createBooleanConstant(randomRowValues.getBoolean(columnIndex));
                            break;
                        case INT:
                            constant = IngresConstant.createIntConstant(randomRowValues.getLong(columnIndex));
                            break;
                        case VARCHAR:
                            String val = randomRowValues.getString(columnIndex);
                            constant = IngresConstant.createTextConstant(val != null ? val.trim() : null);
                            break;
                        case FLOAT:
                            constant = IngresConstant.createFloatConstant((float)randomRowValues.getDouble(columnIndex));
                            break;
                        default:
                            throw new IgnoreMeException();
                        }
                    }
                    values.put(column, constant);
                }
                return new IngresRowValue(this, values);
            } catch (SQLException e) {
                // Ingres specific error codes can be handled here
                throw new IgnoreMeException();
            }
        }
    }

    private static IngresDataType getColumnType(String type) {
        switch (type) {
            case "INTEGER": return IngresDataType.INT;
            case "VARCHAR": return IngresDataType.VARCHAR;
            case "FLOAT":   return IngresDataType.FLOAT;
            case "BOOLEAN":   return IngresDataType.BOOLEAN;
            default: return IngresDataType.VARCHAR;
        }
    }

    public static class IngresRowValue extends AbstractRowValue<IngresTables, IngresColumn, IngresConstant> {

        protected IngresRowValue(IngresTables tables, Map<IngresColumn, IngresConstant> values) {
            super(tables, values);
        }

    }

    public static class IngresTable
            extends AbstractRelationalTable<IngresColumn, IngresIndex, IngresGlobalState> {

        public enum TableType {
            STANDARD, TEMPORARY, VIEW
        }

        private final TableType tableType;
        private final List<IngresStatObject> statistics;
        private final boolean isInsertable;

        public IngresTable(String tableName, List<IngresColumn> columns, List<IngresIndex> indexes,
                TableType tableType, List<IngresStatObject> statistics, boolean isView, boolean isInsertable) {
            super(tableName, columns, indexes, isView);
            this.statistics = statistics;
            this.isInsertable = isInsertable;
            this.tableType = tableType;
        }

        public List<IngresStatObject> getStatistics() {
            return statistics;
        }

        public TableType getTableType() {
            return tableType;
        }

        public boolean isInsertable() {
            return isInsertable;
        }

    }

    public static final class IngresStatObject {
        private final String name;

        public IngresStatObject(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public static final class IngresIndex extends TableIndex {

        private IngresIndex(String indexName) {
            super(indexName);
        }

        public static IngresIndex create(String indexName) {
            return new IngresIndex(indexName);
        }
    }

    public static IngresSchema fromConnection(SQLConnection con, String databaseName) throws SQLException {
        try {
            List<IngresTable> databaseTables = new ArrayList<>();

            // Querying iitables instead of information_schema
            // table_type: 'T' = Table, 'V' = View, 'G' = Global Temp Table
            String sql = "SELECT table_name, table_type FROM iitables " +
                         "WHERE table_owner != '$ingres' " +
                         "AND table_type = 'T' " + // limit to tables only for now
                         "AND system_use = 'U' " + // 'U' ensures we only get User tables
                         "ORDER BY table_name;";

            try (Statement s = con.createStatement()) {
                try (ResultSet rs = s.executeQuery(sql)) {
                    while (rs.next()) {
                        String tableName = rs.getString("table_name").trim();
                        String rawTableType = rs.getString("table_type").trim();

                        // Use the Ingres-specific getTableType logic we wrote previously
                        IngresTable.TableType tableType = getTableType(rawTableType);

                        // Fetch nested metadata
                        List<IngresColumn> databaseColumns = getTableColumns(con, tableName);
                        List<IngresIndex> indexes = getIndexes(con, tableName);

                        // Note: Statistics in Ingres are often column-based
                        List<IngresStatObject> statistics = getStatistics(con);

                        boolean isView = rawTableType.equals("V");
                        boolean isInsertable = !isView; // Simplified logic for fuzzer

                        IngresTable t = new IngresTable(
                            tableName,
                            databaseColumns,
                            indexes,
                            tableType,
                            statistics,
                            isView,
                            isInsertable
                        );

                        for (IngresColumn c : databaseColumns) {
                            c.setTable(t);
                        }
                        databaseTables.add(t);
                    }
                }
            }
            return new IngresSchema(databaseTables, databaseName);
        } catch (SQLException e) {
            throw new AssertionError(e);
        }
    }

    protected static List<IngresStatObject> getStatistics(SQLConnection con) throws SQLException {
        List<IngresStatObject> statistics = new ArrayList<>();

        String sql = "SELECT DISTINCT t.table_name, c.column_name " +
                     "FROM iistatistics s " +
                     "JOIN iitables t ON s.stabbase = t.table_reltid " +
                     "JOIN iicolumns c ON s.satno = c.column_index AND t.table_name = c.table_name " +
                     "WHERE t.table_owner != '$ingres' " +
                     "ORDER BY t.table_name;";

        try (Statement s = con.createStatement()) {
            try (ResultSet rs = s.executeQuery(sql)) {
                while (rs.next()) {
                    String tableName = rs.getString(1).trim();
                    String columnName = rs.getString(2).trim();

                    // Create a unique identifier for SQLancer
                    String statName = tableName + "_" + columnName + "_stat";
                    statistics.add(new IngresStatObject(statName));
                }
            }
        }
        return statistics;
    }

    protected static IngresTable.TableType getTableType(String tableTypeStr) throws AssertionError {
        IngresTable.TableType tableType;
        String sanitizedType = tableTypeStr.trim().toUpperCase();

        switch (sanitizedType) {
            case "T":
                tableType = TableType.STANDARD;
                break;
            case "G":
                tableType = TableType.TEMPORARY;
                break;
            case "V":
                tableType = TableType.VIEW;
                break;
            default:
                // Ingres also has 'S' for System tables, 
                // but usually we want to throw an error if SQLancer hits them
                throw new AssertionError("Unknown Ingres table type: " + sanitizedType);
        }
        return tableType;
    }

    protected static List<IngresIndex> getIndexes(SQLConnection con, String tableName) throws SQLException {
        List<IngresIndex> indexes = new ArrayList<>();

        String sql = String.format(
            "SELECT index_name FROM iiindexes WHERE base_name = '%s' ORDER BY index_name;", tableName
        );

        try (Statement s = con.createStatement()) {
            try (ResultSet rs = s.executeQuery(sql)) {
                while (rs.next()) {
                    String indexName = rs.getString("index_name").trim();
                    if (DBMSCommon.matchesIndexName(indexName)) {
                        indexes.add(IngresIndex.create(indexName));
                    }
                }
            }
        }
        return indexes;
    }

    protected static List<IngresColumn> getTableColumns(SQLConnection con, String tableName) throws SQLException {
        List<IngresColumn> columns = new ArrayList<>();
        try (Statement s = con.createStatement()) {
            try (ResultSet rs = s
                    .executeQuery("SELECT column_name, column_datatype, column_length, column_nulls FROM iicolumns WHERE table_name = '"
                            + tableName + "' ORDER BY column_name")) {
                while (rs.next()) {
                    String columnName = rs.getString("column_name");
                    String dataType = rs.getString("column_datatype");
                    int columnLength = Integer.parseInt(rs.getString("column_length"));

                    IngresColumn c = new IngresColumn(columnName, getColumnType(dataType), columnLength);
                    columns.add(c);
                }
            }
        }
        return columns;
    }

    public IngresSchema(List<IngresTable> databaseTables, String databaseName) {
        super(databaseTables);
        this.databaseName = databaseName;
    }

    public IngresTables getRandomTableNonEmptyTables() {
        return new IngresTables(Randomly.nonEmptySubset(getDatabaseTables()));
    }

    public String getDatabaseName() {
        return databaseName;
    }

}
