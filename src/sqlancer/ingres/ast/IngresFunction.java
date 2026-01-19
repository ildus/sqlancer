package sqlancer.ingres.ast;

import java.util.List;

import sqlancer.common.ast.newast.NewFunctionNode;

public class IngresFunction<F> extends NewFunctionNode<IngresExpression, F> implements IngresExpression {
    public IngresFunction(List<IngresExpression> args, F func) {
        super(args, func);
    }
}
