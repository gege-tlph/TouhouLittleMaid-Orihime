package com.github.tartaricacid.touhoulittlemaid.util.functional;

@FunctionalInterface
public interface TriPredicate<T, U, V> {
    /**
     * 根据给定参数评估此谓词。
     *
     * @param t 第一个输入参数
     * @param u 第二个输入参数
     * @param v 第三个输入参数
     * @return {@code true} 如果输入参数与谓词匹配，
     * 否则 {@code false}
     */
    boolean test(T t, U u, V v);

    /**
     * 返回一个组合的 {@code TriPredicate}，表示此谓词和另一个谓词的短路逻辑 AND。当评估组合谓词时，如果该谓词是 {@code false}，则不评估 {@code other} 谓词。
     *
     * @param other 将与该谓词进行逻辑与运算的谓词
     * @return 一个组合的 {@code TriPredicate} 表示该短路逻辑 AND
     * 谓词和 {@code other} 谓词
     */
    default TriPredicate<T, U, V> and(TriPredicate<? super T, ? super U, ? super V> other) {
        return (t, u, v) -> test(t, u, v) && other.test(t, u, v);
    }
}