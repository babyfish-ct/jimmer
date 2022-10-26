package org.babyfish.jimmer.sql.kt.filter.impl

import org.babyfish.jimmer.sql.ast.table.Props
import org.babyfish.jimmer.sql.filter.CacheableFilter
import org.babyfish.jimmer.sql.filter.ShardingFilter
import org.babyfish.jimmer.sql.filter.Filter
import org.babyfish.jimmer.sql.kt.filter.KFilter

fun <E: Any> Filter<Props>.toKtFilter(): KFilter<E> =
    when {
        this is ShardingFilter<*> && this is CacheableFilter<*> ->
            KtShardingCacheableFilter(this as CacheableFilter<Props>)
        this is ShardingFilter<*> ->
            KtShardingFilter(this as ShardingFilter<Props>)
        this is CacheableFilter<*> ->
            KtCacheableFilter(this as CacheableFilter<Props>)
        else ->
            KtFilter(this)
    }
