package org.babyfish.jimmer.sql.kt.filter.impl

import org.babyfish.jimmer.sql.kt.filter.KCacheableFilter
import org.babyfish.jimmer.sql.kt.filter.KFilter

internal fun KFilter<*>.toJavaFilter(): JavaFilter =
    if (this is KCacheableFilter<*>) {
        JavaCacheableFilter(this)
    } else {
        JavaFilter(this)
    }