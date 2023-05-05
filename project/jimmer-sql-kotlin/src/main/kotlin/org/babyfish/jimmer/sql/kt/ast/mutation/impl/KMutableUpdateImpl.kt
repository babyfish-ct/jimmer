package org.babyfish.jimmer.sql.kt.ast.mutation.impl

import org.babyfish.jimmer.sql.ast.Expression
import org.babyfish.jimmer.sql.ast.impl.mutation.MutableUpdateImpl
import org.babyfish.jimmer.sql.kt.KSubQueries
import org.babyfish.jimmer.sql.kt.KWildSubQueries
import org.babyfish.jimmer.sql.kt.ast.expression.*
import org.babyfish.jimmer.sql.kt.ast.expression.impl.NonNullPropExpressionImpl
import org.babyfish.jimmer.sql.kt.ast.expression.impl.NullablePropExpressionImpl
import org.babyfish.jimmer.sql.kt.ast.expression.impl.toJavaPredicate
import org.babyfish.jimmer.sql.kt.ast.mutation.KMutableUpdate
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullTableEx
import org.babyfish.jimmer.sql.kt.ast.table.impl.KNonNullTableExImpl
import org.babyfish.jimmer.sql.kt.impl.KSubQueriesImpl
import org.babyfish.jimmer.sql.kt.impl.KWildSubQueriesImpl

internal class KMutableUpdateImpl<E: Any>(
    private val javaUpdate: MutableUpdateImpl
): KMutableUpdate<E> {

    override val table: KNonNullTableEx<E> =
        KNonNullTableExImpl(javaUpdate.getTable())

    override fun where(vararg predicates: KNonNullExpression<Boolean>?) {
        javaUpdate.where(*predicates.map { it?.toJavaPredicate() }.toTypedArray())
    }

    @Suppress("UNCHECKED_CAST")
    override fun <X : Any> set(path: KNonNullPropExpression<X>, value: KNonNullExpression<X>) {
        javaUpdate.set(
            (path as NonNullPropExpressionImpl<X>).javaPropExpression,
            value as Expression<X>
        )
    }

    override fun <X : Any> set(path: KNonNullPropExpression<X>, value: X) {
        javaUpdate.set(
            (path as NonNullPropExpressionImpl<X>).javaPropExpression,
            Expression.any().value(value)
        )
    }

    @Suppress("UNCHECKED_CAST")
    override fun <X : Any> set(path: KNullablePropExpression<X>, value: KExpression<X>) {
        javaUpdate.set(
            (path as NullablePropExpressionImpl<X>).javaPropExpression,
            value as Expression<X>
        )
    }

    override fun <X : Any> set(path: KNullableExpression<X>, value: X?) {
        val javaPropExpression = (path as NullablePropExpressionImpl<X>).javaPropExpression
        javaUpdate.set(
            javaPropExpression,
            if (value !== null) {
                Expression.any().value(value)
            } else {
                Expression.nullValue(javaPropExpression.type)
            }
        )
    }

    override val subQueries: KSubQueries<E> =
        KSubQueriesImpl(javaUpdate)

    override val wildSubQueries: KWildSubQueries<E> =
        KWildSubQueriesImpl(javaUpdate)
}