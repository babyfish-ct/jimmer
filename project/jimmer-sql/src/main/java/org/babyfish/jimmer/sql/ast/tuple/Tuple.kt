package org.babyfish.jimmer.sql.ast.tuple

import org.babyfish.jimmer.sql.ast.impl.TupleImplementor
import java.util.function.BiFunction

data class Tuple (
    val items: Array<out Any?>
): TupleImplementor {

    companion object {
        @JvmStatic
        fun from(vararg items: Any?): Tuple = Tuple(items)
        @JvmStatic
        fun fromList(items: List<Any>): Tuple = Tuple(items.toTypedArray())
    }

    override fun size(): Int {
        return items.size
    }

    override operator fun get(index: Int): Any? {
        if (index !in items.indices) {
            throw IllegalArgumentException("Index out of range: $index")
        }

        return items[index]
    }

    override fun convert(block: BiFunction<in Any, Int?, in Any>?): TupleImplementor? {
        return Tuple(items.mapIndexed { index, any ->
            block?.apply(any as Any, index) ?: any
        }.toTypedArray())

    }

    fun <T> getAs(index: Int): T? {
        return get(index) as? T
    }

    fun toList(): List<Any?> = items.toList()

}
