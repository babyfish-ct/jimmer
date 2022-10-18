package org.babyfish.jimmer.sql.kt.ast.table.impl

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
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

internal class KNonNullTableExImpl<E: Any>(
    javaTable: TableImplementor<E>
) : KTableExImpl<E>(javaTable), KNonNullTableEx<E> {

    @Suppress("UNCHECKED_CAST")
    override fun <X : Any, EXP : KPropExpression<X>> get(prop: String): EXP =
        javaTable.get<PropExpressionImpl<X>>(prop).let {
            if (it.prop.isNullable) {
                NullablePropExpressionImpl(it)
            } else {
                NonNullPropExpressionImpl(it)
            }
        } as EXP

    override fun <X : Any> join(prop: String): KNonNullTableEx<X> =
        KNonNullTableExImpl(javaTable.join(prop))

    override fun <X : Any> joinReference(prop: KProperty1<E, X?>): KNonNullTableEx<X> =
        KNonNullTableExImpl(javaTable.join(prop.name))

    override fun <X : Any> joinList(prop: KProperty1<E, List<X>>): KNonNullTableEx<X>  =
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

    override fun fetch(fetcher: Fetcher<E>): Selection<E> =
        javaTable.fetch(fetcher)

    override fun asTableEx(): KNonNullTableEx<E> =
        KNonNullTableExImpl(javaTable.asTableEx() as TableImplementor<E>)
}