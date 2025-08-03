package org.babyfish.jimmer.sql.kt.ast.table.impl

import org.babyfish.jimmer.View
import org.babyfish.jimmer.kt.toImmutableProp
import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.sql.JoinType
import org.babyfish.jimmer.sql.ast.Selection
import org.babyfish.jimmer.sql.ast.impl.PropExpressionImpl
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor
import org.babyfish.jimmer.sql.fetcher.Fetcher
import org.babyfish.jimmer.sql.kt.ast.expression.KPropExpression
import org.babyfish.jimmer.sql.kt.ast.expression.impl.NonNullEmbeddedPropExpressionImpl
import org.babyfish.jimmer.sql.kt.ast.expression.impl.NonNullPropExpressionImpl
import org.babyfish.jimmer.sql.kt.ast.expression.impl.NullableEmbeddedPropExpressionImpl
import org.babyfish.jimmer.sql.kt.ast.expression.impl.NullablePropExpressionImpl
import org.babyfish.jimmer.sql.kt.ast.table.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

internal open class KNonNullTableExImpl<E: Any>(
    javaTable: TableImplementor<E>,
    private val joinDisabledReason: String? = null
) : KTableExImpl<E>(javaTable), KNonNullTableEx<E> {

    override fun <X: Any> get(prop: String): KPropExpression<X> =
        kotlinExpr((javaTable.get<X>(prop) as PropExpressionImpl<X>))

    override fun <X: Any> get(prop: ImmutableProp): KPropExpression<X> =
        kotlinExpr((javaTable.get<X>(prop) as PropExpressionImpl<X>))

    override fun <X: Any> getId(): KPropExpression<X> =
        kotlinExpr((javaTable.getId<X>() as PropExpressionImpl<X>))

    override fun <X: Any> getAssociatedId(prop: String): KPropExpression<X> =
        kotlinExpr((javaTable.getAssociatedId<X>(prop) as PropExpressionImpl<X>))

    override fun <X: Any> getAssociatedId(prop: ImmutableProp): KPropExpression<X> =
        kotlinExpr((javaTable.getAssociatedId<X>(prop) as PropExpressionImpl<X>))

    override fun <X : Any> join(prop: String): KNonNullTableEx<X> =
        if (joinDisabledReason != null) {
            throw IllegalStateException("Table join is disabled because $joinDisabledReason")
        } else {
            KNonNullTableExImpl(javaTable.join(prop))
        }

    override fun <X : Any> join(prop: ImmutableProp): KNonNullTableEx<X> =
        if (joinDisabledReason != null) {
            throw IllegalStateException("Table join is disabled because $joinDisabledReason")
        } else {
            KNonNullTableExImpl(javaTable.join(prop))
        }

    override fun <X : Any> joinReference(prop: KProperty1<E, X?>): KNonNullTableEx<X> =
        if (joinDisabledReason != null) {
            throw IllegalStateException("Table join is disabled because $joinDisabledReason")
        } else {
            KNonNullTableExImpl(javaTable.join(prop.name))
        }

    override fun <X : Any> joinList(prop: KProperty1<E, List<X>>): KNonNullTableEx<X> =
        if (joinDisabledReason != null) {
            throw IllegalStateException("Table join is disabled because $joinDisabledReason")
        } else {
            KNonNullTableExImpl(javaTable.join(prop.name))
        }

    override fun <X: Any> inverseJoin(backProp: ImmutableProp): KNonNullTableEx<X> =
        if (joinDisabledReason != null) {
            throw IllegalStateException("Table join is disabled because $joinDisabledReason")
        } else {
            KNonNullTableExImpl(javaTable.inverseJoin(backProp, JoinType.INNER))
        }

    override fun <X : Any> inverseJoinReference(backProp: KProperty1<X, E?>): KNonNullTableEx<X> =
        if (joinDisabledReason != null) {
            throw IllegalStateException("Table join is disabled because $joinDisabledReason")
        } else {
            KNonNullTableExImpl(
                javaTable.inverseJoin(backProp.toImmutableProp(), JoinType.INNER)
            )
        }

    override fun <X : Any> inverseJoinList(backProp: KProperty1<X, List<E>>): KNonNullTableEx<X> =
        if (joinDisabledReason != null) {
            throw IllegalStateException("Table join is disabled because $joinDisabledReason")
        } else {
            KNonNullTableExImpl(
                javaTable.inverseJoin(backProp.toImmutableProp(), JoinType.INNER)
            )
        }

    override fun fetch(fetcher: Fetcher<E>?): Selection<E> =
        javaTable.fetch(fetcher)

    override fun <S : View<E>> fetch(staticType: KClass<S>): Selection<S> =
        javaTable.fetch(staticType.java)

    override fun asTableEx(): KNonNullTableEx<E> =
        KNonNullTableExImpl(javaTable.asTableEx() as TableImplementor<E>, joinDisabledReason)

    private fun <X: Any> kotlinExpr(javaExpr: PropExpressionImpl<X>): KPropExpression<X> {
        val isNullable = if (javaExpr.table !== javaTable) {
            // IdView
            (javaExpr.table as TableImplementor).joinProp.isNullable
        } else {
            javaExpr.prop.isNullable
        }
        return if (javaExpr is PropExpressionImpl.EmbeddedImpl<*>) {
            if (isNullable) {
                NullableEmbeddedPropExpressionImpl(javaExpr as PropExpressionImpl.EmbeddedImpl<X>)
            } else {
                NonNullEmbeddedPropExpressionImpl(javaExpr as PropExpressionImpl.EmbeddedImpl<X>)
            }
        } else {
            if (isNullable) {
                NullablePropExpressionImpl(javaExpr)
            } else {
                NonNullPropExpressionImpl(javaExpr)
            }
        }
    }
}