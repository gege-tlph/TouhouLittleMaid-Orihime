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

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * 三元条件表达式实现，类似于其他语言中的“if {...} else {...}”表达式。
 *
 * <p>如果 {@code conditional} 表达式计算结果为真值，则 {@code trueExpression} 计算为结果，否则，{@code falseExpression} 为。</p>
 *
 * <p>三元条件表达式示例：{@code true ? 1 : 0}、{@code (age > 18) ? 'adult' : 'minor'}、{@code open ? 'open' : 'closed'}</p>
 *
 * @since 3.0.0
 */
public final class TernaryConditionalExpression implements Expression {

    private final Expression conditional;
    private final Expression trueExpression;
    private final Expression falseExpression;

    public TernaryConditionalExpression(
            final @NotNull Expression conditional,
            final @NotNull Expression trueExpression,
            final @NotNull Expression falseExpression
    ) {
        this.conditional = requireNonNull(conditional, "conditional");
        this.trueExpression = requireNonNull(trueExpression, "trueExpression");
        this.falseExpression = requireNonNull(falseExpression, "falseExpression");
    }

    /**
     * 获取表达式条件。
     *
     * @since 3.0.0
     */
    public @NotNull Expression condition() {
        return conditional;
    }

    /**
     * 获取当条件被评估为真值时应使用的表达式。
     *
     * @since 3.0.0
     */
    public @NotNull Expression trueExpression() {
        return trueExpression;
    }


    /**
     * 获取当条件计算为假值时应使用的表达式。
     *
     * @since 3.0.0
     */
    public @NotNull Expression falseExpression() {
        return falseExpression;
    }

    @Override
    public <R> R visit(final @NotNull ExpressionVisitor<R> visitor) {
        return visitor.visitTernaryConditional(this);
    }

    @Override
    public String toString() {
        return "TernaryCondition(" + conditional + ", "
                + trueExpression + ", "
                + falseExpression + ")";
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TernaryConditionalExpression that = (TernaryConditionalExpression) o;
        return conditional.equals(that.conditional)
                && trueExpression.equals(that.trueExpression)
                && falseExpression.equals(that.falseExpression);
    }

    @Override
    public int hashCode() {
        return Objects.hash(conditional, trueExpression, falseExpression);
    }

}