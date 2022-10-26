package org.babyfish.jimmer.sql.kt.filter.impl

import org.babyfish.jimmer.sql.ast.table.Props
import org.babyfish.jimmer.sql.filter.CacheableFilter
import org.babyfish.jimmer.sql.kt.filter.KCoerciveCacheableFilter

internal class KtCoerciveCacheableFilter<E: Any>(
    javaFilter: CacheableFilter<Props>
) : KtCacheableFilter<E>(javaFilter), KCoerciveCacheableFilter<E>