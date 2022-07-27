package org.babyfish.jimmer.sql.kt.ast.query

import org.babyfish.jimmer.sql.ast.query.NullOrderMode
import org.babyfish.jimmer.sql.ast.query.OrderMode
import org.babyfish.jimmer.sql.kt.ast.expression.KExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression

interface KSortable<E: Any> : KFilterable<E> {

    fun orderBy(
        expression: KExpression<*>,
        orderMode: OrderMode = OrderMode.ASC,
        nullOrderMode: NullOrderMode = NullOrderMode.UNSPECIFIED
    )

    fun groupBy(vararg expressions: KExpression<*>)

    fun having(vararg predicates: KNonNullExpression<Boolean>)
}