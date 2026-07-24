package com.github.tartaricacid.touhoulittlemaid.util.functional;

@FunctionalInterface
public interface QuadConsumer<T, U, V, W> {
    /**
     * 对给定参数执行此操作。
     *
     * @param t 第一个输入参数
     * @param u 第二个输入参数
     * @param v 第三个输入参数
     * @param w 第四个输入参数
     */
    void accept(T t, U u, V v, W w);

    /**
     * 返回一个组合的 {@code QuadConsumer}，它按顺序执行此操作，然后执行 {@code after} 操作。
     *
     * @param after 该操作之后要执行的操作
     * @return 一个组合的 {@code QuadConsumer} 按顺序执行
     * 操作后执行 {@code after} 操作
     */
    default QuadConsumer<T, U, V, W> andThen(QuadConsumer<? super T, ? super U, ? super V, ? super W> after) {
        return (t, u, v, w) -> {
            accept(t, u, v, w);
            after.accept(t, u, v, w);
        };
    }
}