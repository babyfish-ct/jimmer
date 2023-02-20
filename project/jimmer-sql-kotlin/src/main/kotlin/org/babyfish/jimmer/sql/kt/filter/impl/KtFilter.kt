package org.babyfish.jimmer.sql.kt.filter.impl

import org.babyfish.jimmer.meta.ImmutableType
import org.babyfish.jimmer.sql.ast.table.Props
import org.babyfish.jimmer.sql.filter.CacheableFilter
import org.babyfish.jimmer.sql.filter.Filter
import org.babyfish.jimmer.sql.filter.impl.FilterArgsImpl
import org.babyfish.jimmer.sql.filter.impl.FilterManager
import org.babyfish.jimmer.sql.filter.impl.TypeAware
import org.babyfish.jimmer.sql.kt.filter.KFilter
import org.babyfish.jimmer.sql.kt.filter.KFilterArgs

internal open class KtFilter<E: Any>(
    protected val javaFilter: Filter<Props>
) : KFilter<E>, TypeAware {

    override fun filter(args: KFilterArgs<E>) {
        val javaQuery = (args as KFilterArgsImpl<E>).javaQuery
        val javaArgs = FilterArgsImpl<Props>(
            javaQuery,
            javaQuery.getTable(),
            javaFilter is CacheableFilter<*>
        )
        javaFilter.filter(javaArgs)
    }

    override fun getImmutableType(): ImmutableType =
        FilterManager.getImmutableType(javaFilter);

    override fun getFilterType(): Class<*> =
        if (javaFilter is TypeAware) {
            javaFilter.filterType
        } else {
            javaFilter.javaClass
        }

    override fun unwrap(): Any =
        javaFilter

    override fun toString(): String =
        "KtFilter(javaFilter=$javaFilter)"
}