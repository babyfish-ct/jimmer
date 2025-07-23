package org.babyfish.jimmer.sql.kt.ast.mutation

import org.babyfish.jimmer.kt.DslScope
import org.babyfish.jimmer.sql.kt.ast.expression.*
import org.babyfish.jimmer.sql.kt.ast.query.KFilterable
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullTableEx

@DslScope
interface KMutableUpdate<E: Any> : KFilterable<KNonNullTableEx<E>> {

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
        value: KExpression<X>
    )

    fun <X: Any> set(
        path: KNullableExpression<X>,
        value: X?
    )
}