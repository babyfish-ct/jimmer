package org.babyfish.jimmer.sql.kt.ast.query

import org.babyfish.jimmer.sql.kt.ast.table.KNonNullTableEx

interface KMutableSubQuery<P: Any, E: Any> : KMutableQuery<E>, KSubSelectable {

    override val table: KNonNullTableEx<E>

    val parentTable: KNonNullTableEx<P>
}