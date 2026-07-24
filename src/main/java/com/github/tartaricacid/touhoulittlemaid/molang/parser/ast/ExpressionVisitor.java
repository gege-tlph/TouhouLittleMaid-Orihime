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

package com.github.tartaricacid.touhoulittlemaid.molang.parser.ast;

import com.github.tartaricacid.touhoulittlemaid.molang.runtime.Function;
import org.jetbrains.annotations.NotNull;

/**
 * {@link Expression} 访问器，用于在不修改表达式类型的情况下实现求值、格式化等操作。
 *
 * <p>基本用法：</p>
 * <pre>{@code
 *      Expression expr = ...;
 *      String str = expr.visit(new ToStringVisitor());
 * }</pre>
 *
 * <p>调用方应使用 {@link Expression#visit(ExpressionVisitor)} 进行双分派，
 * 不要直接调用 {@link #visit(Expression)}，否则具体表达式类型的重载不会生效。</p>
 *
 * @param <R> 访问结果类型
 * @since 3.0.0
 */
public interface ExpressionVisitor<R> {

    /**
     * 访问未提供专用重载的表达式。
     *
     * @param expression 要访问的表达式
     * @return 访问结果
     * @since 3.0.0
     */
    R visit(final @NotNull Expression expression);

    /**
     * 访问双精度数值表达式。
     *
     * @param expression 要访问的表达式
     * @return 访问结果
     * @since 3.0.0
     */
    default R visitDouble(final @NotNull DoubleExpression expression) {
        return visit(expression);
    }

    /**
     * 评估字符串表达式。
     *
     * @param expression 要访问的表达式
     * @return 访问结果
     * @since 3.0.0
     */
    default R visitString(final @NotNull StringExpression expression) {
        return visit(expression);
    }

    /**
     * 评估标识符表达式。
     *
     * @param expression 要访问的表达式
     * @return 访问结果
     * @since 3.0.0
     */
    default R visitIdentifier(final @NotNull IdentifierExpression expression) {
        return visit(expression);
    }

    default R visitVariable(final @NotNull VariableExpression expression) {
        return visit(expression);
    }

    default R visitAssignableVariable(final @NotNull AssignableVariableExpression expression) {
        return visit(expression);
    }

    default R visitStruct(final @NotNull StructAccessExpression expression) {
        return visit(expression);
    }

    /**
     * 评估三元条件表达式。
     *
     * @param expression 要访问的表达式
     * @return 访问结果
     * @since 3.0.0
     */
    default R visitTernaryConditional(final @NotNull TernaryConditionalExpression expression) {
        return visit(expression);
    }

    /**
     * 评估一元表达式。
     *
     * @param expression 要访问的表达式
     * @return 访问结果
     * @since 3.0.0
     */
    default R visitUnary(final @NotNull UnaryExpression expression) {
        return visit(expression);
    }

    /**
     * 评估执行范围表达式。
     *
     * @param expression 要访问的表达式
     * @return 访问结果
     * @since 3.0.0
     */
    default R visitExecutionScope(final @NotNull ExecutionScopeExpression expression) {
        return visit(expression);
    }

    default Function buildExecutionScopeFunction(final @NotNull ExecutionScopeExpression expression) {
        throw new UnsupportedOperationException("Unsupported expression type: " + expression);
    }

    /**
     * 评估二进制表达式。
     *
     * @param expression 要访问的表达式
     * @return 访问结果
     * @since 3.0.0
     */
    default R visitBinary(final @NotNull BinaryExpression expression) {
        return visit(expression);
    }

    /**
     * 评估调用表达式。
     *
     * @param expression 要访问的表达式
     * @return 访问结果
     * @since 3.0.0
     */
    default R visitCall(final @NotNull CallExpression expression) {
        return visit(expression);
    }

    /**
     * 评估语句表达式。
     *
     * @param expression 的表达。
     * @return 结果。
     * @since 3.0.0
     */
    default R visitStatement(final @NotNull StatementExpression expression) {
        return visit(expression);
    }

}
