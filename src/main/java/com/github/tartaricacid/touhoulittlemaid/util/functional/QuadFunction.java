package com.github.tartaricacid.touhoulittlemaid.util.functional;

@FunctionalInterface
public interface QuadFunction<T, U, V, W, R> {
    /**
     * 将此函数应用于给定的参数。
     *
     * @param t 第一个输入参数
     * @param u 第二个输入参数
     * @param v 第三个输入参数
     * @param w 第四个输入参数
     * @return 函数结果
     */
    R apply(T t, U u, V v, W w);

    /**
     * 返回应用此函数的组合 {@code QuadFunction}，然后应用 {@code after} 函数。
     *
     * @param after 在此函数之后应用的函数
     * @return 组合的 {@code QuadFunction} 应用此函数，然后应用 {@code after} 函数
     */
    default <X> QuadFunction<T, U, V, W, X> andThen(QuadFunction<? super R, ? super U, ? super V, ? super W, ? extends X> after) {
        return (t, u, v, w) -> after.apply(apply(t, u, v, w), u, v, w);
    }
}