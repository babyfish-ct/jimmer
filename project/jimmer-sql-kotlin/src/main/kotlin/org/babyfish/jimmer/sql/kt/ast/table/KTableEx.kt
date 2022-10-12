package org.babyfish.jimmer.sql.kt.ast.table

import org.babyfish.jimmer.meta.ImmutableProp
import kotlin.reflect.KProperty1

interface KTableEx<E: Any> : KTable<E> {

    override fun <X: Any> join(prop: String): KTableEx<X>
    override fun <X: Any> joinReference(prop: KProperty1<E, X?>): KTableEx<X>
    override fun <X: Any> joinList(prop: KProperty1<E, List<X>>): KTableEx<X>

    override fun <X: Any> outerJoin(prop: String): KNullableTableEx<X>
    override fun <X: Any> outerJoinReference(prop: KProperty1<E, X?>): KNullableTableEx<X>
    override fun <X: Any> outerJoinList(prop: KProperty1<E, List<X>>): KNullableTableEx<X>

    override fun <X: Any> inverseJoin(backProp: ImmutableProp): KTableEx<X>
    override fun <X: Any> inverseJoinReference(backProp: KProperty1<X, E?>): KTableEx<X>
    override fun <X: Any> inverseJoinList(backProp: KProperty1<X, List<E>>): KTableEx<X>

    override fun <X: Any> inverseOuterJoin(backProp: ImmutableProp): KNullableTableEx<X>
    override fun <X: Any> inverseOuterJoinReference(backProp: KProperty1<X, E?>): KNullableTableEx<X>
    override fun <X: Any> inverseOuterJoinList(backProp: KProperty1<X, List<E>>): KNullableTableEx<X>
}