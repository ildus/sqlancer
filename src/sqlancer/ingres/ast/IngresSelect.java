package sqlancer.ingres.ast;

import java.util.List;
import java.util.stream.Collectors;

import sqlancer.common.ast.SelectBase;
import sqlancer.common.ast.newast.Select;
import sqlancer.ingres.IngresSchema.IngresColumn;
import sqlancer.ingres.IngresSchema.IngresTable;
import sqlancer.ingres.IngresSchema.IngresDataType;
import sqlancer.ingres.IngresToStringVisitor;

public class IngresSelect extends SelectBase<IngresExpression>
        implements Select<IngresJoin, IngresExpression, IngresTable, IngresColumn>, IngresExpression {

    private boolean isDistinct;

    public void setDistinct(boolean isDistinct) {
        this.isDistinct = isDistinct;
    }

    public boolean isDistinct() {
        return isDistinct;
    }

    @Override
    public void setJoinClauses(List<IngresJoin> joinStatements) {
        List<IngresExpression> expressions = joinStatements.stream().map(e -> (IngresExpression) e)
                .collect(Collectors.toList());
        setJoinList(expressions);
    }

    @Override
    public List<IngresJoin> getJoinClauses() {
        return getJoinList().stream().map(e -> (IngresJoin) e).collect(Collectors.toList());
    }

    @Override
    public String asString() {
        return IngresToStringVisitor.asString(this);
    }

    public static class IngresSubquery implements IngresExpression {
        private final IngresSelect s;
        private final String name;

        public IngresSubquery(IngresSelect s, String name) {
            this.s = s;
            this.name = name;
        }

        public IngresSelect getSelect() {
            return s;
        }

        public String getName() {
            return name;
        }

        @Override
        public IngresDataType getExpressionType() {
            return null;
        }
    }
}
