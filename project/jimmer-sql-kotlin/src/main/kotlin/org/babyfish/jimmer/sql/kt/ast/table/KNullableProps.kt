package org.babyfish.jimmer.sql.kt.ast.table

import org.babyfish.jimmer.meta.ImmutableProp
import kotlin.reflect.KProperty1

interface KNullableProps<E: Any> : KProps<E> {

    override fun <X: Any> join(prop: String): KNullableTable<X>
    override fun <X: Any> joinReference(prop: KProperty1<E, X?>): KNullableTable<X>
    override fun <X: Any> joinList(prop: KProperty1<E, List<X>>): KNullableTable<X>

    override fun <X: Any> inverseJoin(backProp: ImmutableProp): KNullableTable<X>
    override fun <X: Any> inverseJoinReference(backProp: KProperty1<X, E?>): KNullableTable<X>
    override fun <X: Any> inverseJoinList(backProp: KProperty1<X, List<E>>): KNullableTable<X>
}