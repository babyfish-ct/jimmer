package org.babyfish.jimmer.sql.kt.ast.query

import org.babyfish.jimmer.kt.DslScope
import org.babyfish.jimmer.sql.kt.ast.expression.KExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.table.KPropsLike

@DslScope
interface KMutableQuery<P: KPropsLike> : KSortable<P> {

    fun groupBy(vararg expressions: KExpression<*>)

    fun having(vararg predicates: KNonNullExpression<Boolean>?)
}