package org.babyfish.jimmer.sql.kt.ast.query

import org.babyfish.jimmer.sql.ast.query.selectable.SubSelectable
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullTableEx

interface KMutableSubQuery<E: Any> : KSortable<E>, SubSelectable {

    override val table: KNonNullTableEx<E>
}