package org.babyfish.jimmer.sql.kt.fetcher.impl

import org.babyfish.jimmer.sql.ast.table.Table
import org.babyfish.jimmer.sql.fetcher.Filter
import org.babyfish.jimmer.sql.fetcher.FilterArgs
import org.babyfish.jimmer.sql.fetcher.impl.FilterArgsImpl
import org.babyfish.jimmer.sql.kt.fetcher.KFilter
import java.util.*

class KtFilterWrapper<E: Any>(
    private val ktFilter: KFilter<E>
): Filter<Table<E>> {

    override fun apply(args: FilterArgs<Table<E>>) {
        val javaQuery = (args as FilterArgsImpl<*>).unwrap()
        ktFilter.apply {
            KFilterDslImpl<E>(javaQuery, args.getKeys()).apply()
        }
    }

    override fun getArgs(): NavigableMap<String, Any> =
        ktFilter.getArgs()

    override fun hashCode(): Int =
        ktFilter.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is KtFilterWrapper<*>) {
            return false
        }
        return ktFilter == other.ktFilter
    }

    override fun toString(): String =
        ktFilter.toString()
}