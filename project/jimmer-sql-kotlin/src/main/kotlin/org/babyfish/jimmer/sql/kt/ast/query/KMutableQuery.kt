package org.babyfish.jimmer.sql.kt.ast.query

import org.babyfish.jimmer.kt.DslScope
import org.babyfish.jimmer.sql.kt.ast.expression.KExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression

@DslScope
interface KMutableQuery<E: Any> : KSortable<E> {

    fun groupBy(vararg expressions: KExpression<*>)

    fun having(vararg predicates: KNonNullExpression<Boolean>?)
}