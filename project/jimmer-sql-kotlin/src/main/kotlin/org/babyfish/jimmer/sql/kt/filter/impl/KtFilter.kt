package org.babyfish.jimmer.sql.kt.filter.impl

import org.babyfish.jimmer.sql.ast.table.Props
import org.babyfish.jimmer.sql.filter.CacheableFilter
import org.babyfish.jimmer.sql.filter.Filter
import org.babyfish.jimmer.sql.filter.impl.FilterArgsImpl
import org.babyfish.jimmer.sql.kt.filter.KFilter
import org.babyfish.jimmer.sql.kt.filter.KFilterArgs

internal open class KtFilter<E: Any>(
    protected val javaFilter: Filter<Props>
) : KFilter<E> {

    override fun filter(args: KFilterArgs<E>) {
        val javaQuery = (args as KFilterArgsImpl<E>).javaQuery
        val javaArgs = FilterArgsImpl<Props>(
            javaQuery,
            javaQuery.getTable(),
            javaFilter is CacheableFilter<*>
        )
        javaFilter.filter(javaArgs)
    }
}