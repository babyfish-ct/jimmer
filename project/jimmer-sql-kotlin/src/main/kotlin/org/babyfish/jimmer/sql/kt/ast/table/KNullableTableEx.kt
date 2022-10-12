package org.babyfish.jimmer.sql.kt.ast.table

import org.babyfish.jimmer.meta.ImmutableProp
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

interface KNullableTableEx<E: Any> : KNullableTable<E>, KTableEx<E> {

    override fun <X: Any> join(prop: String): KNullableTableEx<X>
    override fun <X: Any> joinReference(prop: KProperty1<E, X?>): KNullableTableEx<X>
    override fun <X: Any> joinList(prop: KProperty1<E, List<X>>): KNullableTableEx<X>

    override fun <X: Any> inverseJoin(backProp: ImmutableProp): KNullableTableEx<X>
    override fun <X: Any> inverseJoinReference(backProp: KProperty1<X, E?>): KNullableTableEx<X>
    override fun <X: Any> inverseJoinList(backProp: KProperty1<X, List<E>>): KNullableTableEx<X>
}