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

/**
 * 语句表达式的实现。语句表达式没有子表达式，它们只有一个操作类型。
 *
 * <p>语句表达式示例：{@code break}、{@code continue}</p>
 *
 * @since 3.0.0
 */
public final class StatementExpression implements Expression {

    private final Op op;

    public StatementExpression(final @NotNull Op op) {
        this.op = Objects.requireNonNull(op, "op");
    }

    /**
     * 获取该语句的操作/类型。
     *
     * @return 语句操作/类型。
     * @since 3.0.0
     */
    public @NotNull Op op() {
        return op;
    }

    @Override
    public <R> R visit(final @NotNull ExpressionVisitor<R> visitor) {
        return visitor.visitStatement(this);
    }


    /**
     * 包含所有可能的操作/语句表达式类型的枚举。
     *
     * @since 3.0.0
     */
    public enum Op {
        /**
         * Break语句类型
         *
         * @since 3.0.0
         */
        BREAK,

        /**
         * continue 语句类型
         *
         * @since 3.0.0
         */
        CONTINUE
    }

}
