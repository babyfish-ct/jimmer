package org.babyfish.jimmer.sql.kt.filter.impl

import org.babyfish.jimmer.sql.kt.filter.KFilter

internal fun KFilter<*>.toJavaFilter(): JavaFilter =
    if (this is KFilter.Parameterized<*>) {
        JavaParameterizedFilter(this)
    } else {
        JavaFilter(this)
    }