package com.github.tartaricacid.touhoulittlemaid.geckolib3.core.molang.value;

import com.github.tartaricacid.touhoulittlemaid.molang.parser.ast.Expression;
import com.github.tartaricacid.touhoulittlemaid.molang.runtime.ExpressionEvaluator;

import java.util.List;

public class MolangValue implements IValue {
    private final List<Expression> expressions;
    private final boolean isUserFunc;

    public MolangValue(List<Expression> expressions, boolean isUserFunc) {
        this.expressions = expressions;
        this.isUserFunc = isUserFunc;
    }

    @Override
    public Object evalUnsafe(ExpressionEvaluator<?> evaluator) {
        // 移植期 ExpressionEvaluator.evalMultiExpressionUnsafe 被移除 → 内联多表达式求值
        Object result = null;
        for (Expression expression : expressions) {
            result = evaluator.eval(expression);
        }
        return result;
    }
}
