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
import org.babyfish.jimmer.sql.kt.ast.expression.impl.NonNullPropExpressionImpl
import org.babyfish.jimmer.sql.kt.ast.expression.impl.NullablePropExpressionImpl
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullTableEx
import org.babyfish.jimmer.sql.kt.ast.table.KWeakJoin
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

internal class KNonNullTableExImpl<E: Any>(
    javaTable: TableImplementor<E>,
    private val joinDisabledReason: String? = null
) : KTableExImpl<E>(javaTable), KNonNullTableEx<E> {

    override fun <X: Any> get(prop: String): KPropExpression<X> =
        (javaTable.get<X>(prop) as PropExpressionImpl<X>).let {
            val isNullable = if (it.table !== javaTable) {
                // IdView
                (it.table as TableImplementor).joinProp.isNullable
            } else {
                it.prop.isNullable
            }
            if (isNullable) {
                NullablePropExpressionImpl(it)
            } else {
                NonNullPropExpressionImpl(it)
            }
        }

    override fun <X: Any> get(prop: ImmutableProp): KPropExpression<X> =
        (javaTable.get<X>(prop) as PropExpressionImpl<X>).let {
            val isNullable = if (it.table !== javaTable) {
                // IdView
                (it.table as TableImplementor).joinProp.isNullable
            } else {
                it.prop.isNullable
            }
            if (isNullable) {
                NullablePropExpressionImpl(it)
            } else {
                NonNullPropExpressionImpl(it)
            }
        }

    override fun <X: Any> getId(): KPropExpression<X> =
        (javaTable.getId<X>() as PropExpressionImpl<X>).let {
            val isNullable = if (it.table !== javaTable) {
                // IdView
                (it.table as TableImplementor).joinProp.isNullable
            } else {
                it.prop.isNullable
            }
            if (isNullable) {
                NullablePropExpressionImpl(it)
            } else {
                NonNullPropExpressionImpl(it)
            }
        }

    override fun <X: Any> getAssociatedId(prop: String): KPropExpression<X> =
        (javaTable.getAssociatedId<X>(prop) as PropExpressionImpl<X>).let {
            val isNullable = if (it.table !== javaTable) {
                // IdView
                (it.table as TableImplementor).joinProp.isNullable
            } else {
                it.prop.isNullable
            }
            if (isNullable) {
                NullablePropExpressionImpl(it)
            } else {
                NonNullPropExpressionImpl(it)
            }
        }

    override fun <X: Any> getAssociatedId(prop: ImmutableProp): KPropExpression<X> =
        (javaTable.getAssociatedId<X>(prop) as PropExpressionImpl<X>).let {
            val isNullable = if (it.table !== javaTable) {
                // IdView
                (it.table as TableImplementor).joinProp.isNullable
            } else {
                it.prop.isNullable
            }
            if (isNullable) {
                NullablePropExpressionImpl(it)
            } else {
                NonNullPropExpressionImpl(it)
            }
        }

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

    override fun <X : Any> weakJoin(weakJoinType: KClass<out KWeakJoin<E, X>>): KNonNullTableEx<X> =
        if (joinDisabledReason != null) {
            throw IllegalStateException("Table join is disabled because $joinDisabledReason")
        } else {
            KNonNullTableExImpl(
                javaTable.weakJoinImplementor(weakJoinType.java, JoinType.INNER)
            )
        }

    override fun fetch(fetcher: Fetcher<E>?): Selection<E> =
        javaTable.fetch(fetcher)

    override fun <S : View<E>> fetch(staticType: KClass<S>): Selection<S> =
        javaTable.fetch(staticType.java)

    override fun asTableEx(): KNonNullTableEx<E> =
        KNonNullTableExImpl(javaTable.asTableEx() as TableImplementor<E>)
}