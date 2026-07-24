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

import java.io.IOException;
import java.io.Reader;

import static java.util.Objects.requireNonNull;

final class MolangLexerImpl implements MolangLexer {

    // 源读取器
    private final Reader reader;

    // 当前索引
    private final Cursor cursor = new Cursor();

    // 下一个要检查的字符
    private int next;

    // 当前令牌
    private Token lastToken = null;
    private Token token = null;

    MolangLexerImpl(final @NotNull Reader reader) throws IOException {
        this.reader = requireNonNull(reader, "reader");
        this.next = reader.read();
    }

    @Override
    public @NotNull Cursor cursor() {
        return cursor;
    }

    @Override
    public @NotNull Token current() {
        if (token == null) {
            throw new IllegalStateException("No current token, please call next() at least once");
        }
        return token;
    }

    @Override
    public @NotNull Token next() throws IOException {
        lastToken = token;
        return token = next0();
    }

    @Override
    public void close() throws IOException {
        this.reader.close();
    }

    private @NotNull Token next0() throws IOException {
        int c = next;
        if (c == -1) {
            // 达到 EOF
            return new Token(TokenKind.EOF, null, cursor.index(), cursor.index() + 1);
        }


        while (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
            c = read();
        }

        // 末尾是否有额外的空格、行等？
        if (c == -1) {
            // 达到 EOF
            return new Token(TokenKind.EOF, null, cursor.index(), cursor.index() + 1);
        }

        int start = cursor.index();
        if (c == '.' && lastToken != null && lastToken.kind() == TokenKind.RPAREN) {
            read();
            return new Token(TokenKind.DOT, null, start, cursor.index());
        }
        boolean isLastIdentifier = (lastToken != null && lastToken.kind() == TokenKind.IDENTIFIER);
        if (Characters.isDigit(c) || (!isLastIdentifier && c == '.')) {
            StringBuilder builder = new StringBuilder(8);
            if (!isLastIdentifier) {
                builder.appendCodePoint(c);

                // 第一个字符是数字，继续阅读数字
                while (Characters.isDigit(c = read())) {
                    builder.appendCodePoint(c);
                }
            } else {
                builder.append('0');
            }

            if (c == '.') {
                builder.append('.');
                while (Characters.isDigit(c = read())) {
                    builder.appendCodePoint(c);
                }
            }

            return new Token(TokenKind.FLOAT, builder.toString(), start, cursor.index());
        } else if (Characters.isValidForWordStart(c)) {
            // 可以是标识符或关键字
            StringBuilder builder = new StringBuilder();
            do {
                builder.appendCodePoint(c);
            } while (Characters.isValidForWordContinuation(c = read()));
            String word = builder.toString().toLowerCase();
            TokenKind kind;
            switch (word) {
                // @格式化程序：关闭
                case "break": kind = TokenKind.BREAK; break;
                case "continue": kind = TokenKind.CONTINUE; break;
                case "return": kind = TokenKind.RETURN; break;
                case "true": kind = TokenKind.TRUE; break;
                case "false": kind = TokenKind.FALSE; break;
                default: kind = TokenKind.IDENTIFIER; break;
                // @格式化程序：打开
            }

            return new Token(
                    kind,
                    // 关键字没有值
                    kind == TokenKind.IDENTIFIER ? word : null,
                    start,
                    cursor.index()
            );
        } else if (c == '\'') { // 单引号表示字符串开始
            StringBuilder value = new StringBuilder(16);
            while (true) {
                c = read();
                if (c == -1) {
                    // 到底是什么？你没有关闭字符串
                    return new Token(TokenKind.ERROR, "Found end-of-file before closing quote", start, cursor.index());
                } else if (c == '\'') {
                    // 字符串已关闭！
                    break;
                } else {

                    value.appendCodePoint(c);
                }
            }
            // 这里，“c”应该是一个引号，所以跳过它并把它交给下一个人
            read();
            return new Token(TokenKind.STRING, value.toString(), start, cursor.index());
        } else {
            // 这里我们确信“c”是 NOT: - EOF - 单引号 (') - A-Za-z_ - 0-9 所以它必须是像 ?, *, +, - 这样的符号
            TokenKind tokenKind;
            String value = null;
            int c1 = -2; // 仅当“c”可能有延续时才设置，例如“==”、“!=”、“??”
            switch (c) {
                case '!': {
                    c1 = read();
                    if (c1 == '=') {
                        read();
                        tokenKind = TokenKind.BANGEQ;
                    } else {
                        tokenKind = TokenKind.BANG;
                    }
                    break;
                }
                case '&': {
                    c1 = read();
                    if (c1 == '&') {
                        read();
                        tokenKind = TokenKind.AMPAMP;
                    } else {
                        tokenKind = TokenKind.ERROR;
                        value = "Unexpected token '" + ((char) c1) + "', expected '&' (Molang doesn't support bitwise operators)";
                    }
                    break;
                }
                case '|': {
                    c1 = read();
                    if (c1 == '|') {
                        read();
                        tokenKind = TokenKind.BARBAR;
                    } else {
                        tokenKind = TokenKind.ERROR;
                        value = "Unexpected token '" + ((char) c1) + "', expected '|' (Molang doesn't support bitwise operators)";
                    }
                    break;
                }
                case '<': {
                    c1 = read();
                    if (c1 == '=') {
                        read();
                        tokenKind = TokenKind.LTE;
                    } else {
                        tokenKind = TokenKind.LT;
                    }
                    break;
                }
                case '>': {
                    c1 = read();
                    if (c1 == '=') {
                        read();
                        tokenKind = TokenKind.GTE;
                    } else {
                        tokenKind = TokenKind.GT;
                    }
                    break;
                }
                case '=': {
                    c1 = read();
                    if (c1 == '=') {
                        read();
                        tokenKind = TokenKind.EQEQ;
                    } else {
                        tokenKind = TokenKind.EQ;
                    }
                    break;
                }
                case '-': {
                    c1 = read();
                    if (c1 == '>') {
                        read();
                        tokenKind = TokenKind.ARROW;
                    } else {
                        tokenKind = TokenKind.SUB;
                    }
                    break;
                }
                case '?': {
                    c1 = read();
                    if (c1 == '?') {
                        read();
                        tokenKind = TokenKind.QUESQUES;
                    } else {
                        tokenKind = TokenKind.QUES;
                    }
                    break;
                }
                // @格式化程序：关闭
                case '/': tokenKind = TokenKind.SLASH; break;
                case '*': tokenKind = TokenKind.STAR; break;
                case '+': tokenKind = TokenKind.PLUS; break;
                case ',': tokenKind = TokenKind.COMMA; break;
                case '.': tokenKind = TokenKind.DOT; break;
                case '(': tokenKind = TokenKind.LPAREN; break;
                case ')': tokenKind = TokenKind.RPAREN; break;
                case '{': tokenKind = TokenKind.LBRACE; break;
                case '}': tokenKind = TokenKind.RBRACE; break;
                case ':': tokenKind = TokenKind.COLON; break;
                case '[': tokenKind = TokenKind.LBRACKET; break;
                case ']': tokenKind = TokenKind.RBRACKET; break;
                case ';': tokenKind = TokenKind.SEMICOLON; break;
                // @格式化程序：打开
                default: {
                    // “c”是我们不知道的东西！
                    tokenKind = TokenKind.ERROR;
                    value = "Unexpected token '" + ((char) c) + "': invalid token";
                    break;
                }
            }

            if (c1 == -2) {

                read();
            }

            return new Token(tokenKind, value, start, cursor.index());
        }
    }

    private int read() throws IOException {
        int c = reader.read();
        cursor.push(c);
        next = c;
        return c;
    }

}
