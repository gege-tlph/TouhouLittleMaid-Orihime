package com.github.tartaricacid.touhoulittlemaid.util.functional;

@FunctionalInterface
public interface TriConsumer<T, U, V> {
    /**
     * 对给定参数执行此操作。
     *
     * @param t 第一个输入参数
     * @param u 第二个输入参数
     * @param v 第三个输入参数
     */
    void accept(T t, U u, V v);

    /**
     * 返回一个组合的 {@code TriConsumer}，它按顺序执行此操作，然后执行 {@code after} 操作。
     *
     * @param after 该操作之后要执行的操作
     * @return 一个组合的 {@code TriConsumer} 按顺序执行
     * 操作后跟 {@code after} 操作
     */
    default TriConsumer<T, U, V> andThen(TriConsumer<? super T, ? super U, ? super V> after) {
        return (t, u, v) -> {
            accept(t, u, v);
            after.accept(t, u, v);
        };
    }
}