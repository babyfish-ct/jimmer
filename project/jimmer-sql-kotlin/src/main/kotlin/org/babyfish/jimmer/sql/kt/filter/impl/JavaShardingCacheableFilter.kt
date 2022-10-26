package org.babyfish.jimmer.sql.kt.filter.impl

import org.babyfish.jimmer.sql.ast.table.Props
import org.babyfish.jimmer.sql.filter.ShardingCacheableFilter
import org.babyfish.jimmer.sql.kt.filter.KCacheableFilter

internal class JavaShardingCacheableFilter(
    ktFilter: KCacheableFilter<*>
) : JavaCacheableFilter(ktFilter), ShardingCacheableFilter<Props>