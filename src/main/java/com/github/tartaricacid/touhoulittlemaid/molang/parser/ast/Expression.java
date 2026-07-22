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

/**
 * 表达式接口。它是所有表达式类型的超级接口。
 *
 * <p>表达式是代码的可评估部分，表达式由解析器发出。</p>
 *
 * <p>在Molang中，几乎每个表达式的计算结果都是数值</p>
 *
 * @since 3.0.0
 */
public interface Expression {

    /**
     * 使用给定的访问者访问此表达式。
     *
     * @param visitor 表达访客
     * @param <R> 访问结果返回类型
     * @return 参观结果
     * @since 3.0.0
     */
    <R> R visit(final @NotNull ExpressionVisitor<R> visitor);

}