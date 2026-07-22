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

import java.util.Objects;

/**
 * 执行词法分析时跟踪字符位置的可变类
 *
 * <p>可用于以人类可读的方式显示词汇错误的位置</p>
 *
 * @since 3.0.0
 */
public final class Cursor implements Cloneable {

    private int index = 0;
    private int line = 0;
    private int column = 0;

    public Cursor(final int line, final int column) {
        this.line = line;
        this.column = column;
    }

    public Cursor() {
    }

    public int index() {
        return index;
    }

    public int line() {
        return line;
    }

    public int column() {
        return column;
    }

    public void push(final int character) {
        index++;
        if (character == '\n') {

            line++;
            column = 1;
        } else {
            column++;
        }
    }

    @Override
    public @NotNull Cursor clone() {
        return new Cursor(line, column);
    }

    @Override
    public String toString() {
        return "line " + line + ", column " + column;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Cursor that = (Cursor) o;
        return line == that.line && column == that.column;
    }

    @Override
    public int hashCode() {
        return Objects.hash(line, column);
    }

}
