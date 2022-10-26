package org.babyfish.jimmer.sql.kt.filter.impl

import org.babyfish.jimmer.sql.ast.table.Props
import org.babyfish.jimmer.sql.filter.CoerciveCacheableFilter
import org.babyfish.jimmer.sql.kt.filter.KCacheableFilter

internal class JavaCoerciveCacheableFilter(
    ktFilter: KCacheableFilter<*>
) : JavaCacheableFilter(ktFilter), CoerciveCacheableFilter<Props>