package org.babyfish.jimmer.sql.kt.filter.impl

import org.babyfish.jimmer.meta.ImmutableType
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor
import org.babyfish.jimmer.sql.ast.table.Props
import org.babyfish.jimmer.sql.filter.CacheableFilter
import org.babyfish.jimmer.sql.filter.Filter
import org.babyfish.jimmer.sql.filter.impl.FilterArgsImpl
import org.babyfish.jimmer.sql.filter.impl.FilterManager
import org.babyfish.jimmer.sql.filter.impl.FilterWrapper
import org.babyfish.jimmer.sql.kt.filter.KFilter
import org.babyfish.jimmer.sql.kt.filter.KFilterArgs

internal open class KtFilter<E: Any>(
    protected val javaFilter: Filter<Props>
) : KFilter<E>, FilterWrapper {

    override fun filter(args: KFilterArgs<E>) {
        val javaQuery = (args as KFilterArgsImpl<E>).javaStatement
        val javaArgs = FilterArgsImpl<Props>(
            javaQuery.tableLikeImplementor,
            javaQuery.getTable(),
            javaFilter is CacheableFilter<*>
        )
        javaFilter.filter(javaArgs)
    }

    override fun getImmutableType(): ImmutableType =
        FilterManager.getImmutableType(javaFilter);

    override fun getFilterType(): Class<*> =
        if (javaFilter is FilterWrapper) {
            javaFilter.filterType
        } else {
            javaFilter.javaClass
        }

    override fun unwrap(): Any =
        javaFilter

    override fun hashCode(): Int =
        FilterWrapper.unwrap(javaFilter).hashCode()

    override fun equals(other: Any?): Boolean =
        FilterWrapper.unwrap(javaFilter) == FilterWrapper.unwrap(other)

    override fun toString(): String =
        "KtFilter(javaFilter=$javaFilter)"
}