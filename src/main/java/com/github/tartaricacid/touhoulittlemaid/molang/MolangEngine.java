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

package com.github.tartaricacid.touhoulittlemaid.molang;

import com.github.tartaricacid.touhoulittlemaid.molang.lexer.Cursor;
import com.github.tartaricacid.touhoulittlemaid.molang.parser.ParseException;
import com.github.tartaricacid.touhoulittlemaid.molang.parser.ast.Expression;
import com.github.tartaricacid.touhoulittlemaid.molang.runtime.binding.ObjectBinding;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;

/**
 * 发动机的入门级。提供从字符串和读取器评估和解析 Molang 代码的方法。
 *
 * @since 3.0.0
 */
public interface MolangEngine {

    /**
     * 将给定 {@code reader} 中的数据解析为 {@link Expression} 的 {@link List}
     *
     * <strong>请注意，此方法不会关闭给定的 {@code reader}</strong>
     *
     * @throws ParseException 如果读取失败或出现
     * 脚本中有语法错误吗
     */
    List<Expression> parse(Reader reader) throws IOException;

    /**
     * 将给定的 {@code string} 解析为 {@link Expression} 列表
     *
     * @param string MoLang 字符串
     * @return 已解析表达式列表
     * @throws ParseException 如果解析失败
     */
    default List<Expression> parse(String string) throws ParseException {
        try (Reader reader = new StringReader(string)) {
            return parse(reader);
        } catch (ParseException e) {
            throw e;
        } catch (IOException e) {
            throw new ParseException("Failed to close string reader", e, new Cursor(0, 0));
        }
    }

    static MolangEngine fromCustomBinding(ObjectBinding binding) {
        return new MolangEngineImpl(binding);
    }

    static MolangEngine createEmpty() {
        return new MolangEngineImpl(ObjectBinding.EMPTY);
    }
}
