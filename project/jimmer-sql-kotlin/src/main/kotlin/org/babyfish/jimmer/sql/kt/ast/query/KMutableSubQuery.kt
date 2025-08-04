package org.babyfish.jimmer.sql.kt.ast.query

import org.babyfish.jimmer.kt.DslScope
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullTableEx
import org.babyfish.jimmer.sql.kt.ast.table.KPropsLike

@DslScope
interface KMutableSubQuery<PP: KPropsLike, P: KNonNullTableEx<*>> : KMutableQuery<P>, KSubSelectable {

    override val table: P

    val parentTable: PP
}