package sqlancer.ingres;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import sqlancer.OracleFactory;
import sqlancer.common.oracle.CompositeTestOracle;
import sqlancer.common.oracle.NoRECOracle;
import sqlancer.common.oracle.TestOracle;
import sqlancer.common.oracle.TLPWhereOracle;
import sqlancer.common.query.ExpectedErrors;

import sqlancer.ingres.gen.IngresExpressionGenerator;

public enum IngresOracleFactory implements OracleFactory<IngresProvider.IngresGlobalState> {
    NOREC {
        @Override
        public TestOracle<IngresProvider.IngresGlobalState> create(IngresProvider.IngresGlobalState globalState)
                throws SQLException {
            IngresExpressionGenerator gen = new IngresExpressionGenerator(globalState);
            ExpectedErrors expectedErrors = ExpectedErrors.newErrors().build();
            return new NoRECOracle<>(globalState, gen, expectedErrors);
        }
    },
    WHERE {
        @Override
        public TestOracle<IngresProvider.IngresGlobalState> create(IngresProvider.IngresGlobalState globalState)
                throws SQLException {
            IngresExpressionGenerator gen = new IngresExpressionGenerator(globalState);
            ExpectedErrors expectedErrors = ExpectedErrors.newErrors().build();

            return new TLPWhereOracle<>(globalState, gen, expectedErrors);
        }
    },
    QUERY_PARTITIONING {
        @Override
        public TestOracle<IngresProvider.IngresGlobalState> create(IngresProvider.IngresGlobalState globalState)
                throws Exception {
            List<TestOracle<IngresProvider.IngresGlobalState>> oracles = new ArrayList<>();
            oracles.add(WHERE.create(globalState));
            return new CompositeTestOracle<IngresProvider.IngresGlobalState>(oracles, globalState);
        }
    };
}
