package org.babyfish.jimmer.sql.kt.filter.impl

import org.babyfish.jimmer.sql.ast.table.Props
import org.babyfish.jimmer.sql.filter.CoerciveFilter
import org.babyfish.jimmer.sql.kt.filter.KCoerciveFilter

internal class JavaCoerciveFilter(
    ktFilter: KCoerciveFilter<*>
) : JavaFilter(ktFilter), CoerciveFilter<Props>