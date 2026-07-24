package com.github.tartaricacid.touhoulittlemaid.util.functional;

@FunctionalInterface
public interface TriFunction<T, U, V, R> {
    /**
     * 将此函数应用于给定的参数。
     *
     * @param t 第一个输入参数
     * @param u 第二个输入参数
     * @param v 第三个输入参数
     * @return 函数结果
     */
    R apply(T t, U u, V v);

    /**
     * 返回应用此函数的组合 {@code TriFunction}，然后应用 {@code after} 函数。
     *
     * @param after 在此函数之后应用的函数
     * @return 组合的 {@code TriFunction} 应用此函数，然后应用 {@code after} 函数
     */
    default <W> TriFunction<T, U, V, W> andThen(TriFunction<? super R, ? super U, ? super V, ? extends W> after) {
        return (t, u, v) -> after.apply(apply(t, u, v), u, v);
    }
}