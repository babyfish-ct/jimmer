package org.babyfish.jimmer.sql.kt.ast.table.impl

import org.babyfish.jimmer.kt.toImmutableProp
import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.sql.JoinType
import org.babyfish.jimmer.sql.ast.Selection
import org.babyfish.jimmer.sql.ast.impl.PropExpressionImpl
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor
import org.babyfish.jimmer.sql.fetcher.Fetcher
import org.babyfish.jimmer.sql.kt.ast.expression.KPropExpression
import org.babyfish.jimmer.sql.kt.ast.expression.impl.NullablePropExpressionImpl
import org.babyfish.jimmer.sql.kt.ast.table.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

internal class KNullableTableExImpl<E: Any>(
    javaTable: TableImplementor<E>
) : KTableExImpl<E>(javaTable), KNullableTableEx<E> {

    @Suppress("UNCHECKED_CAST")
    override fun <X : Any, EXP : KPropExpression<X>> get(prop: String): EXP =
        NullablePropExpressionImpl(javaTable.get<PropExpressionImpl<X>>(prop)) as EXP

    override fun <X : Any> join(prop: String): KNonNullTableEx<X> =
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

    override fun <X : Any> weakJoin(weakJoinType: KClass<out KWeakJoin<E, X>>): KNullableTableEx<X> =
        KNullableTableExImpl(
            javaTable.weakJoinImplementor(weakJoinType.java, JoinType.INNER)
        )

    override fun fetch(fetcher: Fetcher<E>?): Selection<E?> =
        javaTable.fetch(fetcher)

    override fun asTableEx(): KNullableTableEx<E> =
        KNullableTableExImpl(javaTable.asTableEx() as TableImplementor<E>)
}