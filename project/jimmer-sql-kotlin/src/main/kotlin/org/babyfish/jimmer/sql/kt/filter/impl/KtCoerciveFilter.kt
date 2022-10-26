package org.babyfish.jimmer.sql.kt.filter.impl

import org.babyfish.jimmer.sql.ast.table.Props
import org.babyfish.jimmer.sql.filter.CoerciveFilter
import org.babyfish.jimmer.sql.kt.filter.KCoerciveFilter

internal class KtCoerciveFilter<E: Any>(
    javaFilter: CoerciveFilter<Props>
) : KtFilter<E>(javaFilter), KCoerciveFilter<E>