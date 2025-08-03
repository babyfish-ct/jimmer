package org.babyfish.jimmer.sql.kt.ast.table.impl

import org.babyfish.jimmer.View
import org.babyfish.jimmer.kt.toImmutableProp
import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.sql.JoinType
import org.babyfish.jimmer.sql.ast.Selection
import org.babyfish.jimmer.sql.ast.impl.PropExpressionImpl
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor
import org.babyfish.jimmer.sql.fetcher.Fetcher
import org.babyfish.jimmer.sql.kt.ast.expression.KNullablePropExpression
import org.babyfish.jimmer.sql.kt.ast.expression.impl.NullableEmbeddedPropExpressionImpl
import org.babyfish.jimmer.sql.kt.ast.expression.impl.NullablePropExpressionImpl
import org.babyfish.jimmer.sql.kt.ast.table.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

internal class KNullableTableExImpl<E: Any>(
    javaTable: TableImplementor<E>
) : KTableExImpl<E>(javaTable), KNullableTableEx<E> {

    override fun <X: Any> get(prop: String): KNullablePropExpression<X> =
        kotlinExpr(javaTable.get<X>(prop) as PropExpressionImpl<X>)

    override fun <X: Any> get(prop: ImmutableProp): KNullablePropExpression<X> =
        kotlinExpr(javaTable.get<X>(prop) as PropExpressionImpl<X>)

    override fun <X: Any> getId(): KNullablePropExpression<X> =
        kotlinExpr(javaTable.getId<X>() as PropExpressionImpl<X>)

    override fun <X: Any> getAssociatedId(prop: String): KNullablePropExpression<X> =
        kotlinExpr(javaTable.getAssociatedId<X>(prop) as PropExpressionImpl<X>)

    override fun <X: Any> getAssociatedId(prop: ImmutableProp): KNullablePropExpression<X> =
        kotlinExpr(javaTable.getAssociatedId<X>(prop) as PropExpressionImpl<X>)

    override fun <X : Any> join(prop: String): KNonNullTableEx<X> =
        KNonNullTableExImpl(javaTable.join(prop))

    override fun <X : Any> join(prop: ImmutableProp): KNonNullTableEx<X> =
        KNonNullTableExImpl(javaTable.join(prop))

    override fun <X : Any> joinReference(prop: KProperty1<E, X?>): KNonNullTableEx<X> =
        KNonNullTableExImpl(javaTable.join(prop.name))

    override fun <X : Any> joinList(prop: KProperty1<E, List<X>>): KNonNullTableEx<X> =
        KNonNullTableExImpl(javaTable.join(prop.name))

    override fun <X: Any> inverseJoin(backProp: ImmutableProp): KNonNullTableEx<X> =
        KNonNullTableExImpl(javaTable.inverseJoin(backProp, JoinType.INNER))

    override fun <X : Any> inverseJoinReference(backProp: KProperty1<X, E?>): KNonNullTableEx<X> =
        KNonNullTableExImpl(
            javaTable.inverseJoin(backProp.toImmutableProp(), JoinType.INNER)
        )

    override fun <X : Any> inverseJoinList(backProp: KProperty1<X, List<E>>): KNonNullTableEx<X> =
        KNonNullTableExImpl(
            javaTable.inverseJoin(backProp.toImmutableProp(), JoinType.INNER)
        )

    override fun fetch(fetcher: Fetcher<E>?): Selection<E?> =
        javaTable.fetch(fetcher)

    override fun <S : View<E>> fetch(staticType: KClass<S>): Selection<S?> =
        javaTable.fetch(staticType.java)

    override fun asTableEx(): KNullableTableEx<E> =
        KNullableTableExImpl(javaTable.asTableEx() as TableImplementor<E>)

    companion object {

        private fun <X: Any> kotlinExpr(javaExpr: PropExpressionImpl<X>): KNullablePropExpression<X> =
            if (javaExpr is PropExpressionImpl.EmbeddedImpl<*>) {
                NullableEmbeddedPropExpressionImpl(javaExpr as PropExpressionImpl.EmbeddedImpl<X>)
            } else {
                NullablePropExpressionImpl(javaExpr)
            }
    }
}