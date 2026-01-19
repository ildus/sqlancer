package sqlancer.ingres.ast;

import java.util.ArrayList;
import java.util.List;

import sqlancer.Randomly;
import sqlancer.common.ast.newast.Join;
import sqlancer.ingres.IngresProvider.IngresGlobalState;
import sqlancer.ingres.IngresSchema.IngresColumn;
import sqlancer.ingres.IngresSchema.IngresTable;
import sqlancer.ingres.gen.IngresExpressionGenerator;

public class IngresJoin implements IngresExpression, Join<IngresExpression, IngresTable, IngresColumn> {

    private final IngresTableReference leftTable;
    private final IngresTableReference rightTable;
    private final JoinType joinType;
    private IngresExpression onCondition;
    private OuterType outerType;

    public enum JoinType {
        INNER, LEFT, RIGHT;

        public static JoinType getRandom() {
            return Randomly.fromOptions(values());
        }
    }

    public enum OuterType {
        FULL, LEFT, RIGHT;

        public static OuterType getRandom() {
            return Randomly.fromOptions(values());
        }
    }

    public IngresJoin(IngresTableReference leftTable, IngresTableReference rightTable, JoinType joinType,
            IngresExpression whereCondition) {
        this.leftTable = leftTable;
        this.rightTable = rightTable;
        this.joinType = joinType;
        this.onCondition = whereCondition;
    }

    public IngresTableReference getLeftTable() {
        return leftTable;
    }

    public IngresTableReference getRightTable() {
        return rightTable;
    }

    public JoinType getJoinType() {
        return joinType;
    }

    public IngresExpression getOnCondition() {
        return onCondition;
    }

    public OuterType getOuterType() {
        return outerType;
    }

    public static List<IngresJoin> getJoins(List<IngresTableReference> tableList, IngresGlobalState globalState) {
        List<IngresJoin> joinExpressions = new ArrayList<>();
        while (tableList.size() >= 2 && Randomly.getBooleanWithRatherLowProbability()) {
            IngresTableReference leftTable = tableList.remove(0);
            IngresTableReference rightTable = tableList.remove(0);
            List<IngresColumn> columns = new ArrayList<>(leftTable.getTable().getColumns());
            columns.addAll(rightTable.getTable().getColumns());
            IngresExpressionGenerator joinGen = new IngresExpressionGenerator(globalState).setColumns(columns);
            switch (IngresJoin.JoinType.getRandom()) {
            case INNER:
                joinExpressions.add(IngresJoin.createInnerJoin(leftTable, rightTable, joinGen.generateExpression()));
                break;
            case LEFT:
                joinExpressions
                        .add(IngresJoin.createLeftOuterJoin(leftTable, rightTable, joinGen.generateExpression()));
                break;
            case RIGHT:
                joinExpressions
                        .add(IngresJoin.createRightOuterJoin(leftTable, rightTable, joinGen.generateExpression()));
                break;
            default:
                throw new AssertionError();
            }
        }
        return joinExpressions;
    }

    public static IngresJoin createRightOuterJoin(IngresTableReference left, IngresTableReference right,
            IngresExpression predicate) {
        return new IngresJoin(left, right, JoinType.RIGHT, predicate);
    }

    public static IngresJoin createLeftOuterJoin(IngresTableReference left, IngresTableReference right,
            IngresExpression predicate) {
        return new IngresJoin(left, right, JoinType.LEFT, predicate);
    }

    public static IngresJoin createInnerJoin(IngresTableReference left, IngresTableReference right,
            IngresExpression predicate) {
        return new IngresJoin(left, right, JoinType.INNER, predicate);
    }

    @Override
    public void setOnClause(IngresExpression onClause) {
        this.onCondition = onClause;
    }
}
