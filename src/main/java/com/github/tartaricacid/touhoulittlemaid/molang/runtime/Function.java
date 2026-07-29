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

import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.molang.context.IContext;
import com.github.tartaricacid.touhoulittlemaid.molang.parser.ast.Expression;
import com.github.tartaricacid.touhoulittlemaid.molang.runtime.binding.ValueConversions;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Molang function. Receives a certain amount of
 * parameters and (optionally) returns a value. Can be called
 * from Molang code using call expressions: {@code my_function(1, 2, 3)}
 *
 * <p>This is a very low-level function that is "expression-sensitive",
 * this means, it takes the raw expression arguments instead of the
 * evaluated expression argument values.</p>
 *
 * @since 3.0.0
 */
@FunctionalInterface
public interface Function {
    /**
     * Executes this function with the given arguments.
     *
     * @param context   The execution context
     * @param arguments The arguments
     * @return The function result
     * @since 3.0.0
     */
    @Nullable Object evaluate(final @NotNull ExecutionContext<?> context, final @NotNull ArgumentCollection arguments);

    default boolean validateArgumentSize(int size) {
        return true;
    }

    ArgumentCollection EMPTY_ARGUMENT = new ArgumentCollection(new ArrayList<>());

    class ArgumentCollection {
        private final List<Expression> arguments;

        public ArgumentCollection(List<Expression> arguments) {
            this.arguments = arguments;
        }

        public int size() {
            return arguments.size();
        }

        public String getAsString(@NotNull ExecutionContext<?> ctx, final int index) {
            return ValueConversions.asString(ctx.eval(arguments.get(index)));
        }

        public int getAsPooledString(@NotNull ExecutionContext<?> ctx, final int index) {
            return ValueConversions.asPooledString(ctx.eval(arguments.get(index)));
        }

        public double getAsDouble(@NotNull ExecutionContext<?> ctx, final int index) {
            return ValueConversions.asDouble(ctx.eval(arguments.get(index)));
        }

        public int getAsInt(@NotNull ExecutionContext<?> ctx, final int index) {
            return ValueConversions.asInt(ctx.eval(arguments.get(index)));
        }

        public float getAsFloat(@NotNull ExecutionContext<?> ctx, final int index) {
            return ValueConversions.asFloat(ctx.eval(arguments.get(index)));
        }

        public boolean getAsBoolean(@NotNull ExecutionContext<?> ctx, final int index) {
            return ValueConversions.asBoolean(ctx.eval(arguments.get(index)));
        }

        /**
         * 解析参数为 {@link Identifier}，失败时返回 null 并打印诊断信息。
         *
         * <p>行为等价于 1.21.1 的 {@code MolangUtils.parseResourceLocation(ctx.entity(), getAsString(ctx, index))}
         * （该方法原本就只是 {@code tryParse}，其 context 参数未被使用），额外加上 26.1 的 debugPrint 诊断。</p>
         *
         * <p><b>与 26.1 的有意差异</b>：26.1 在此处缓存解析结果到 {@code StringExpression.cachedValue}，
         * 其前提是 26.1 把 {@code ExpressionEvaluatorImpl.visitString} 改为返回 AST 节点本身；
         * 本仓库的 {@code visitString} 返回 {@code expression.value()}（String），
         * 故该缓存分支在此永远不成立。移植缓存需连带迁移求值器语义（波及所有字符串消费方），
         * 属优化而非 1.21.11 强制的 API 变更，故不引入。</p>
         */
        @Nullable
        public Identifier getAsResourceLocation(@NotNull ExecutionContext<? extends IContext<?>> ctx, final int index) {
            String value = getAsString(ctx, index);
            Identifier id = (value == null) ? null : Identifier.tryParse(value);
            if (id == null) {
                ctx.entity().debugPrint("Illegal resource location: %s", value);
            }
            return id;
        }

        public Object getValue(@NotNull ExecutionContext<?> ctx, final int index) {
            return ctx.eval(arguments.get(index));
        }

        public Expression getExpression(final int index) {
            return arguments.get(index);
        }
    }
}
