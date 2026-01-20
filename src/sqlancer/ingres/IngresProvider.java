package sqlancer.ingres;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import com.google.auto.service.AutoService;

import sqlancer.AbstractAction;
import sqlancer.DatabaseProvider;
import sqlancer.IgnoreMeException;
//import sqlancer.MainOptions;
import sqlancer.Randomly;
import sqlancer.SQLConnection;
import sqlancer.SQLGlobalState;
import sqlancer.SQLProviderAdapter;
import sqlancer.StatementExecutor;
//import sqlancer.common.query.ExpectedErrors;
import sqlancer.common.query.SQLQueryAdapter;
import sqlancer.common.query.SQLQueryProvider;
import sqlancer.ingres.IngresProvider.IngresGlobalState;
import sqlancer.ingres.gen.IngresDeleteGenerator;
import sqlancer.ingres.gen.IngresInsertGenerator;
import sqlancer.ingres.gen.IngresTableGenerator;
import sqlancer.ingres.gen.IngresUpdateGenerator;

@AutoService(DatabaseProvider.class)
public class IngresProvider extends SQLProviderAdapter<IngresGlobalState, IngresOptions> {
    protected String databaseName;
    protected String host;
    protected int port;

    public IngresProvider() {
        super(IngresGlobalState.class, IngresOptions.class);
    }

    public enum Action implements AbstractAction<IngresGlobalState> {
        INSERT(IngresInsertGenerator::getQuery), //
        DELETE(IngresDeleteGenerator::generate), //
        UPDATE(IngresUpdateGenerator::getQuery);

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
        case UPDATE:
            return r.getInteger(0, globalState.getDbmsSpecificOptions().maxNumUpdates + 1);
        case DELETE:
            return r.getInteger(0, globalState.getDbmsSpecificOptions().maxNumDeletes + 1);
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
    public void generateDatabase(IngresGlobalState globalState) throws Exception {
        for (int i = 0; i < Randomly.fromOptions(1, 2); i++) {
            boolean success;
            do {
                SQLQueryAdapter qt = new IngresTableGenerator().getQuery(globalState);
                success = globalState.executeStatement(qt);
            } while (!success);
        }
        if (globalState.getSchema().getDatabaseTables().isEmpty()) {
            throw new IgnoreMeException(); // TODO
        }
        StatementExecutor<IngresGlobalState, Action> se = new StatementExecutor<>(globalState, Action.values(),
                IngresProvider::mapActions, (q) -> {
                    if (globalState.getSchema().getDatabaseTables().isEmpty()) {
                        throw new IgnoreMeException();
                    }
                });
        se.executeStatements();
    }

    @Override
    public SQLConnection createDatabase(IngresGlobalState globalState) throws SQLException {
        databaseName = globalState.getDatabaseName();
        host = globalState.getOptions().getHost();
        port = globalState.getOptions().getPort();

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

        String url = String.format("%s://%s:IK7/%s", "jdbc:ingres", host, databaseName);
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
