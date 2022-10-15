package org.babyfish.jimmer.sql.kt.fetcher.impl

import org.babyfish.jimmer.sql.ast.table.Table
import org.babyfish.jimmer.sql.fetcher.FieldFilter
import org.babyfish.jimmer.sql.fetcher.FieldFilterArgs
import org.babyfish.jimmer.sql.fetcher.impl.FieldFilterArgsImpl
import org.babyfish.jimmer.sql.kt.fetcher.KFieldFilterDsl

internal class JavaFieldFilter<E: Any>(
    private val ktFilter: KFieldFilterDsl<E>.() -> Unit
) : FieldFilter<Table<E>> {

    override fun apply(args: FieldFilterArgs<Table<E>>) {
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
        if (other !is JavaFieldFilter<*>) {
            return false
        }
        return ktFilter == other.ktFilter
    }

    override fun toString(): String =
        ktFilter.toString()
}