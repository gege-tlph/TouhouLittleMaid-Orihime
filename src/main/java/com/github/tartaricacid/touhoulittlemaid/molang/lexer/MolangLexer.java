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

package com.github.tartaricacid.touhoulittlemaid.molang.lexer;

import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Molang 语言词法分析器。
 *
 * <p>将字符流转换为词元流。</p>
 *
 * <p>这是一个流式词法分析器：只有继续调用 {@link #next()}，它才会继续读取输入。</p>
 *
 * <p>基本用法：</p>
 * <pre>{@code
 *     MolangLexer lexer = MolangLexer.lexer(new StringReader("1 + 1"));
 *     List<Token> tokens = new ArrayList<>();
 *     Token token;
 *     while ((token = lexer.next()).kind() != TokenKind.EOF) {
 *         tokens.add(token);
 *     }
 *     // tokens: [ Double, Plus, Double ]
 * }</pre>
 *
 * <p>也可以使用便捷方法一次读取全部词元：</p>
 * <pre>{@code
 *      List<Token> tokens = MolangLexer.tokenizeAll("1 + 1");
 *      // tokens: [ Double, Plus, Double ]
 * }</pre>
 *
 * @since 3.0.0
 */
public interface MolangLexer extends Closeable {

    /**
     * 返回当前游标。游标记录行号与列号，用于定位词法错误。
     *
     * @return 词法分析器游标
     * @since 3.0.0
     */
    @NotNull Cursor cursor();

    /**
     * 返回最近一次调用 {@link #next()} 得到的词元。
     *
     * <p>调用此方法前必须至少成功调用一次 {@link #next()}。</p>
     *
     * @return 最近读取的词元
     * @throws IllegalStateException 尚未读取任何词元时抛出
     * @since 3.0.0
     */
    @NotNull Token current();

    /**
     * 从输入流中读取并返回下一个词元。
     *
     * <p>返回值不会为 {@code null}，但可能是 {@link TokenKind#EOF} 或 {@link TokenKind#ERROR}。</p>
     *
     * <p>首次读到 {@link TokenKind#EOF} 后即可停止；后续调用仍会返回 EOF 词元。</p>
     *
     * @return 下一个词元
     * @throws IOException 如果读取失败
     * @since 3.0.0
     */
    @NotNull Token next() throws IOException;

    /**
     * 读取全部词元，直到遇到 {@link TokenKind#EOF}。
     *
     * <p>调用完成后输入流已读至末尾，后续读取均应得到 EOF。</p>
     *
     * @return 读取到的全部词元，不包含 EOF
     * @throws IOException 如果读取失败
     * @since 3.0.0
     */
    default @NotNull List<Token> tokenizeAll() throws IOException {
        List<Token> tokens = new ArrayList<>();
        Token token;
        while ((token = next()).kind() != TokenKind.EOF) {
            tokens.add(token);
        }
        return tokens;
    }

    /**
     * 关闭此词法分析器和内部 {@link Reader}。
     *
     * @throws IOException 如果关闭失败
     * @since 3.0.0
     */
    @Override
    void close() throws IOException;

    /**
     * 创建从指定字符流读取内容的词法分析器。
     *
     * @param reader 输入字符流
     * @return 创建的词法分析器
     * @throws IOException 如果词法分析器初始化失败。
     * @since 3.0.0
     */
    static @NotNull MolangLexer lexer(final @NotNull Reader reader) throws IOException {
        return new MolangLexerImpl(reader);
    }

    /**
     * 创建读取指定字符串的词法分析器。
     *
     * @param string 要进行词法分析的字符串
     * @return 创建的词法分析器
     * @throws IOException 如果词法分析器初始化失败。
     * @since 3.0.0
     */
    static @NotNull MolangLexer lexer(final @NotNull String string) throws IOException {
        return lexer(new StringReader(string));
    }

    /**
     * 对指定字符流中的全部内容进行词法分析。
     *
     * @param reader 输入字符流
     * @return 读取到的全部词元
     * @throws IOException 如果读取失败。
     * @since 3.0.0
     */
    static @NotNull List<Token> tokenizeAll(final @NotNull Reader reader) throws IOException {
        try (MolangLexer lexer = lexer(reader)) {
            return lexer.tokenizeAll();
        }
    }

    /**
     * 对指定字符串进行词法分析。
     *
     * @param string 输入字符串
     * @return 读取到的全部词元
     * @throws IOException 如果读取失败。
     * @since 3.0.0
     */
    static @NotNull List<Token> tokenizeAll(final @NotNull String string) throws IOException {
        try (MolangLexer lexer = lexer(string)) {
            return lexer.tokenizeAll();
        }
    }

}
