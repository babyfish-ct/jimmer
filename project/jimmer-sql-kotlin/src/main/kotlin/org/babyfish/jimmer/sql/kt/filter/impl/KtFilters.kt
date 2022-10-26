package org.babyfish.jimmer.sql.kt.filter.impl

import org.babyfish.jimmer.sql.ast.table.Props
import org.babyfish.jimmer.sql.filter.CacheableFilter
import org.babyfish.jimmer.sql.filter.CoerciveFilter
import org.babyfish.jimmer.sql.filter.Filter
import org.babyfish.jimmer.sql.kt.filter.KFilter

fun <E: Any> Filter<Props>.toKtFilter(): KFilter<E> =
    when {
        this is CoerciveFilter<*> && this is CacheableFilter<*> ->
            KtCoerciveCacheableFilter(this as CacheableFilter<Props>)
        this is CoerciveFilter<*> ->
            KtCoerciveFilter(this as CoerciveFilter<Props>)
        this is CacheableFilter<*> ->
            KtCacheableFilter(this as CacheableFilter<Props>)
        else ->
            KtFilter(this)
    }
