package org.babyfish.jimmer.sql.kt.filter.impl

import org.babyfish.jimmer.sql.kt.filter.KCacheableFilter
import org.babyfish.jimmer.sql.kt.filter.KCoerciveCacheableFilter
import org.babyfish.jimmer.sql.kt.filter.KCoerciveFilter
import org.babyfish.jimmer.sql.kt.filter.KFilter

internal fun KFilter<*>.toJavaFilter(): JavaFilter =
    when {
        this is KCoerciveFilter<*> && this is KCacheableFilter<*> ->
            JavaCoerciveCacheableFilter(this as KCacheableFilter<*>)
        this is KCoerciveFilter<*> ->
            JavaCoerciveFilter(this)
        this is KCacheableFilter<*> ->
            JavaCacheableFilter(this)
        else ->
            JavaFilter(this)
    }