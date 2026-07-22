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

package com.github.tartaricacid.touhoulittlemaid.molang.parser;

import com.github.tartaricacid.touhoulittlemaid.molang.lexer.Cursor;
import com.github.tartaricacid.touhoulittlemaid.molang.lexer.MolangLexer;
import com.github.tartaricacid.touhoulittlemaid.molang.lexer.TokenKind;
import com.github.tartaricacid.touhoulittlemaid.molang.parser.ast.Expression;
import com.github.tartaricacid.touhoulittlemaid.molang.runtime.binding.ObjectBinding;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * Molang 语言解析器。
 *
 * <p>将词元流转换为表达式流。</p>
 *
 * <p>这是一个流式解析器：只有继续调用 {@link #next()}，它才会继续消费词法分析器的输入。</p>
 *
 * @since 3.0.0
 */
public interface MolangParser extends Closeable {

    /**
     * 返回正在使用的内部词法分析器。
     *
     * @return 该解析器的词法分析器。
     * @since 3.0.0
     */
    @NotNull MolangLexer lexer();

    /**
     * 返回当前游标，用于报告解析错误所在的行与列。
     *
     * @return 当前游标
     * @since 3.0.0
     */
    default @NotNull Cursor cursor() {
        //noinspection resource
        return lexer().cursor();
    }

    /**
     * 返回最近一次调用 {@link #next()} 得到的表达式。
     *
     * <p>调用此方法前必须至少成功调用一次 {@link #next()}。</p>
     *
     * @return 最近解析的表达式
     * @throws IllegalStateException 尚未解析任何表达式时抛出
     * @since 3.0.0
     */
    @Nullable Expression current();

    /**
     * 解析下一个表达式。
     *
     * <p>如果到达文件末尾，此方法将返回 {@code null}；如果出现错误，则抛出 {@link ParseException}。</p>
     *
     * @return 解析后的表达式
     * @throws IOException 如果读取或解析失败
     * @since 3.0.0
     */
    @Nullable Expression next() throws IOException;

    /**
     * 解析全部表达式，直到遇到 {@link TokenKind#EOF}。
     *
     * <p>调用完成后输入已耗尽，后续调用 {@link #next()} 将返回 {@code null}。</p>
     *
     * @return 解析得到的全部表达式
     * @throws IOException 如果读取或解析失败
     * @since 3.0.0
     */
    default @NotNull List<Expression> parseAll() throws IOException {
        List<Expression> tokens = new ArrayList<>();
        Expression expr;
        while ((expr = next()) != null) {
            tokens.add(expr);
        }
        return tokens;
    }

    /**
     * 关闭此解析器和内部 {@link MolangLexer}。
     *
     * @throws IOException 如果关闭失败
     * @since 3.0.0
     */
    @Override
    void close() throws IOException;

    /**
     * 创建从指定词法分析器读取词元的解析器。
     *
     * @param lexer 词法分析器
     * @return 创建的解析器
     * @throws IOException 如果解析器初始化失败。
     * @since 3.0.0
     */
    static @NotNull MolangParser parser(final @NotNull MolangLexer lexer, @NotNull ObjectBinding binding) throws IOException {
        return new MolangParserImpl(lexer, binding);
    }

    /**
     * 创建从指定字符流读取内容的解析器。
     *
     * @param reader 读取器
     * @return 创建的解析器
     * @throws IOException 如果解析器初始化失败。
     * @since 3.0.0
     */
    static @NotNull MolangParser parser(final @NotNull Reader reader, @NotNull ObjectBinding binding) throws IOException {
        return parser(MolangLexer.lexer(reader), binding);
    }


    /**
     * 创建读取指定字符串的解析器。
     *
     * @param string 字符串
     * @return 创建的解析器
     * @throws IOException 如果解析器初始化失败。
     * @since 3.0.0
     */
    static @NotNull MolangParser parser(final @NotNull String string, @NotNull ObjectBinding binding) throws IOException {
        return parser(MolangLexer.lexer(string), binding);
    }

    /**
     * 解析指定字符流中的全部表达式。
     *
     * @param reader 输入字符流
     * @return 解析得到的全部表达式
     * @throws IOException 如果读取或解析失败。
     * @since 3.0.0
     */
    static @NotNull List<Expression> parseAll(final @NotNull Reader reader, @NotNull ObjectBinding binding) throws IOException {
        try (MolangParser parser = parser(reader, binding)) {
            return parser.parseAll();
        }
    }

    /**
     * 解析指定字符串。
     *
     * @param string 输入字符串
     * @return 解析得到的全部表达式
     * @throws IOException 如果读取或解析失败。
     * @since 3.0.0
     */
    static @NotNull List<Expression> parseAll(final @NotNull String string, @NotNull ObjectBinding binding) throws IOException {
        try (MolangParser parser = parser(string, binding)) {
            return parser.parseAll();
        }
    }

}
