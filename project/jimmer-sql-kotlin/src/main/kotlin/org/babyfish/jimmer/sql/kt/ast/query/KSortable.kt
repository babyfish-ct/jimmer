package org.babyfish.jimmer.sql.kt.ast.query

import org.babyfish.jimmer.sql.ast.query.NullOrderMode
import org.babyfish.jimmer.sql.ast.query.OrderMode
import org.babyfish.jimmer.sql.kt.ast.expression.KExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression

interface KSortable<E> : KFilterable<E> {

    fun orderBy(
        expression: KExpression<E>,
        orderMode: OrderMode = OrderMode.ASC,
        nullOrderMode: NullOrderMode = NullOrderMode.NULLS_FIRST
    )

    fun groupBy(vararg expressions: KExpression<E>)

    fun having(vararg predicates: KNonNullExpression<E>)
}