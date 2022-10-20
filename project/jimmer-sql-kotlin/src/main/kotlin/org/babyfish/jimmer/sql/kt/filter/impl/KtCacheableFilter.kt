package org.babyfish.jimmer.sql.kt.filter.impl

import org.babyfish.jimmer.sql.ast.table.Props
import org.babyfish.jimmer.sql.event.EntityEvent
import org.babyfish.jimmer.sql.filter.CacheableFilter
import org.babyfish.jimmer.sql.kt.filter.KCacheableFilter
import java.util.*

internal class KtCacheableFilter<E: Any>(
    javaFilter: CacheableFilter<Props>
) : KtFilter<E>(javaFilter), KCacheableFilter<E> {

    override fun getParameters(): SortedMap<String, Any>? =
        (javaFilter as CacheableFilter<*>).parameters

    override fun isAffectedBy(e: EntityEvent<*>): Boolean =
        (javaFilter as CacheableFilter<*>).isAffectedBy(e)
}