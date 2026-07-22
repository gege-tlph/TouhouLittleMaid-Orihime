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

import java.util.*;

/**
 * Molang 词元类型枚举。
 *
 * <p>一个词元通常由一个或多个连续字符组成，例如 {@code ??}、{@code ->}、{@code !}、
 * {@code true} 或 {@code 1.0}。</p>
 *
 * <p>词元本身不执行行为，只负责将输入字符归类后交给解析器。</p>
 *
 * @since 3.0.0
 */
public enum TokenKind {

    /**
     * 文件结束词元。
     */
    EOF,

    /**
     * 词法错误词元。
     */
    ERROR(Tag.HAS_VALUE),

    /**
     * 标识符词元，其值为标识符名称。
     */
    IDENTIFIER(Tag.HAS_VALUE),

    /**
     * 字符串字面量词元，其值为字符串内容。
     */
    STRING(Tag.HAS_VALUE),

    /**
     * 浮点数字面量词元，其字符串值可解析为浮点数。
     */
    FLOAT(Tag.HAS_VALUE),

    /**
     * {@code true} 布尔字面量。
     */
    TRUE,

    /**
     * {@code false} 布尔字面量。
     */
    FALSE,

    /**
     * {@code break} 关键字。
     */
    BREAK,

    /**
     * {@code continue} 关键字。
     */
    CONTINUE,

    /**
     * {@code return} 关键字。
     */
    RETURN,

    /**
     * 点符号 (.)
     */
    DOT,

    /**
     * 感叹号 ({@code !})。
     */
    BANG,

    /**
     * 逻辑与符号 ({@code &&})。
     */
    AMPAMP,

    /**
     * 逻辑或符号 ({@code ||})。
     */
    BARBAR,

    /**
     * 小于标记 (<)
     */
    LT,

    /**
     * 小于或等于标记 (<=)
     */
    LTE,

    /**
     * 大于标记 (>)
     */
    GT,

    /**
     * 大于或等于标记 (>=)
     */
    GTE,

    /**
     * 等号 (=)
     */
    EQ,

    /**
     * 相等标记 (==)
     */
    EQEQ,

    /**
     * 不等于符号 ({@code !=})。
     */
    BANGEQ,

    /**
     * 星号 (*)
     */
    STAR,

    /**
     * 斜线符号 (/)
     */
    SLASH,

    /**
     * 加号 (+)
     */
    PLUS,

    /**
     * 减号 ({@code -})。
     */
    SUB,

    /**
     * 左括号符号“(”
     */
    LPAREN,

    /**
     * 右括号符号“)”
     */
    RPAREN,

    /**
     * 左大括号符号“{”
     */
    LBRACE,

    /**
     * 右大括号符号“}”
     */
    RBRACE,

    /**
     * 空值合并符号 ({@code ??})。
     */
    QUESQUES,

    /**
     * 问号（？）
     */
    QUES,

    /**
     * 冒号符号 (:)
     */
    COLON,

    /**
     * 箭头标记 (->)
     */
    ARROW,

    /**
     * 左括号标记“[”
     */
    LBRACKET,

    /**
     * 右方括号 ({@code ]})。
     */
    RBRACKET,

    /**
     * 逗号符号 (,)
     */
    COMMA,

    /**
     * 分号符号 (;)
     */
    SEMICOLON;

    private final Set<Tag> tags;

    TokenKind(final Tag... tags) {
        this.tags = EnumSet.copyOf(Arrays.asList(tags));
    }

    TokenKind() {
        this.tags = Collections.emptySet();
    }

    /**
     * 判断此词元类型是否具有指定标签。
     *
     * @param tag 要检查的标签。
     * @return 具有指定标签时返回 {@code true}
     * @since 3.0.0
     */
    public boolean hasTag(final @NotNull Tag tag) {
        Objects.requireNonNull(tag, "tag");
        return tags.contains(tag);
    }

    /**
     * 词元类型特征标签。
     *
     * @since 3.0.0
     */
    public enum Tag {

        /**
         * 表示该词元类型携带值，例如浮点数、字符串或标识符词元。
         *
         * @since 3.0.0
         */
        HAS_VALUE

    }

}
