package org.babyfish.jimmer.sql.kt.fetcher.impl

import org.babyfish.jimmer.sql.ast.table.Table
import org.babyfish.jimmer.sql.fetcher.FieldFilter
import org.babyfish.jimmer.sql.fetcher.FieldFilterArgs
import org.babyfish.jimmer.sql.fetcher.impl.FieldFilterArgsImpl
import org.babyfish.jimmer.sql.kt.fetcher.KFilterDsl

internal class LambdaFieldFilterWrapper<E: Any>(
    private val ktFilter: KFilterDsl<E>.() -> Unit
) : FieldFilter<Table<E>> {

    override fun apply(args: FieldFilterArgs<Table<E>>?) {
        val javaQuery = (args as FieldFilterArgsImpl<*>).unwrap()
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
        if (other !is LambdaFieldFilterWrapper<*>) {
            return false
        }
        return ktFilter == other.ktFilter
    }

    override fun toString(): String =
        ktFilter.toString()
}