package org.babyfish.jimmer.sql.kt.fetcher.impl

import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor
import org.babyfish.jimmer.sql.ast.table.Table
import org.babyfish.jimmer.sql.fetcher.FieldFilter
import org.babyfish.jimmer.sql.fetcher.FieldFilterArgs
import org.babyfish.jimmer.sql.fetcher.spi.FieldFilterArgsImplementor
import org.babyfish.jimmer.sql.kt.fetcher.KFieldFilter

internal class JavaFieldFilter2<E: Any>(
    private val ktFilter: KFieldFilter<E>
) : FieldFilter<Table<E>> {

    override fun apply(args: FieldFilterArgs<Table<E>>) {
        val javaQuery = (args as FieldFilterArgsImplementor<*>).query()
        ktFilter.apply {
            KFilterDslImpl(
                javaQuery,
                args.getTable() as TableImplementor<E>,
                args.getKeys()
            ).applyTo()
        }
    }

    override fun hashCode(): Int =
        ktFilter.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is JavaFieldFilter2<*>) {
            return false
        }
        return ktFilter == other.ktFilter
    }

    override fun toString(): String =
        ktFilter.toString()
}