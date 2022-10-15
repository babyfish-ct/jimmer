package org.babyfish.jimmer.sql.kt.ast.table

import org.babyfish.jimmer.meta.ImmutableProp
import kotlin.reflect.KProperty1

interface KNonNullProps<E: Any> : KProps<E> {

    override fun <X: Any> join(prop: String): KNonNullTable<X>
    override fun <X: Any> joinReference(prop: KProperty1<E, X?>): KNonNullTable<X>
    override fun <X: Any> joinList(prop: KProperty1<E, List<X>>): KNonNullTable<X>

    override fun <X: Any> inverseJoin(backProp: ImmutableProp): KNonNullTable<X>
    override fun <X: Any> inverseJoinReference(backProp: KProperty1<X, E?>): KNonNullTable<X>
    override fun <X: Any> inverseJoinList(backProp: KProperty1<X, List<E>>): KNonNullTable<X>
}