package sqlancer.ingres;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import com.google.auto.service.AutoService;

import sqlancer.AbstractAction;
import sqlancer.DatabaseProvider;
import sqlancer.IgnoreMeException;
import sqlancer.MainOptions;
import sqlancer.Randomly;
import sqlancer.SQLConnection;
import sqlancer.SQLGlobalState;
import sqlancer.SQLProviderAdapter;
import sqlancer.StatementExecutor;
import sqlancer.common.query.ExpectedErrors;
import sqlancer.common.query.SQLQueryAdapter;
import sqlancer.common.query.SQLQueryProvider;
import sqlancer.duckdb.IngresProvider.IngresGlobalState;

@AutoService(DatabaseProvider.class)
public class IngresProvider extends SQLProviderAdapter<IngresGlobalState, IngresOptions> {

    public IngresProvider() {
        super(IngresGlobalState.class, IngresOptions.class);
    }

    public enum Action implements AbstractAction<IngresGlobalState> {
        INSERT(IngresInsertGenerator::getQuery), //
        CREATE_INDEX(IngresIndexGenerator::getQuery), //
        DELETE(IngresDeleteGenerator::generate), //
        UPDATE(IngresUpdateGenerator::getQuery), //
        CREATE_VIEW(IngresViewGenerator::generate), //
        EXPLAIN((g) -> {
            ExpectedErrors errors = new ExpectedErrors();
            IngresErrors.addExpressionErrors(errors);
            IngresErrors.addGroupByErrors(errors);
            return new SQLQueryAdapter(
                    "EXPLAIN " + IngresToStringVisitor
                            .asString(IngresRandomQuerySynthesizer.generateSelect(g, Randomly.smallNumber() + 1)),
                    errors);
        });

        private final SQLQueryProvider<IngresGlobalState> sqlQueryProvider;

        Action(SQLQueryProvider<IngresGlobalState> sqlQueryProvider) {
            this.sqlQueryProvider = sqlQueryProvider;
        }

        @Override
        public SQLQueryAdapter getQuery(IngresGlobalState state) throws Exception {
            return sqlQueryProvider.getQuery(state);
        }
    }

    private static int mapActions(IngresGlobalState globalState, Action a) {
        Randomly r = globalState.getRandomly();
        switch (a) {
        case INSERT:
            return r.getInteger(0, globalState.getOptions().getMaxNumberInserts());
        case CREATE_INDEX:
            if (!globalState.getDbmsSpecificOptions().testIndexes) {
                return 0;
            }
            // fall through
        case UPDATE:
            return r.getInteger(0, globalState.getDbmsSpecificOptions().maxNumUpdates + 1);
        case EXPLAIN:
            return r.getInteger(0, 2);
        case DELETE:
            return r.getInteger(0, globalState.getDbmsSpecificOptions().maxNumDeletes + 1);
        case CREATE_VIEW:
            return r.getInteger(0, globalState.getDbmsSpecificOptions().maxNumViews + 1);
        default:
            throw new AssertionError(a);
        }
    }

    public static class IngresGlobalState extends SQLGlobalState<IngresOptions, IngresSchema> {
        @Override
        protected IngresSchema readSchema() throws SQLException {
            return IngresSchema.fromConnection(getConnection(), getDatabaseName());
        }
    }

    @Override
    public void generateDatabase(PostgresGlobalState globalState) throws Exception {
        readFunctions(globalState);
        createTables(globalState, Randomly.fromOptions(4, 5, 6));
        prepareTables(globalState);
    }

    @Override
    public SQLConnection createDatabase(IngresGlobalState globalState) throws SQLException {
        try {
            ProcessBuilder pbDestroy = new ProcessBuilder("destroydb", databaseName);
            // Redirecting to inheritIO can help debugging, or use discard
            pbDestroy.start().waitFor();
        } catch (Exception e) {
            // We often ignore errors here because destroydb fails if the db doesn't exist
        }

        try {
            ProcessBuilder pbCreate = new ProcessBuilder("createdb", databaseName);
            int exitCode = pbCreate.start().waitFor();
            if (exitCode != 0) {
                throw new SQLException("createdb failed with exit code " + exitCode);
            }
        } catch (Exception e) {
            throw new SQLException("Failed to execute createdb utility", e);
        }

        // 3. Establish the JDBC connection to the newly created database
        // Ensure url is: jdbc:ingres://localhost:21064/databaseName
        Connection conn = DriverManager.getConnection(url);

        try (Statement stmt = conn.createStatement()) {
            // Ensure rows are returned immediately for evaluation
            stmt.execute("SET NOJOURNALING");
            // Avoid hanging on locks during high-concurrency fuzzing
            stmt.execute("SET LOCKMODE SESSION WHERE READLOCK = NOLOCK");
        }

        return new SQLConnection(conn);
    }

    @Override
    public String getDBMSName() {
        return "ingres";
    }

}
