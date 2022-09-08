package org.babyfish.jimmer.sql.kt.ast.query

import org.babyfish.jimmer.sql.ast.query.Order
import org.babyfish.jimmer.sql.kt.ast.expression.KExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression

interface KSortable<E: Any> : KFilterable<E> {

    fun orderBy(vararg expression: KExpression<*>?)

    fun orderBy(vararg orders: Order?)

    fun groupBy(vararg expressions: KExpression<*>)

    fun having(vararg predicates: KNonNullExpression<Boolean>?)
}