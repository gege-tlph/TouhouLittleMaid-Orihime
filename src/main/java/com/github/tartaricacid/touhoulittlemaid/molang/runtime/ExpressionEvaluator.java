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

package com.github.tartaricacid.touhoulittlemaid.molang.runtime;

import com.github.tartaricacid.touhoulittlemaid.molang.parser.ast.Expression;
import com.github.tartaricacid.touhoulittlemaid.molang.parser.ast.ExpressionVisitor;
import com.github.tartaricacid.touhoulittlemaid.molang.runtime.binding.ObjectBinding;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 用于解释表达式并返回单个结果（通常为双精度浮点数）的 {@link ExpressionVisitor}。
 *
 * @since 3.0.0
 */
@SuppressWarnings("rawtypes")
public interface ExpressionEvaluator<TEntity> extends ExecutionContext<TEntity>, ExpressionVisitor<Object> {
    /**
     * 为指定实体创建新的 {@link ExpressionEvaluator}。
     *
     * @param entity 实体对象
     * @return 新建的表达式求值器
     * @since 3.0.0
     */
    static @NotNull <TEntity> ExpressionEvaluator<TEntity> evaluator(final @Nullable TEntity entity) {
        return new ExpressionEvaluatorImpl<>(entity);
    }

    /**
     * 创建不绑定实体的 {@link ExpressionEvaluator}。
     *
     * @return 新建的表达式求值器
     * @since 3.0.0
     */
    static @NotNull ExpressionEvaluator evaluator() {
        return evaluator(ObjectBinding.EMPTY);
    }

    @Override
    default @Nullable Object eval(final @NotNull Expression expression) {
        return expression.visit(this);
    }

    /**
     * 创建一个新的子表达式求值器。
     *
     * <p>子求值器继承父求值器的全部绑定，也可以添加自己的绑定。</p>
     *
     * <p>子求值器拥有独立的运行栈。</p>
     *
     * @return 子表达式求值器。
     * @since 3.0.0
     */
    @NotNull ExpressionEvaluator<TEntity> createChild();

    /**
     * 创建一个新的子表达式求值器。
     *
     * <p>子求值器继承父求值器的全部绑定，也可以添加自己的绑定。</p>
     *
     * <p>子求值器拥有独立的运行栈。</p>
     *
     * @param entity 子求值器使用的新实体对象
     * @return 子表达式求值器。
     * @since 3.0.0
     */
    @NotNull <TNewEntity> ExpressionEvaluator<TNewEntity> createChild(final @Nullable TNewEntity entity);

    /**
     * 取出最近一次 {@code return} 表达式设置的返回值。
     *
     * @return 返回值；若尚未执行 {@code return} 表达式则为 {@code null}
     * @since 3.0.0
     */
    @Nullable Object popReturnValue();

}
