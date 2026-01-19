package sqlancer.ingres.ast;

import java.util.List;

import sqlancer.common.ast.newast.NewCaseOperatorNode;

public class IngresCaseOperator extends NewCaseOperatorNode<IngresExpression> implements IngresExpression {
    public IngresCaseOperator(IngresExpression switchCondition, List<IngresExpression> conditions,
            List<IngresExpression> expressions, IngresExpression elseExpr) {
        super(switchCondition, conditions, expressions, elseExpr);
    }
}
