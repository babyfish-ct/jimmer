package org.babyfish.jimmer.sql.kt.filter.impl

import org.babyfish.jimmer.sql.ast.table.Props
import org.babyfish.jimmer.sql.filter.Filter
import org.babyfish.jimmer.sql.kt.filter.KFilter

fun <E: Any> Filter<Props>.toKtFilter(): KFilter<E> =
    if (this is Filter.Parameterized<*>) {
        KtParameterizedFilter(this as Filter.Parameterized<Props>)
    } else {
        KtFilter(this)
    }
