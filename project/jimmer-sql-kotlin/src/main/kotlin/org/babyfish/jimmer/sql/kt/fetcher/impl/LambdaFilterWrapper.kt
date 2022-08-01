package org.babyfish.jimmer.sql.kt.fetcher.impl

import org.babyfish.jimmer.sql.ast.table.Table
import org.babyfish.jimmer.sql.fetcher.Filter
import org.babyfish.jimmer.sql.fetcher.FilterArgs
import org.babyfish.jimmer.sql.fetcher.impl.FilterArgsImpl
import org.babyfish.jimmer.sql.kt.fetcher.KFilterDsl

internal class LambdaFilterWrapper<E: Any>(
    private val ktFilter: KFilterDsl<E>.() -> Unit
) : Filter<Table<E>> {

    override fun apply(args: FilterArgs<Table<E>>?) {
        val javaQuery = (args as FilterArgsImpl<*>).unwrap()
        ktFilter.apply {
            KFilterDslImpl<E>(javaQuery, args.getKeys()).ktFilter()
        }
    }

    override fun hashCode(): Int =
        ktFilter.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is LambdaFilterWrapper<*>) {
            return false
        }
        return ktFilter == other.ktFilter
    }

    override fun toString(): String =
        ktFilter.toString()
}