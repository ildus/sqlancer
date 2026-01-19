package sqlancer.ingres;

import java.util.List;
import java.util.Arrays;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import sqlancer.DBMSSpecificOptions;

@Parameters(commandDescription = "Ingres")
public class IngresOptions implements DBMSSpecificOptions<IngresOracleFactory> {
    @Parameter(names = "--max-num-deletes", description = "The maximum number of DELETE statements that are issued for a database", arity = 1)
    public int maxNumDeletes = 1;

    @Parameter(names = "--max-num-updates", description = "The maximum number of UPDATE statements that are issued for a database", arity = 1)
    public int maxNumUpdates = 5;

    @Parameter(names = "--oracle")
    public List<IngresOracleFactory> oracles = Arrays.asList(IngresOracleFactory.QUERY_PARTITIONING);

    @Override
    public List<IngresOracleFactory> getTestOracleFactory() {
        return oracles;
    }

}
