package org.babyfish.jimmer.sql.ast.tuple

import org.babyfish.jimmer.sql.ast.impl.TupleImplementor
import java.util.function.BiFunction
import java.util.function.Function

data class Tuple2<T1, T2>(
    val _1: T1,
    val _2: T2
) : TupleImplementor {

    override fun size(): Int = 2

    override operator fun get(index: Int): Any? =
        when (index) {
            0 -> _1
            1 -> _2
            else -> throw IllegalArgumentException("Index must between 0 and ${size() - 1}")
        }

    override fun convert(block: BiFunction<Any?, Int, Any?>): TupleImplementor =
        Tuple2(
            block.apply(_1, 0),
            block.apply(_2, 1),
        )

    companion object {

        /**
         * Shortcut method only for java, java8 stream API is too complex
         */
        @JvmStatic
        fun <K, V> toMap(tuples: Collection<Tuple2<K, V>>): Map<K, V> =
            tuples.associateBy({
                it._1
            }) {
                it._2
            }

        /**
         * Shortcut method only for java, java8 stream API is too complex
         */
        @JvmStatic
        fun <K, T, V> toMap(
            tuples: Collection<Tuple2<K, T>>,
            valueMapper: Function<T, V>
        ): Map<K, V> =
            tuples.associateBy({
                it._1
            }) {
                valueMapper.apply(it._2)
            }

        /**
         * Shortcut method only for java, java8 stream API is too complex
         */
        @JvmStatic
        fun <K, V> toMultiMap(
            tuples: Collection<Tuple2<K, V>>
        ): Map<K, List<V>> =
            tuples.groupBy({
                it._1
            }) {
                it._2
            }

        /**
         * Shortcut method only for java, java8 stream API is too complex
         */
        @JvmStatic
        fun <K, T, V> toMultiMap(
            tuples: Collection<Tuple2<K, T>>,
            valueMapper: Function<T, V>
        ): Map<K, List<V>> =
            tuples.groupBy({
                it._1
            }) {
                valueMapper.apply(it._2)
            }
    }
}
