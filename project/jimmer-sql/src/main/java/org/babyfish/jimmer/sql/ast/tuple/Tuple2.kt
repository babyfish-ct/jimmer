package org.babyfish.jimmer.sql.ast.tuple

import org.babyfish.jimmer.sql.ast.impl.TupleImplementor
import java.util.function.Function

data class Tuple2<T1, T2>(
    val _1: T1,
    val _2: T2
) : TupleImplementor {

    companion object {

        @JvmStatic
        fun <K, V> toMap(tuples: Collection<Tuple2<K, V>>): Map<K, V> =
            tuples.associateBy({
                it._1
            }) {
                it._2
            }

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

        @JvmStatic
        fun <K, V> toMultiMap(
            tuples: Collection<Tuple2<K, V>>
        ): Map<K, List<V>> =
            tuples.groupBy({
                it._1
            }) {
                it._2
            }

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
