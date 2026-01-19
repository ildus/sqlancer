package sqlancer.ingres.gen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import sqlancer.Randomly;
import sqlancer.common.ast.BinaryOperatorNode.Operator;
import sqlancer.common.ast.newast.NewOrderingTerm.Ordering;
import sqlancer.common.ast.newast.NewUnaryPostfixOperatorNode;
import sqlancer.common.gen.NoRECGenerator;
import sqlancer.common.gen.TLPWhereGenerator;
import sqlancer.common.gen.UntypedExpressionGenerator;
import sqlancer.common.schema.AbstractTables;
import sqlancer.ingres.IngresProvider.IngresGlobalState;
import sqlancer.ingres.IngresSchema.IngresColumn;
import sqlancer.ingres.IngresSchema.IngresDataType;
import sqlancer.ingres.IngresSchema.IngresTable;
import sqlancer.ingres.IngresToStringVisitor;
import sqlancer.ingres.ast.IngresBetweenOperator;
import sqlancer.ingres.ast.IngresBinaryOperator;
import sqlancer.ingres.ast.IngresCaseOperator;
import sqlancer.ingres.ast.IngresColumnReference;
import sqlancer.ingres.ast.IngresConstant;
import sqlancer.ingres.ast.IngresExpression;
import sqlancer.ingres.ast.IngresFunction;
import sqlancer.ingres.ast.IngresInOperator;
import sqlancer.ingres.ast.IngresJoin;
import sqlancer.ingres.ast.IngresOrderingTerm;
import sqlancer.ingres.ast.IngresPostFixText;
import sqlancer.ingres.ast.IngresSelect;
import sqlancer.ingres.ast.IngresTableReference;
import sqlancer.ingres.ast.IngresTernary;

public final class IngresExpressionGenerator extends UntypedExpressionGenerator<IngresExpression, IngresColumn>
        implements NoRECGenerator<IngresSelect, IngresJoin, IngresExpression, IngresTable, IngresColumn>,
        TLPWhereGenerator<IngresSelect, IngresJoin, IngresExpression, IngresTable, IngresColumn> {

    private final IngresGlobalState globalState;
    private List<IngresTable> tables;

    public IngresExpressionGenerator(IngresGlobalState globalState) {
        this.globalState = globalState;
    }

    private enum Expression {
        UNARY_POSTFIX, UNARY_PREFIX, BINARY_COMPARISON, BINARY_LOGICAL, BINARY_ARITHMETIC, CAST, FUNC, BETWEEN, CASE,
        IN, COLLATE, LIKE_ESCAPE
    }

    @Override
    protected IngresExpression generateExpression(int depth) {
        if (depth >= globalState.getOptions().getMaxExpressionDepth() || Randomly.getBoolean()) {
            return generateLeafNode();
        }
        if (allowAggregates && Randomly.getBoolean()) {
            IngresAggregateFunction aggregate = IngresAggregateFunction.getRandom();
            allowAggregates = false;
            return new IngresFunction<>(generateExpressions(aggregate.getNrArgs(), depth + 1), aggregate);
        }
        List<Expression> possibleOptions = new ArrayList<>(Arrays.asList(Expression.values()));
        Expression expr = Randomly.fromList(possibleOptions);
        switch (expr) {
        case COLLATE:
            return new sqlancer.ingres.ast.IngresUnaryPostfixOperator(generateExpression(depth + 1),
                    IngresCollate.getRandom());
        case UNARY_PREFIX:
            return new sqlancer.ingres.ast.IngresUnaryPrefixOperator(generateExpression(depth + 1),
                    IngresUnaryPrefixOperator.getRandom());
        case UNARY_POSTFIX:
            return new sqlancer.ingres.ast.IngresUnaryPostfixOperator(generateExpression(depth + 1),
                    IngresUnaryPostfixOperator.getRandom());
        case BINARY_COMPARISON:
            Operator op = IngresBinaryComparisonOperator.getRandom();
            return new IngresBinaryOperator(generateExpression(depth + 1), generateExpression(depth + 1), op);
        case BINARY_LOGICAL:
            op = IngresBinaryLogicalOperator.getRandom();
            return new IngresBinaryOperator(generateExpression(depth + 1), generateExpression(depth + 1), op);
        case BINARY_ARITHMETIC:
            return new IngresBinaryOperator(generateExpression(depth + 1), generateExpression(depth + 1),
                    IngresBinaryArithmeticOperator.getRandom());
        case CAST:
            return new IngresCastOperation(generateExpression(depth + 1),
                    IngresDataType.getRandomType());
        case FUNC:
            DBFunction func = DBFunction.getRandom();
            return new IngresFunction<>(generateExpressions(func.getNrArgs()), func);
        case BETWEEN:
            return new IngresBetweenOperator(generateExpression(depth + 1), generateExpression(depth + 1),
                    generateExpression(depth + 1), Randomly.getBoolean());
        case IN:
            return new IngresInOperator(generateExpression(depth + 1),
                    generateExpressions(Randomly.smallNumber() + 1, depth + 1), Randomly.getBoolean());
        case CASE:
            int nr = Randomly.smallNumber() + 1;
            return new IngresCaseOperator(generateExpression(depth + 1), generateExpressions(nr, depth + 1),
                    generateExpressions(nr, depth + 1), generateExpression(depth + 1));
        case LIKE_ESCAPE:
            return new IngresTernary(generateExpression(depth + 1), generateExpression(depth + 1),
                    generateExpression(depth + 1), "LIKE", "ESCAPE");
        default:
            throw new AssertionError();
        }
    }

    @Override
    protected IngresExpression generateColumn() {
        IngresColumn column = Randomly.fromList(columns);
        return new IngresColumnReference(column);
    }

    @Override
    public IngresExpression generateConstant() {
        if (Randomly.getBooleanWithSmallProbability()) {
            return IngresConstant.createNullConstant();
        }
        IngresDataType type = IngresDataType.getRandomType();
        switch (type) {
        case INT:
            return IngresConstant.createIntConstant(globalState.getRandomly().getInteger());
        case TEXT:
            return IngresConstant.createTextConstant(globalState.getRandomly().getString());
        case BOOLEAN:
            return IngresConstant.createBooleanConstant(Randomly.getBoolean());
        case FLOAT:
            return IngresConstant.createFloatConstant((float)globalState.getRandomly().getDouble());
        default:
            throw new AssertionError();
        }
    }

    @Override
    public List<IngresExpression> generateOrderBys() {
        List<IngresExpression> expr = super.generateOrderBys();
        List<IngresExpression> newExpr = new ArrayList<>(expr.size());
        for (IngresExpression curExpr : expr) {
            if (Randomly.getBoolean()) {
                curExpr = new IngresOrderingTerm(curExpr, Ordering.getRandom());
            }
            newExpr.add(curExpr);
        }
        return newExpr;
    };

    public static class IngresCastOperation extends NewUnaryPostfixOperatorNode<IngresExpression>
            implements IngresExpression {

        public IngresCastOperation(IngresExpression expr, IngresDataType type) {
            super(expr, new Operator() {

                @Override
                public String getTextRepresentation() {
                    return String.format("cast(%s, %s)", expr.toString(), type.toString());
                }
            });
        }

    }

    public enum IngresAggregateFunction {
        MAX(1), MIN(1), AVG(1), COUNT(1);

        private int nrArgs;

        IngresAggregateFunction(int nrArgs) {
            this.nrArgs = nrArgs;
        }

        public static IngresAggregateFunction getRandom() {
            return Randomly.fromOptions(values());
        }

        public int getNrArgs() {
            return nrArgs;
        }

    }

    public enum DBFunction {
        // trigonometric functions
        LENGTH(1), //
        LOWER(1), //
        UPPER(1), //
        SUBSTRING(3), //
        REVERSE(1), //
        REPLACE(3);

        private int nrArgs;
        private boolean isVariadic;

        DBFunction(int nrArgs) {
            this(nrArgs, false);
        }

        DBFunction(int nrArgs, boolean isVariadic) {
            this.nrArgs = nrArgs;
            this.isVariadic = isVariadic;
        }

        public static DBFunction getRandom() {
            return Randomly.fromOptions(values());
        }

        public int getNrArgs() {
            if (isVariadic) {
                return Randomly.smallNumber() + nrArgs;
            } else {
                return nrArgs;
            }
        }

    }

    public enum IngresUnaryPostfixOperator implements Operator {

        IS_NULL("IS NULL"), IS_NOT_NULL("IS NOT NULL");

        private String textRepr;

        IngresUnaryPostfixOperator(String textRepr) {
            this.textRepr = textRepr;
        }

        @Override
        public String getTextRepresentation() {
            return textRepr;
        }

        public static IngresUnaryPostfixOperator getRandom() {
            return Randomly.fromOptions(values());
        }

    }

    public static final class IngresCollate implements Operator {

        private final String textRepr;

        private IngresCollate(String textRepr) {
            this.textRepr = textRepr;
        }

        @Override
        public String getTextRepresentation() {
            return "COLLATE " + textRepr;
        }

        public static IngresCollate getRandom() {
            return new IngresCollate(IngresTableGenerator.getRandomCollate());
        }

    }

    public enum IngresUnaryPrefixOperator implements Operator {

        NOT("NOT"), PLUS("+"), MINUS("-");

        private String textRepr;

        IngresUnaryPrefixOperator(String textRepr) {
            this.textRepr = textRepr;
        }

        @Override
        public String getTextRepresentation() {
            return textRepr;
        }

        public static IngresUnaryPrefixOperator getRandom() {
            return Randomly.fromOptions(values());
        }

    }

    public enum IngresBinaryLogicalOperator implements Operator {

        AND, OR;

        @Override
        public String getTextRepresentation() {
            return toString();
        }

        public static Operator getRandom() {
            return Randomly.fromOptions(values());
        }

    }

    public enum IngresBinaryArithmeticOperator implements Operator {
        CONCAT("||"), ADD("+"), SUB("-"), MULT("*"), DIV("/"), MOD("%"), AND("&"), OR("|"), LSHIFT("<<"), RSHIFT(">>");

        private String textRepr;

        IngresBinaryArithmeticOperator(String textRepr) {
            this.textRepr = textRepr;
        }

        public static Operator getRandom() {
            return Randomly.fromOptions(values());
        }

        @Override
        public String getTextRepresentation() {
            return textRepr;
        }

    }

    public enum IngresBinaryComparisonOperator implements Operator {
        EQUALS("="), GREATER(">"), GREATER_EQUALS(">="), SMALLER("<"), SMALLER_EQUALS("<="), NOT_EQUALS("!="),
        LIKE("LIKE"), NOT_LIKE("NOT LIKE"), SIMILAR_TO("SIMILAR TO"), NOT_SIMILAR_TO("NOT SIMILAR TO");

        private String textRepr;

        IngresBinaryComparisonOperator(String textRepr) {
            this.textRepr = textRepr;
        }

        public static Operator getRandom() {
            return Randomly.fromOptions(values());
        }

        @Override
        public String getTextRepresentation() {
            return textRepr;
        }

    }

    public IngresFunction<IngresAggregateFunction> generateArgsForAggregate(IngresAggregateFunction aggregateFunction) {
        return new IngresFunction<>(generateExpressions(aggregateFunction.getNrArgs()), aggregateFunction);
    }

    public IngresExpression generateAggregate() {
        IngresAggregateFunction aggrFunc = IngresAggregateFunction.getRandom();
        return generateArgsForAggregate(aggrFunc);
    }

    @Override
    public IngresExpression negatePredicate(IngresExpression predicate) {
        return new sqlancer.ingres.ast.IngresUnaryPrefixOperator(predicate, IngresUnaryPrefixOperator.NOT);
    }

    @Override
    public IngresExpression isNull(IngresExpression expr) {
        return new sqlancer.ingres.ast.IngresUnaryPostfixOperator(expr, IngresUnaryPostfixOperator.IS_NULL);
    }

    @Override
    public IngresExpressionGenerator setTablesAndColumns(AbstractTables<IngresTable, IngresColumn> tables) {
        this.columns = tables.getColumns();
        this.tables = tables.getTables();

        return this;
    }

    @Override
    public IngresExpression generateBooleanExpression() {
        return generateExpression();
    }

    @Override
    public IngresSelect generateSelect() {
        return new IngresSelect();
    }

    @Override
    public List<IngresJoin> getRandomJoinClauses() {
        List<IngresTableReference> tableList = tables.stream().map(t -> new IngresTableReference(t))
                .collect(Collectors.toList());
        List<IngresJoin> joins = IngresJoin.getJoins(tableList, globalState);
        tables = tableList.stream().map(t -> t.getTable()).collect(Collectors.toList());
        return joins;
    }

    @Override
    public List<IngresExpression> getTableRefs() {
        return tables.stream().map(t -> new IngresTableReference(t)).collect(Collectors.toList());
    }

    @Override
    public String generateOptimizedQueryString(IngresSelect select, IngresExpression whereCondition,
            boolean shouldUseAggregate) {
        List<IngresExpression> allColumns = columns.stream().map((c) -> new IngresColumnReference(c))
                .collect(Collectors.toList());
        if (shouldUseAggregate) {
            IngresFunction<IngresAggregateFunction> aggr = new IngresFunction<>(
                    Arrays.asList(new IngresColumnReference(
                            new IngresColumn("*", IngresDataType.INT))),
                    IngresAggregateFunction.COUNT);
            select.setFetchColumns(Arrays.asList(aggr));
        } else {
            select.setFetchColumns(allColumns);
            if (Randomly.getBooleanWithSmallProbability()) {
                select.setOrderByClauses(generateOrderBys());
            }
        }
        select.setWhereClause(whereCondition);

        return select.asString();
    }

    @Override
    public String generateUnoptimizedQueryString(IngresSelect select, IngresExpression whereCondition) {
        IngresExpression asText = new IngresPostFixText(new IngresCastOperation(
                new IngresPostFixText(whereCondition,
                        " IS NOT NULL AND " + IngresToStringVisitor.asString(whereCondition)),
                IngresDataType.INT), "as count");
        select.setFetchColumns(Arrays.asList(asText));

        return "SELECT SUM(count) FROM (" + select.asString() + ") as res";
    }

    @Override
    public List<IngresExpression> generateFetchColumns(boolean shouldCreateDummy) {
        if (Randomly.getBoolean()) {
            return List.of(new IngresColumnReference(new IngresColumn("*", null)));
        }
        return Randomly.nonEmptySubset(columns).stream().map(c -> new IngresColumnReference(c))
                .collect(Collectors.toList());
    }
}
