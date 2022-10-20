package org.babyfish.jimmer.sql.kt.filter.impl

import org.babyfish.jimmer.sql.ast.table.Props
import org.babyfish.jimmer.sql.event.EntityEvent
import org.babyfish.jimmer.sql.filter.Filter
import org.babyfish.jimmer.sql.kt.filter.KFilter
import java.util.*

internal class JavaCacheableFilter(
    ktFilter: KFilter.Parameterized<*>
) : JavaFilter(ktFilter), Filter.Parameterized<Props> {

    override fun getParameters(): SortedMap<String, Any>? =
        (ktFilter as KFilter.Parameterized<*>).getParameters()

    override fun isAffectedBy(e: EntityEvent<*>): Boolean =
        (ktFilter as KFilter.Parameterized<*>).isAffectedBy(e)
}