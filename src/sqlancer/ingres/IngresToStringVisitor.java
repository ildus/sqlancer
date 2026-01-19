package sqlancer.ingres;

import sqlancer.common.ast.newast.NewToStringVisitor;
import sqlancer.common.ast.newast.TableReferenceNode;
import sqlancer.ingres.ast.IngresConstant;
import sqlancer.ingres.ast.IngresExpression;
import sqlancer.ingres.ast.IngresJoin;
import sqlancer.ingres.ast.IngresSelect;

public class IngresToStringVisitor extends NewToStringVisitor<IngresExpression> {

    @Override
    public void visitSpecific(IngresExpression expr) {
        if (expr instanceof IngresConstant) {
            visit((IngresConstant) expr);
        } else if (expr instanceof IngresSelect) {
            visit((IngresSelect) expr);
        } else if (expr instanceof IngresJoin) {
            visit((IngresJoin) expr);
        } else {
            throw new AssertionError(expr.getClass());
        }
    }

    private void visit(IngresJoin join) {
        visit((TableReferenceNode<IngresExpression, IngresSchema.IngresTable>) join.getLeftTable());
        sb.append(" ");
        sb.append(join.getJoinType());
        sb.append(" ");
        if (join.getOuterType() != null) {
            sb.append(join.getOuterType());
        }
        sb.append(" JOIN ");
        visit((TableReferenceNode<IngresExpression, IngresSchema.IngresTable>) join.getRightTable());
        if (join.getOnCondition() != null) {
            sb.append(" ON ");
            visit(join.getOnCondition());
        }
    }

    private void visit(IngresConstant constant) {
        sb.append(constant.toString());
    }

    private void visit(IngresSelect select) {
        sb.append("SELECT ");
        if (select.isDistinct()) {
            sb.append("DISTINCT ");
        }
        if (select.getLimitClause() != null) {
            sb.append(" FIRST ");
            visit(select.getLimitClause());
        }
        visit(select.getFetchColumns());
        sb.append(" FROM ");
        visit(select.getFromList());
        if (!select.getFromList().isEmpty() && !select.getJoinList().isEmpty()) {
            sb.append(", ");
        }
        if (!select.getJoinList().isEmpty()) {
            visit(select.getJoinList());
        }
        if (select.getWhereClause() != null) {
            sb.append(" WHERE ");
            visit(select.getWhereClause());
        }
        if (!select.getGroupByExpressions().isEmpty()) {
            sb.append(" GROUP BY ");
            visit(select.getGroupByExpressions());
        }
        if (!select.getOrderByClauses().isEmpty()) {
            sb.append(" ORDER BY ");
            visit(select.getOrderByClauses());
        }
        if (select.getOffsetClause() != null) {
            sb.append(" OFFSET ");
            visit(select.getOffsetClause());
        }
    }

    public static String asString(IngresExpression expr) {
        IngresToStringVisitor visitor = new IngresToStringVisitor();
        visitor.visit(expr);
        return visitor.get();
    }

}
