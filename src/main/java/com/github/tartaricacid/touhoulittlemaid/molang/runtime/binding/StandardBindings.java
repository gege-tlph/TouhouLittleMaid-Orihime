/*
 * This file is part of molang, licensed under the MIT license
 *
 * Copyright (c) 2021-2023 Unnamed Team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.github.tartaricacid.touhoulittlemaid.molang.runtime.binding;

import com.github.tartaricacid.touhoulittlemaid.molang.parser.ast.*;
import com.github.tartaricacid.touhoulittlemaid.molang.runtime.AssignableVariable;
import com.github.tartaricacid.touhoulittlemaid.molang.runtime.Function;

import java.util.Arrays;

/**
 * 包含一些默认绑定和静态实用方法的类，以便于使用绑定
 */
public final class StandardBindings {
    private static final int MAX_LOOP_ROUND = 1024;

    public static final Function LOOP_FUNC = (ctx, args) -> {
        // 参数： - double：我们应该循环多少次 - CallableBinding：循环表达式

        if (args.size() < 2) {
            return null;
        }

        int n = Math.min((int) Math.round(args.getAsDouble(ctx, 0)), MAX_LOOP_ROUND);
        Object expr = args.getExpression(1);

        if (expr instanceof ExecutionScopeExpression) {
            Function callable = ((ExecutionScopeExpression) expr).buildFunction((ExpressionVisitor<?>) ctx);
            if (callable != null) {
                for (int i = 0; i < n; i++) {
                    Object value = callable.evaluate(ctx, Function.EMPTY_ARGUMENT);
                    if (value == StatementExpression.Op.BREAK) {
                        break;
                    }

                }
            }
        }
        return null;
    };

    public static final Function FOR_EACH_FUNC = (ctx, args) -> {
        // 参数： - any：变量 - array：任意数组 - CallableBinding：循环表达式

        if (args.size() < 3) {
            return null;
        }

        final Expression variableExpr = args.getExpression(0);
        if (!(variableExpr instanceof AssignableVariableExpression)) {
            // 第一个参数必须是访问表达式，例如'variable.test'、'v.pig'、't.entity' 或 't.entity.location.world'
            return null;
        }
        final AssignableVariable variableAccess = ((AssignableVariableExpression) variableExpr).target();

        final Object array = args.getValue(ctx, 1);
        final Iterable<?> arrayIterable;
        if (array instanceof Object[]) {
            arrayIterable = Arrays.asList((Object[]) array);
        } else if (array instanceof Iterable<?>) {
            arrayIterable = (Iterable<?>) array;
        } else {
            // 第二个参数必须是数组或可迭代
            return null;
        }

        final Expression expr = args.getExpression(2);

        if (expr instanceof ExecutionScopeExpression) {
            Function callable = ((ExecutionScopeExpression) expr).buildFunction((ExpressionVisitor<?>) ctx);
            if (callable != null) {
                for (final Object val : arrayIterable) {
                    // 将 'val' 设置为当前值 eval (objectExpr.propertyName = val)
                    variableAccess.assign(ctx, val);
                    final Object returnValue = callable.evaluate(ctx, Function.EMPTY_ARGUMENT);

                    if (returnValue == StatementExpression.Op.BREAK) {
                        break;
                    }
                }
            }
        }
        return null;
    };
}
