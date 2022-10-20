package org.babyfish.jimmer.sql.kt.filter.impl

import org.babyfish.jimmer.sql.ast.table.Props
import org.babyfish.jimmer.sql.event.EntityEvent
import org.babyfish.jimmer.sql.filter.Filter
import org.babyfish.jimmer.sql.kt.filter.KFilter
import java.util.*

internal class KtCacheableFilter<E: Any>(
    javaFilter: Filter.Parameterized<Props>
) : KtFilter<E>(javaFilter), KFilter.Parameterized<E> {

    override fun getParameters(): SortedMap<String, Any>? =
        (javaFilter as Filter.Parameterized<*>).parameters

    override fun isAffectedBy(e: EntityEvent<*>): Boolean =
        (javaFilter as Filter.Parameterized<*>).isAffectedBy(e)
}