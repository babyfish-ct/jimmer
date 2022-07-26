package org.babyfish.jimmer.sql.kt.ast.mutation

import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullPropExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNullableExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNullablePropExpression
import org.babyfish.jimmer.sql.kt.ast.query.KFilterable
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullTableEx

interface KMutableUpdate<E: Any> : KFilterable<E> {

    override val table: KNonNullTableEx<E>

    fun <X: Any> set(
        path: KNonNullPropExpression<X>,
        value: KNonNullExpression<X>
    )

    fun <X: Any> set(
        path: KNonNullPropExpression<X>,
        value: X
    )

    fun <X: Any> set(
        path: KNullablePropExpression<X>,
        value: KNullableExpression<X>
    )

    fun <X: Any> set(
        path: KNullableExpression<X>,
        value: X?
    )
}